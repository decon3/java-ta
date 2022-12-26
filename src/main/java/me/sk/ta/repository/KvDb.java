package me.sk.ta.repository;

import org.h2.mvstore.tx.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface KvDb<K, V> {

    Optional<V> find(K key);

    List<V> findAll(Function<V, Optional<V>> filter);

    boolean delete(K key);
    boolean delete(K key, Transaction x);

    void close();

    Transaction beginTransaction();
    boolean save(K key, V value, Transaction tx);
    boolean save(K key, V value);

    V get(K key);

    void drop();
}
