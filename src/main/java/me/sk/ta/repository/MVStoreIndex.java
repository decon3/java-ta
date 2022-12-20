package me.sk.ta.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MVStoreIndex<K, V> {
    private static final Logger log = LoggerFactory.getLogger(MVStoreIndex.class);
    final MVStore indexDb;
    final String indexName;
    final IndexingStrategy strategy;
    File baseDir;
    final Class valueClass;
    final Class keyClass;
    final String keySeparator;
    final ObjectMapper serializer;

    public MVStoreIndex(String dbFolder, String indexName, IndexingStrategy strategy, Class valueType, Class keyClass, String keySeparator, ObjectMapper serializer) {
        this.strategy = strategy;
        this.valueClass = valueType;
        this.keyClass = keyClass;
        this.indexName = indexName;
        this.keySeparator = keySeparator;
        this.serializer = serializer;
        if (strategy.equals(IndexingStrategy.PostfixValue) || strategy.equals(IndexingStrategy.PostfixWithCount)) {
            if (keySeparator == null || keySeparator.isEmpty()) {
                throw new RuntimeException("key separator is required for PostfixValue/PostfixWithCount strategies");
            }
        }
        baseDir = new File(dbFolder, indexName);

        try {
            log.debug("Creating path: {}", baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getParentFile().toPath());
            indexDb = new MVStore.Builder()
                    .fileName(baseDir.getAbsolutePath())
                    .encryptionKey("007".toCharArray())
                    .compress()
                    .open();
            log.info("{} IndexDB initialized at {}", indexName, baseDir.getAbsoluteFile().toPath());
        } catch (IOException e) {
            log.error("Error initializing IndexDB. Exception: '{}', message: '{}'", e.getCause(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public enum IndexingStrategy {
        PostfixValue,
        MultipleValues,
        PostfixWithCount
    }

    ObjectMapper jacksonMapper;

    public synchronized List<V> find(K desiredKey) {
        if (desiredKey == null) {
            throw new IllegalArgumentException("indexKey");
        }
        List<V> result = new ArrayList<>();
        MVMap<String, String> map = indexDb.openMap(indexName);
        try {
            var ikd = serializer.writeValueAsString(desiredKey);
            Iterator<String> it = map.keyIterator(ikd);
            while (it.hasNext()) {
                var key = it.next();
                if (key.startsWith(ikd) == false) {
                    break;
                }
                result.add(getValue(key, map.get(ikd)));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized V getValue(String key, String value) {
        try {
            switch (strategy) {
                case PostfixWithCount -> {
                    return (V) serializer.readValue(value, valueClass);
                }
                case PostfixValue -> {
                    var posOfValue = key.indexOf(keySeparator) + keySeparator.length();
                    if (posOfValue < key.length())
                        return (V) serializer.readValue(value, valueClass);
                    else
                        return (V) serializer.readValue(key.substring(posOfValue, key.length()), valueClass);
                }
                case MultipleValues -> {
                    throw new RuntimeException("MultipleValues strategy is not implemented");
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean index(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
        log.debug("saving value '{}' with key '{}'", value, key);

        try {
            MVMap<String, String> map = indexDb.openMap(indexName);
            if (map.get(serializer.writeValueAsString(key)) == null) {
                map.put(serializer.writeValueAsString(key), serializer.writeValueAsString(value));
            } else {
                map.put(generateKey(key, value), serializer.writeValueAsString(value));
            }
        } catch (JsonProcessingException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }

    public synchronized void delete(K indexKey, V indexValue) {
        if (indexKey == null) {
            throw new IllegalArgumentException("indexKey");
        }
        if (indexValue == null) {
            throw new IllegalArgumentException("indexValue");
        }
        MVMap<String, String> map = indexDb.openMap(indexName);
        map.remove(generateKey(indexKey, indexValue));
    }

    public synchronized void close() {
        indexDb.close();
        log.debug("{} index closed", indexName);
    }

    public synchronized void clear() {
        MVMap<String, String> map = indexDb.openMap(indexName);
        map.clear();
        log.debug("{} index cleared of all entries", indexName);
    }

    public synchronized boolean drop() {
        try {
            log.info("Dropping {} index", baseDir.getAbsolutePath());
            indexDb.close();
            Files.deleteIfExists(baseDir.getAbsoluteFile().toPath());
            return true;
        } catch (IOException e) {
            log.error("Error dropping {} index. Cause: '{}', message: '{}'", baseDir.getAbsolutePath(), e.getCause(), e.getMessage());
            return false;
        }
    }

    private String generateKey(K key, V value) {
        switch (this.strategy) {
            case PostfixValue -> {
                return postFixValue(key, value);
            }
            case PostfixWithCount -> {
                return postFixCount(key);
            }
            case MultipleValues -> {
                throw new RuntimeException("MultipleValues strategy is not implemented");
            }
        }
        return "";
    }

    private String postFixCount(K key) {
        try {
            var ks = serializer.writeValueAsString(key);
            var vs = serializer.writeValueAsString(GetPostfixCount());
            return (ks + keySeparator + vs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String postFixValue(K key, V value) {
        try {
            var ks = serializer.writeValueAsString(key);
            var vs = serializer.writeValueAsString(value);
            return (ks + keySeparator + vs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Long GetPostfixCount() {
        MVMap<Integer, Long> map = indexDb.openMap("indexName" + "-counter");
        if (map.get(1) == null) {
            map.put(1, 0L);
        }
        var val = map.get(1);
        map.put(1, val + 1L);
        return val;
    }
}
