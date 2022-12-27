package me.sk.ta.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.h2.mvstore.tx.Transaction;
import org.h2.mvstore.tx.TransactionMap;
import org.h2.mvstore.tx.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MVStoreRepo<K, V> implements KvDb<K, V> {
    private static final Logger log = LoggerFactory.getLogger(MVStoreRepo.class);
    private String DB_FILE_NAME;
    File baseDir;
    MVStore db;
    TransactionStore ts;
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
            ts = new TransactionStore(db);
            ts.init();
        } catch (IOException e) {
            log.error("Error initializing {} db. Exception: '{}', message: '{}'", DB_FILE_NAME, e.getCause(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public synchronized Transaction beginTransaction() {
        if (ts == null) {
            ts = new TransactionStore(db);
            ts.init();
        }
        return ts.begin();
    }

    @Override
    public synchronized boolean save(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        log.trace("saving value '{}' with key '{}'", value, key);

        var tx = beginTransaction();
        try {
            TransactionMap<String, String> map = tx.openMap(DB_FILE_NAME);
            map.put(serializer.writeValueAsString(key), value == null ? null : serializer.writeValueAsString(value));
            tx.commit();
        } catch (MVStoreException te) {
            tx.rollback();
            log.error("Transaction failed: {}", te);
            return false;
        } catch (JsonProcessingException e) {
            tx.rollback();
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public synchronized boolean save(K key, V value, Transaction tx) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        log.trace("saving value '{}' with key '{}'", value, key);

        try {
            TransactionMap<String, String> map = tx.openMap(DB_FILE_NAME);
            map.put(serializer.writeValueAsString(key), value == null ? null : serializer.writeValueAsString(value));
            tx.prepare();
        } catch (MVStoreException te) {
            log.error("Transaction failed: {}", te);
            return false;
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
        log.trace("Open transactions: {}", ts.getOpenTransactions().size());
        var tx = beginTransaction();
        try {
            TransactionMap<String, String> map = tx.openMap(DB_FILE_NAME);
            var kd = serializer.writeValueAsString(key);
            if (map.containsKey(key)) {
                var value = map.get(kd);
                log.trace("get key '{}' returns '{}'", key, value);
                if (value == null) {
                    return null;
                } else {
                    return (V) serializer.readValue(value, valueClass);
                }
            } else {
                throw new RuntimeException("Not found");
            }
        } catch (JsonProcessingException e) {
            log.error(
                    "Error retrieving the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage());
            throw new RuntimeException(e);
        } finally {
            tx.commit();
        }
    }

    @Override
    public synchronized Optional<V> find(K key) {

        log.trace("Open transactions: {}", ts.getOpenTransactions().size());
        var tx = beginTransaction();
        try {
            TransactionMap<String, String> map = tx.openMap(DB_FILE_NAME);
            var value = map.get(serializer.writeValueAsString(key));
            log.trace("finding key '{}' returns '{}'", key, value);
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
        } finally {
            tx.commit();
        }
    }

    @Override
    public synchronized Optional<V> find(K key, Transaction tx) {
        try {
            TransactionMap<String, String> map = tx.openMap(DB_FILE_NAME);
            var value = map.get(serializer.writeValueAsString(key));
            log.trace("found key '{}' returns '{}'", key, value);
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
        var tx = beginTransaction();
        try {
            TransactionMap<String, String> map = tx.openMap(DB_FILE_NAME);
            if (map.firstKey() == null) {
                return result;
            }
            Iterator<String> it = map.keyIterator(map.firstKey());
            while (it.hasNext()) {
                var key = it.next();
                V val = null;
                if (map.get(key) != null) {
                    val = (V) jm.readValue(map.get(key), valueClass);
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
        } finally {
            tx.commit();
        }
    }

    @Override
    public synchronized boolean delete(K key) {
        var tx = beginTransaction();
        try {
            var result = delete(key, tx);
            if (result) {
                tx.commit();
            } else {
                tx.rollback();
            }
            return result;
        } catch (Exception ex) {
            tx.rollback();
            throw ex;
        }
    }

    @Override
    public synchronized boolean delete(K key, Transaction tx) {
        log.info("deleting key '{}'", key);
        if (key == null) {
            return false;
        }
        TransactionMap<String, String> map = tx.openMap(DB_FILE_NAME);
        try {
            map.remove(serializer.writeValueAsString(key));
            tx.prepare();
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
