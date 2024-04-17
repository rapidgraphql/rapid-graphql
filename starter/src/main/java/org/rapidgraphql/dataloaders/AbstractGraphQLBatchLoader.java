package org.rapidgraphql.dataloaders;

import com.google.common.cache.Cache;
import graphql.schema.DataFetchingEnvironment;
import lombok.Getter;
import org.dataloader.*;
import org.rapidgraphql.directives.GraphQLDataLoader;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractGraphQLBatchLoader<K, T> implements GraphQLDataLoader, DataLoaderRegistrar {
    @Getter
    private final String dataLoaderName;
    @Getter
    private final DataLoaderOptions dataLoaderOptions = DataLoaderOptions.newOptions();
    public AbstractGraphQLBatchLoader() {
        dataLoaderName = ClassUtils.getUserClass(getClass()).getName();
    }

    /**
     * Retrieves entity by key using dataLoader instance registered in the data fetching environment
     * @param key - key of entity to retrieve
     * @param env - DataFetchingEnvironment
     * @return CompletableFuture to the entity. All entities are resolved in a single batch.
     */
    public CompletableFuture<T> get(K key, DataFetchingEnvironment env) {
        DataLoader<K, T> dataLoader = env.getDataLoader(dataLoaderName);
        return dataLoader.load(key);
    }

    public void useValueCache(Cache<K, T> cache) {
        getDataLoaderOptions().setValueCache(new GuavaValueCache<>(cache));
    }

    public void setMaxBatchSize(int maxBatchSize) {
        getDataLoaderOptions().setMaxBatchSize(maxBatchSize);
    }
}
