package me.sk.ta.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface KvDb<K, V> {
    boolean save(K key, V value);

    Optional<V> find(K key);
    List<V> findAll(Function<V, Optional<V>> filter);

    boolean delete(K key);

    void close();
}
