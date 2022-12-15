package me.sk.ta.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.sk.ta.domain.Utils;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RocksDbIndex<K, V> {
    private static final Logger log = LoggerFactory.getLogger("RocksDbIndex");
    RocksDB indexDb;
    String indexName;
    IndexingStrategy strategy;
    File baseDir;
    Class valueClass;
    Class keyClass;

    public RocksDbIndex(String dbFolder, String indexName, IndexingStrategy strategy, Class valueType, Class keyClass) {
        this.strategy = strategy;

        this.valueClass = valueType;
        this.keyClass = keyClass;
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        baseDir = new File(dbFolder, indexName);

        try {
            Files.createDirectories(baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getAbsoluteFile().toPath());
            indexDb = RocksDB.open(options, baseDir.getAbsolutePath());

            log.info("RocksDB initialized at {}", baseDir.getAbsoluteFile().toPath());
        } catch (IOException | RocksDBException e) {
            log.error("Error initializng RocksDB. Exception: '{}', message: '{}'", e.getCause(), e.getMessage(), e);
        }
    }

    public enum IndexingStrategy {
        PostFixValue,
        MultipleValues,
        PostFixCounter
    }

    public synchronized List<V> find(K indexKey) {
        if (indexKey == null) {
            throw new IllegalArgumentException("indexKey");
        }
        try {
            List<V> result = new ArrayList<>();
            RocksIterator iterator = indexDb.newIterator();
            byte[] prefix = new byte[0];
            var om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            prefix = om.writeValueAsBytes(indexKey);
            for (iterator.seek(prefix); iterator.isValid(); iterator
                    .next()) {
                if (!Utils.byteArrayStartsWith(iterator.key(), prefix))
                    break;
                result.add(getValue(iterator.key(), iterator.value(), prefix));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private V getValue(byte[] key, byte[] value, byte[] prefix) {
        try {
            var om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            switch (strategy) {
                case PostFixCounter -> {
                    return (V) om.readValue(value, valueClass);
                }
                case PostFixValue -> {
                    var posOfValue = prefix.length + 1;
                    return (V) om.readValue(Arrays.copyOfRange(key, posOfValue, key.length - 1), valueClass);
                }
                case MultipleValues -> {
                    throw new RuntimeException("'MultipleValues' strategy is not yet implemented");
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean index(K key, V value) {
        log.debug("saving value '{}' with key '{}'", value, key);

        try {
            indexDb.put(generateKey(key, value), SerializationUtils.serialize(value));
        } catch (RocksDBException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }

        return true;
    }

    public void delete(K indexKey, V indexValue) {
        if (indexKey == null) {
            throw new IllegalArgumentException("indexKey");
        }
        if (indexValue == null) {
            throw new IllegalArgumentException("indexValue");
        }
        try {
            indexDb.delete(generateKey(indexKey, indexValue));
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            indexDb.cancelAllBackgroundWork(true);
            indexDb.flush(new FlushOptions().setWaitForFlush(true));
            indexDb.flushWal(true);
            indexDb.closeE();
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] generateKey(K key, V value) {
        switch (this.strategy) {
            case PostFixValue -> {
                return postFixValue(key, value);
            }
            case PostFixCounter -> {
                return postFixTime(key);
            }
            case MultipleValues -> {
                throw new RuntimeException("MultipleValues strategy is not implemented");
            }
        }
        return new byte[0];
    }

    private byte[] postFixTime(K key) {
        try {
            var om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            var ks = om.writeValueAsString(key);
            var vs = om.writeValueAsString(java.time.Instant.now().toEpochMilli());
            return (ks + "~" + vs).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] postFixValue(K key, V value) {
        try {
            var om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            var ks = om.writeValueAsString(key);
            var vs = om.writeValueAsString(value);
            return (ks + "~" + vs).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
