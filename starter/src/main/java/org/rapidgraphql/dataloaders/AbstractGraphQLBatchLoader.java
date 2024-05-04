package org.rapidgraphql.dataloaders;

import com.google.common.cache.Cache;
import graphql.schema.DataFetchingEnvironment;
import lombok.Getter;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.registries.DispatchPredicate;
import org.dataloader.registries.ScheduledDataLoaderRegistry;
import org.jetbrains.annotations.NotNull;
import org.rapidgraphql.directives.GraphQLDataLoader;
import org.slf4j.Logger;
import org.springframework.util.ClassUtils;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractGraphQLBatchLoader<K, T> implements GraphQLDataLoader, DataLoaderRegistrar<K, T> {
    private static final Logger LOGGER = getLogger(AbstractGraphQLBatchLoader.class);

    private final String dataLoaderName;
    @Getter
    private final DataLoaderOptions dataLoaderOptions = DataLoaderOptions.newOptions();
    @Getter
    private DataLoader<K, T> scheduledDataLoader;

    @Getter
    private DispatchPredicate dispatchPredicate = DispatchPredicate.DISPATCH_ALWAYS;

    public AbstractGraphQLBatchLoader() {
        dataLoaderName = ClassUtils.getUserClass(getClass()).getName();
    }

    /**
     * Retrieves entity by key using dataLoader instance registered in the data fetching environment
     *
     * @param key - key of entity to retrieve
     * @param env - DataFetchingEnvironment
     * @return CompletableFuture to the entity.
     * Requested entities are resolved in batches when the dispatch on dataloader is called.
     */
    public CompletableFuture<T> get(K key, DataFetchingEnvironment env) {
        DataLoader<K, T> dataLoader = env.getDataLoader(dataLoaderName);
        if (dataLoader == null) {
            dataLoader = registerIn(env.getDataLoaderRegistry());
        }
        return dataLoader.load(key);
    }

    /**
     * Setups Guava cache as ValueCache for DataLoader
     * <code>
     *     useValueCache(CacheBuilder.newBuilder()
     *                 .maximumSize(1000) // Adjust size as per your requirements
     *                 .expireAfterWrite(30, TimeUnit.MINUTES) // Adjust expiry time as per your requirements
     *                 .build());
     * </code>
     * @param cache - initialized Guava cache to store loaded values
     */
    protected void useValueCache(Cache<K, T> cache) {
        getDataLoaderOptions().setValueCache(new GuavaValueCache<>(cache));
    }

    /**
     * Setups maximum batch size for batchload
     * If more than maxBatchSize keys were aggregated multiple asynchronous load requests will be issued
     * @param maxBatchSize - maximum number of keys in each batch
     */
    protected void setMaxBatchSize(int maxBatchSize) {
        getDataLoaderOptions().setMaxBatchSize(maxBatchSize);
    }

    /**
     * Scheduled dispatch allows to batch queries across multiple GraphQL requests.
     * This comes at the cost of increased single request latency.
     * The DataLoader is created just once and used by all requests.
     * Actual dispatch happens only when one of the dispatch conditions is met
     *
     * @param durationSinceLastDispatch - minimum duration to pass since last dispatch
     * @param minDispatchSize - minimal accumulated number of items to dispatch
     */
    protected void useScheduledDispatch(Duration durationSinceLastDispatch, int minDispatchSize) {
        if (scheduledDataLoader == null) {
            scheduledDataLoader = createNewDataLoader();
        }
        dispatchPredicate = DispatchPredicate.dispatchIfLongerThan(durationSinceLastDispatch)
                .or(DispatchPredicate.dispatchIfDepthGreaterThan(minDispatchSize-1));
    }
    @Override
    public DataLoader<K, T> registerIn(DataLoaderRegistry dataLoaderRegistry) {
        synchronized (dataLoaderRegistry) {
            DataLoader<K, T> dataLoader = dataLoaderRegistry.getDataLoader(getDataLoaderName());
            if (dataLoader == null) {
                dataLoader = createOrGetDataLoader();
                if (dataLoaderRegistry instanceof ScheduledDataLoaderRegistry) {
                    LOGGER.info("Registration of {} data loader in ScheduledDataLoaderRegistry", getDataLoaderName());
                    ((ScheduledDataLoaderRegistry)dataLoaderRegistry).register(getDataLoaderName(), dataLoader, getDispatchPredicate());
                } else {
                    LOGGER.info("Registration of {} data loader in DataLoaderRegistry", getDataLoaderName());
                    dataLoaderRegistry.register(getDataLoaderName(), dataLoader);
                }
            }
            return dataLoader;
        }
    }

    @NotNull
    protected DataLoader<K, T> createOrGetDataLoader() {
        if (scheduledDataLoader != null) {
            return scheduledDataLoader;
        }
        return createNewDataLoader();
    }
    @NotNull
    abstract protected DataLoader<K, T> createNewDataLoader();

    @Override
    public boolean isLazyRegistrar() {
        return true;
    }

    @Override
    public boolean isScheduled() {
        return scheduledDataLoader != null;
    }

    @Override
    public String getDataLoaderName() {
        return dataLoaderName;
    }
}
