package org.rapidgraphql.dataloaders;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.MappedBatchLoader;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class GraphQLMappedBatchLoader<K, T> extends AbstractGraphQLBatchLoader<K, T>  implements MappedBatchLoader<K, T> {

    /**
     * Synchronous batch load
     * @param keys - Set of keys to load
     * @return Map of entities successfully loaded.
     * Keys that are missing in the map will be treated as null entries
     */
    abstract public Map<K,T> syncLoad(Set<K> keys);

    /**
     * This method is called by DataLoader framework to load batch of entities
     * We recommend to implement synchronous variant of this: loadSync
     * @param keys the collection of keys to load
     *
     * @return CompletionStage with Map of results
     */
    @Override
    public CompletionStage<Map<K,T>> load(Set<K> keys) {
        return CompletableFuture.supplyAsync(() -> syncLoad(keys));
    }

    @Override
    protected DataLoader<K, T> createNewDataLoader() {
        return DataLoaderFactory.newMappedDataLoader(this, getDataLoaderOptions());
    }
}
