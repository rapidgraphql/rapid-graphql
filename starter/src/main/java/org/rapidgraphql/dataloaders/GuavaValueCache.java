package org.rapidgraphql.dataloaders;

import com.google.common.cache.Cache;
import org.dataloader.ValueCache;

import java.util.concurrent.CompletableFuture;

public class GuavaValueCache<K,V> implements ValueCache<K,V> {
    public static class EntityNotFoundException extends RuntimeException {
    }
    private final Cache<K,V> cache;
    private final EntityNotFoundException defaultException = new EntityNotFoundException();
    public GuavaValueCache(Cache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public CompletableFuture<V> get(K key) {
        V value = cache.getIfPresent(key);
        return value==null ?
                CompletableFuture.failedFuture(defaultException)
                : CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<V> set(K key, V value) {
        cache.put(key, value);
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public CompletableFuture<Void> delete(K key) {
        cache.invalidate(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> clear() {
        cache.invalidateAll();
        return CompletableFuture.completedFuture(null);
    }
}
