package me.sk.ta.repository;

import me.sk.ta.domain.Utils;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RocksDbRepo<K, V> implements KvDb<K, V> {
    private static final Logger log = LoggerFactory.getLogger(RocksDbRepo.class);
    private String DB_FILE_NAME;
    File baseDir;
    RocksDB db;

    // execute after the application starts.
    public RocksDbRepo(String dbFolder, String dbName) {
        DB_FILE_NAME = dbName;
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        baseDir = new File(dbFolder, DB_FILE_NAME);

        try {
            Files.createDirectories(baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getAbsoluteFile().toPath());
            db = RocksDB.open(options, baseDir.getAbsolutePath());

            log.info("RocksDB initialized");
        } catch (IOException | RocksDBException e) {
            log.error("Error initializng RocksDB. Exception: '{}', message: '{}'", e.getCause(), e.getMessage(), e);
        }
    }

    @Override
    public synchronized boolean save(K key, V value) {
        log.info("saving value '{}' with key '{}'", value, key);

        try {
            db.put(SerializationUtils.serialize(key), SerializationUtils.serialize(value));
        } catch (RocksDBException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());

            return false;
        }

        return true;
    }

    @Override
    public synchronized Optional<V> find(K key) {
        V value = null;

        try {
            byte[] bytes = db.get(SerializationUtils.serialize(key));
            if (bytes != null) value = (V) SerializationUtils.deserialize(bytes);
        } catch (RocksDBException e) {
            log.error(
                    "Error retrieving the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage()
            );
        }

        log.info("finding key '{}' returns '{}'", key, value);

        return value != null ? Optional.of(value) : Optional.empty();
    }

    @Override
    public synchronized List<V> findAll(Function<V, Optional<V>> filter) {
        List<V> result = new ArrayList<>();
        RocksIterator iterator = db.newIterator();
        for (; iterator.isValid(); iterator.next()) {
            var v = (V)SerializationUtils.deserialize(iterator.value());
            var ov = filter.apply(v);
            if( ov.isPresent()) {
                result.add(ov.get());
            }
        }
        return result;
    }

    @Override
    public synchronized boolean delete(K key) {
        log.info("deleting key '{}'", key);

        try {
            db.delete(SerializationUtils.serialize(key));
        } catch (RocksDBException e) {
            log.error("Error deleting entry, cause: '{}', message: '{}'", e.getCause(), e.getMessage());

            return false;
        }

        return true;
    }

    @Override
    public void close() {
        try {
            db.cancelAllBackgroundWork(true);
            db.flush(new FlushOptions().setWaitForFlush(true));
            db.flushWal(true);
            db.closeE();
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void drop() {
        this.close();
        try {
            Files.deleteIfExists(baseDir.getAbsoluteFile().toPath());
        } catch (IOException e) {
            log.error("Error dropping the table, cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public V get(K key) {
        return null;
    }
}
