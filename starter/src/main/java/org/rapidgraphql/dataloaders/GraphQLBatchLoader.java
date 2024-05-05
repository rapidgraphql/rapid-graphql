package org.rapidgraphql.dataloaders;

import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class GraphQLBatchLoader<K, T> extends AbstractGraphQLBatchLoader<K, T> implements BatchLoader<K, T> {

    /**
     * This method is called by DataLoader framework to load batch of entities
     * We recommend to implement synchronous variant of this: loadSync
     * @param keys the collection of keys to load
     *
     * @return a promise of the values for those keys in the same order
     */
    @Override
    public CompletionStage<List<T>> load(List<K> keys) {
        return CompletableFuture.supplyAsync(() -> syncLoad(keys));
    }

    abstract public List<T> syncLoad(List<K> keys);

    @Override
    @NotNull
    protected DataLoader<K, T> createNewDataLoader() {
        return DataLoaderFactory.newDataLoader(this, getDataLoaderOptions());
    }
}
