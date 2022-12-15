package me.sk.ta.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    // execute after the application starts.
    public MVStoreRepo(String dbFolder, String dbName, Class valueType, Class keyClass) {
        DB_FILE_NAME = dbName;
        baseDir = new File(dbFolder, DB_FILE_NAME);
        this.valueClass = valueType;
        this.keyClass = keyClass;

        try {
            Files.createDirectories(baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getAbsoluteFile().toPath());
            db = new MVStore.Builder()
                    .fileName(baseDir.getAbsolutePath())
                    .encryptionKey("007".toCharArray())
                    .compress()
                    .open();
            log.info("{} db initialized at {}", DB_FILE_NAME, baseDir.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error initializing {} db. Exception: '{}', message: '{}'", DB_FILE_NAME, e.getCause(), e.getMessage(), e);
        }
    }

    private ObjectMapper getSerializer() {
        var jm = new ObjectMapper();

        if (keyClass.getModule().getName().equals("java.time") ||
                valueClass.getModule().getName().equals("java.time")) {
            jm.registerModule(new JavaTimeModule());
        }
        return jm;
    }

    @Override
    public synchronized boolean save(K key, V value) {
        log.info("saving value '{}' with key '{}'", value, key);

        try {
            MVMap<String, String> map = db.openMap(DB_FILE_NAME);
            map.put(getSerializer().writeValueAsString(key), getSerializer().writeValueAsString(value));
        } catch (JsonProcessingException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public synchronized Optional<V> find(K key) {

        try {
            MVMap<String, String> map = db.openMap(DB_FILE_NAME);
            var value = map.get(getSerializer().writeValueAsString(key));
            log.info("finding key '{}' returns '{}'", key, value);
            if (value == null) {
                return Optional.empty();
            } else {
                return Optional.of((V) getSerializer().readValue(value, valueClass));
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
        var jm = getSerializer();
        try {
            MVMap<String, String> map = db.openMap(DB_FILE_NAME);
            if (map.firstKey() == null) {
                return result;
            }
            Cursor<String, String> it = map.cursor(map.firstKey());
            while (it.hasNext()) {
                it.next();
                var v = (V) jm.readValue(it.getValue(), valueClass);
                var ov = filter.apply(v);
                if (ov.isPresent()) {
                    result.add(ov.get());
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
            throw new IllegalArgumentException("key");
        }
        MVMap<String, String> map = db.openMap(DB_FILE_NAME);
        try {
            map.remove(getSerializer().writeValueAsString(key));
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
}
