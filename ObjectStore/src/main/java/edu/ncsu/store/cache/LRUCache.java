package edu.ncsu.store.cache;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A simple cache in which you can put a key-value pairs and get back those pairs.
 * Before doing a get the value must be put in the cache with same key. Cache will write back
 * the data periodically.
 * When the cache is full it will free some space by evicting some keys. However, the key-values might
 * have to be written to disk or some other action might be needed. hence whenever a key is evicted.
 * The cache will generate a notification whenever it is about the evict an element. If such notification
 * handler is set then it will be called, otherwise element will be removed.n
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> {

    private LinkedList<CacheNode<K, V>> list;
    private Map<K, CacheNode<K, V>> map;
    private int maximumCapacity;

    /* accept method of provided BiConsumer will be called whenever an element is about
     * to be removed. This method will be called with both the key and value of the entry that is
     * about to be removed. */
    private BiConsumer<K, V> removalListener;

    public LRUCache(int maximumCapacity) {
        list = new LinkedList<>();
        this.maximumCapacity = maximumCapacity;
        map = new HashMap<>();
    }

    public LRUCache(int maximumCapacity,
                    BiConsumer<K, V> removalListener) {
        this(maximumCapacity);
        this.removalListener = removalListener;
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public V get(K key) {
        if (map.containsKey(key)) {
            CacheNode<K, V> node = map.get(key);
            list.remove(node);
            list.add(node);
            return node.t2;
        }
        return null;
    }

    private void removeLRU() {
        CacheNode<K, V> removed = list.remove(0);
        map.remove(removed.t1);
        removalListener.accept(removed.t1, removed.t2);
    }

    public void put(K key, V value) {
        if (list.size() == maximumCapacity)
            removeLRU();
        CacheNode<K, V> node;
        if (map.containsKey(key)) {
            node = map.get(key);
            list.remove(node);
        } else {
            node = new CacheNode<>(key, value);
        }
        list.add(node);
    }



}