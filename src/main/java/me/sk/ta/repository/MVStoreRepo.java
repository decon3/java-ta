package me.sk.ta.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MVStoreRepo<K, V> implements KvDb<K, V> {
    private static final Logger log = LoggerFactory.getLogger(MVStoreRepo.class);
    private String DB_FILE_NAME;
    File baseDir;
    MVStore db;
    final Class valueClass;

    final Class keyClass;
    final ObjectMapper serializer;

    // execute after the application starts.
    public MVStoreRepo(String dbFolder, String dbName, Class keyClass, Class valueType, ObjectMapper serializer) {
        DB_FILE_NAME = dbName;
        baseDir = new File(dbFolder, DB_FILE_NAME);
        this.valueClass = valueType;
        this.keyClass = keyClass;
        this.serializer = serializer;

        try {
            log.debug("Creating path: {}", baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getParentFile().toPath());
            db = new MVStore.Builder()
                    .fileName(baseDir.getAbsolutePath())
                    .encryptionKey("007".toCharArray())
                    .compress()
                    .open();
            log.info("{} db initialized at {}", DB_FILE_NAME, baseDir.getAbsolutePath());
            log.info("key class: {}, value class: {}", keyClass, valueClass);
        } catch (IOException e) {
            log.error("Error initializing {} db. Exception: '{}', message: '{}'", DB_FILE_NAME, e.getCause(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean save(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        log.trace("saving value '{}' with key '{}'", value, key);

        try {
            MVMap<String, String> map = db.openMap(DB_FILE_NAME);
            map.put(serializer.writeValueAsString(key), value == null ? null : serializer.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public synchronized V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        try {
            MVMap<String, String> map = db.openMap(DB_FILE_NAME);
            var kd = serializer.writeValueAsString(key);
            if (map.containsKey(key)) {
                var value = map.get(kd);
                log.info("finding key '{}' returns '{}'", key, value);
                if (value == null) {
                    return null;
                } else {
                    return (V) serializer.readValue(value, valueClass);
                }
            }
            else {
                throw new RuntimeException("Not found");
            }
        } catch (JsonProcessingException e) {
            log.error(
                    "Error retrieving the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized Optional<V> find(K key) {

        try {
            MVMap<String, String> map = db.openMap(DB_FILE_NAME);
            var value = map.get(serializer.writeValueAsString(key));
            log.info("finding key '{}' returns '{}'", key, value);
            if (value == null) {
                return Optional.empty();
            } else {
                return Optional.of((V) serializer.readValue(value, valueClass));
            }
        } catch (JsonProcessingException e) {
            log.error(
                    "Error retrieving the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized List<V> findAll(Function<V, Optional<V>> filter) {
        List<V> result = new ArrayList<>();
        var jm = serializer;
        try {
            MVMap<String, String> map = db.openMap(DB_FILE_NAME);
            if (map.firstKey() == null) {
                return result;
            }
            Cursor<String, String> it = map.cursor(map.firstKey());
            while (it.hasNext()) {
                it.next();
                V val = null;
                if (it.getValue() != null) {
                    val = (V) jm.readValue(it.getValue(), valueClass);
                }
                if (filter == null) {
                    result.add(val);
                } else {
                    var filterResult = filter.apply(val);
                    if (filterResult.isPresent()) {
                        result.add(filterResult.get());
                    }
                }
            }
            return result;
        } catch (JsonProcessingException e) {
            log.error(
                    "Error retrieving retrieving entries - cause: {}, message: {}",
                    e.getCause(),
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean delete(K key) {
        log.info("deleting key '{}'", key);
        if (key == null) {
            return false;
        }
        MVMap<String, String> map = db.openMap(DB_FILE_NAME);
        try {
            map.remove(serializer.writeValueAsString(key));
            return true;
        } catch (JsonProcessingException e) {
            log.error(
                    "Error deleting the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public synchronized void drop() {
        try {
            log.info("Dropping {} table", baseDir.getAbsolutePath());
            db.close();
            Files.deleteIfExists(baseDir.getAbsoluteFile().toPath());
        } catch (IOException e) {
            log.error("Error dropping {} table. Cause: '{}', message: '{}'", baseDir.getAbsolutePath(), e.getCause(), e.getMessage());
        }
    }
}
