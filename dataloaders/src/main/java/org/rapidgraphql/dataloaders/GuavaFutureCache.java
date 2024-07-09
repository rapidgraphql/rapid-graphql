package org.rapidgraphql.dataloaders;

import com.google.common.cache.Cache;
import org.dataloader.CacheMap;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class GuavaFutureCache<K, V> implements CacheMap<K, V> {
    private final Cache<K, CompletableFuture<V>> cache;

    public GuavaFutureCache(Cache<K, CompletableFuture<V>> cache) {
        this.cache = cache;
    }

    @Override
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public CompletableFuture<V> get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public Collection<CompletableFuture<V>> getAll() {
        return cache.asMap().values().stream().toList();
    }

    @Override
    public CacheMap<K, V> set(K key, CompletableFuture<V> value) {
        cache.put(key, value);
        return this;
    }

    @Override
    public CacheMap<K, V> delete(K key) {
        cache.invalidate(key);
        return this;
    }

    @Override
    public CacheMap<K, V> clear() {
        cache.invalidateAll();
        return this;
    }
}
