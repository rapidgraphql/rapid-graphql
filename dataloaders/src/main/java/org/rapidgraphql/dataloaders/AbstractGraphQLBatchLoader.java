package org.rapidgraphql.dataloaders;

import com.google.common.cache.Cache;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.Getter;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.ValueCacheOptions;
import org.dataloader.registries.DispatchPredicate;
import org.dataloader.registries.ScheduledDataLoaderRegistry;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.rapidgraphql.dataloaders.TypeUtils.getUserClass;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractGraphQLBatchLoader<K, T> implements GraphQLDataLoader, DataLoaderRegistrar<K, T> {
    private static final Logger LOGGER = getLogger(AbstractGraphQLBatchLoader.class);
    private static final DispatchPredicate DISPATCH_IF_EMPTY = (dataLoaderKey, dataLoader) -> dataLoader.dispatchDepth()==0;

    private final String dataLoaderName;
    @Getter
    private final DataLoaderOptions dataLoaderOptions = DataLoaderOptions.newOptions();
    @Getter
    private DataLoader<K, T> sharedDataLoader;

    @Getter
    private DispatchPredicate dispatchPredicate = DispatchPredicate.DISPATCH_ALWAYS;
    private boolean isScheduled = false;

    public AbstractGraphQLBatchLoader() {
        this.dataLoaderName = getUserClass(getClass()).getName();
    }

    public AbstractGraphQLBatchLoader(String dataLoaderName) {
        this.dataLoaderName = dataLoaderName;
    }

    /**
     * Retrieves promise to entity by key using dataLoader instance registered in the data fetching environment
     * Creates and registers dataloader lazily if it isn't present in the environment
     * Entity will be fetched only when the dispatch will be scheduled
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
     * Retrieves promise to list of entities by list of keys using dataLoader instance registered in the data fetching environment
     * Creates and registers dataloader lazily if it isn't present in the environment
     * Entity will be fetched only when the dispatch will be scheduled
     *
     * @param keys - list of keys to retrieve
     * @param env - DataFetchingEnvironment
     * @return CompletableFuture to the retrieved entities.
     * Requested entities are resolved in batches when the dispatch on dataloader is called.
     */
    public CompletableFuture<List<T>> getMany(List<K> keys, DataFetchingEnvironment env) {
        DataLoader<K, T> dataLoader = env.getDataLoader(dataLoaderName);
        if (dataLoader == null) {
            dataLoader = registerIn(env.getDataLoaderRegistry());
        }
        return dataLoader.loadMany(keys);
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
        getDataLoaderOptions()
                .setValueCache(new GuavaValueCache<>(cache))
                .setValueCacheOptions(ValueCacheOptions.newOptions().setCompleteValueAfterCacheSet(true));
    }
    /**
     * Setups Guava cache as FutureCache for DataLoader
     * FutureCache is the most efficient way to cache entities. It will store completed futures
     * <code>
     *     useFutureCache(CacheBuilder.newBuilder()
     *                 .maximumSize(1000) // Adjust size as per your requirements
     *                 .expireAfterWrite(30, TimeUnit.MINUTES) // Adjust expiry time as per your requirements
     *                 .build());
     * </code>
     * @param cache - initialized Guava cache to store CompletableFutures for loaded values
     */
    protected void useFutureCache(Cache<K, CompletableFuture<T>> cache) {
        if (sharedDataLoader == null) {
            sharedDataLoader = createNewDataLoader();
        }
        getDataLoaderOptions()
                .setCacheMap(new GuavaFutureCache<>(cache))
                .setCachingEnabled(true);
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
        isScheduled = true;
        if (sharedDataLoader == null) {
            sharedDataLoader = createNewDataLoader();
        }
        dispatchPredicate = DISPATCH_IF_EMPTY
                .or(DispatchPredicate.dispatchIfLongerThan(durationSinceLastDispatch))
                .or(DispatchPredicate.dispatchIfDepthGreaterThan(minDispatchSize-1));
        // disable caching until explicit CacheMap is defined
        if (dataLoaderOptions.cacheMap().isEmpty()) {
            dataLoaderOptions.setCachingEnabled(false);
        }
    }

    /**
     * Creates dataFetcher that uses underlying dataloader to fetch data.
     * Key for data loader is extracted using keyExtractor function
     * @param keyExtractor
     * @return dataFetcher that can be used to fetch data
     */
    public DataFetcher<CompletableFuture<T>> dataFetcher(Function<DataFetchingEnvironment, K> keyExtractor) {
        return environment -> {
            K key = keyExtractor.apply(environment);
            return get(key, environment);
        };
    }

    public static <KEY> Function<DataFetchingEnvironment, KEY> keyExtractorFromMapSource(String keyName) {
        return environment -> (KEY)((Map)environment.getSource()).get(keyName);
    }

    public static <KEY> Function<DataFetchingEnvironment, ?> keyExtractorFromArgument(String argName) {
        return environment -> (KEY)environment.getArgument(argName);
    }

    @Override
    public DataLoader<K, T> registerIn(DataLoaderRegistry dataLoaderRegistry) {
        synchronized (dataLoaderRegistry) {
            DataLoader<K, T> dataLoader = dataLoaderRegistry.getDataLoader(getDataLoaderName());
            if (dataLoader == null) {
                dataLoader = createOrGetDataLoader();
                if (dataLoaderRegistry instanceof ScheduledDataLoaderRegistry) {
                    LOGGER.debug("Registration of {} data loader in ScheduledDataLoaderRegistry", getDataLoaderName());
                    ((ScheduledDataLoaderRegistry)dataLoaderRegistry).register(getDataLoaderName(), dataLoader, getDispatchPredicate());
                } else {
                    LOGGER.debug("Registration of {} data loader in DataLoaderRegistry", getDataLoaderName());
                    dataLoaderRegistry.register(getDataLoaderName(), dataLoader);
                }
            }
            return dataLoader;
        }
    }

    protected DataLoader<K, T> createOrGetDataLoader() {
        if (sharedDataLoader != null) {
            return sharedDataLoader;
        }
        return createNewDataLoader();
    }
    abstract protected DataLoader<K, T> createNewDataLoader();

    @Override
    public boolean isLazyRegistrar() {
        return true;
    }

    @Override
    public boolean isScheduled() {
        return isScheduled;
    }

    @Override
    public String getDataLoaderName() {
        return dataLoaderName;
    }

}
