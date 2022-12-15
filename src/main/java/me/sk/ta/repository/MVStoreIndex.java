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
    private static final Logger log = LoggerFactory.getLogger("H2IndexDb");
    final MVStore indexDb;
    final String indexName;
    final IndexingStrategy strategy;
    File baseDir;
    final Class valueClass;
    final Class keyClass;
    final String keySeparator;

    public MVStoreIndex(String dbFolder, String indexName, IndexingStrategy strategy, Class valueType, Class keyClass, String keySeparator) {
        this.strategy = strategy;
        this.valueClass = valueType;
        this.keyClass = keyClass;
        this.indexName = indexName;
        this.keySeparator = keySeparator;
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

    private synchronized ObjectMapper getSerializer() {
        if (jacksonMapper == null) {
            jacksonMapper = new ObjectMapper();
            jacksonMapper.registerModule(new JavaTimeModule());
        }
        return jacksonMapper;
    }

    public synchronized List<V> find(K desiredKey) {
        if (desiredKey == null) {
            throw new IllegalArgumentException("indexKey");
        }
        List<V> result = new ArrayList<>();
        MVMap<String, String> map = indexDb.openMap(indexName);
        var jm = getSerializer();
        try {
            var ikd = jm.writeValueAsString(desiredKey);
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

    private V getValue(String key, String value) {
        try {
            var om = getSerializer();
            switch (strategy) {
                case PostfixWithCount -> {
                    return (V) om.readValue(value, valueClass);
                }
                case PostfixValue -> {
                    var posOfValue = key.indexOf(keySeparator) + keySeparator.length();
                    if (posOfValue < key.length())
                        return (V) om.readValue(value, valueClass);
                    else
                        return (V) om.readValue(key.substring(posOfValue, key.length()), valueClass);
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
            if (map.get(getSerializer().writeValueAsString(key)) == null) {
                map.put(getSerializer().writeValueAsString(key), getSerializer().writeValueAsString(value));
            } else {
                map.put(generateKey(key, value), getSerializer().writeValueAsString(value));
            }
        } catch (JsonProcessingException e) {
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
        MVMap<String, String> map = indexDb.openMap(indexName);
        map.remove(generateKey(indexKey, indexValue));
    }

    public void close() {
        indexDb.close();
    }

    public void clear() {
        MVMap<String, String> map = indexDb.openMap(indexName);
        map.clear();
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
            var om = getSerializer();
            var ks = om.writeValueAsString(key);
            var vs = om.writeValueAsString(GetCount());
            return (ks + keySeparator + vs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String postFixValue(K key, V value) {
        try {
            var om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            var ks = om.writeValueAsString(key);
            var vs = om.writeValueAsString(value);
            return (ks + keySeparator + vs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Long GetCount() {
        MVMap<Integer, Long> map = indexDb.openMap("indexName" + "-counter");
        if (map.get(1) == null) {
            map.put(1, 0L);
        }
        var val = map.get(1);
        map.put(1, val + 1L);
        return val;
    }
}
