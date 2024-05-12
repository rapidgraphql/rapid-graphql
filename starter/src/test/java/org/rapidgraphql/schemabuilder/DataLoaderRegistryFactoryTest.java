package org.rapidgraphql.schemabuilder;

import com.google.common.cache.CacheBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.Getter;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.registries.ScheduledDataLoaderRegistry;
import org.junit.jupiter.api.Test;
import org.rapidgraphql.dataloaders.GraphQLMappedBatchLoader;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataLoaderRegistryFactoryTest {

    public static class MultiplicationMappedLoader extends GraphQLMappedBatchLoader<Integer, Integer> {
        @Getter
        private int numLoads = 0;
        @Override
        public Map<Integer, Integer> syncLoad(Set<Integer> keys) {
            numLoads += keys.size();
            return keys.stream().collect(Collectors.toMap(key -> key, key -> key*2));
        }
    }

    public static class ScheduledLoader extends MultiplicationMappedLoader {
        public ScheduledLoader(long durationSinceLastDispatchInMillis, int minDispatchSize) {
            useScheduledDispatch(Duration.ofMillis(durationSinceLastDispatchInMillis), minDispatchSize);
        }
    }

    public static class LoaderWithValueCache extends MultiplicationMappedLoader {
        public LoaderWithValueCache() {
            useValueCache(CacheBuilder.newBuilder()
                    .maximumSize(1000) // Adjust size as per your requirements
                    .build());
        }
    }

    public static class LoaderWithFutureCache extends MultiplicationMappedLoader {
        public LoaderWithFutureCache() {
            useFutureCache(CacheBuilder.newBuilder()
                    .maximumSize(1000) // Adjust size as per your requirements
                    .build());
        }
    }

    @Test
    public void dispatchImmediately() throws InterruptedException {
        int MIN_DISPATCH_SIZE = 5;
        MultiplicationMappedLoader loader = new MultiplicationMappedLoader();
        DataLoaderRegistryFactory dataLoaderRegistryFactory = new DataLoaderRegistryFactory(List.of(loader));
        List<CompletableFuture<Integer>> promises = new ArrayList<>();
        DataLoaderRegistry registry = dataLoaderRegistryFactory.build();
        assertThat(registry).isNotInstanceOf(ScheduledDataLoaderRegistry.class);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        when(env.getDataLoaderRegistry()).thenReturn(registry);
        for(int i = 0; i < MIN_DISPATCH_SIZE; i++) {
            CompletableFuture<Integer> promise = loader.get(i, env);
            promises.add(promise);
        }
        int dispatchAllWithCount = registry.dispatchAllWithCount();
        assertThat(dispatchAllWithCount).isEqualTo(MIN_DISPATCH_SIZE);
        Thread.sleep(10); // to let complete async batch load
        for (int i = 0; i < promises.size(); i++) {
            assertThat(promises.get(i)).isCompletedWithValue(i*2);
        }
        dataLoaderRegistryFactory.close();
    }

    @Test
    public void dispatchWithValueCache() throws InterruptedException {
        int MIN_DISPATCH_SIZE = 5;
        LoaderWithValueCache loader = new LoaderWithValueCache();
        DataLoaderRegistryFactory dataLoaderRegistryFactory = new DataLoaderRegistryFactory(List.of(loader));
        List<CompletableFuture<Integer>> promises = new ArrayList<>();
        DataLoaderRegistry registry = dataLoaderRegistryFactory.build();
        assertThat(registry).isNotInstanceOf(ScheduledDataLoaderRegistry.class);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        when(env.getDataLoaderRegistry()).thenReturn(registry);
        for(int i = 0; i < MIN_DISPATCH_SIZE; i++) {
            CompletableFuture<Integer> promise = loader.get(i, env);
            promises.add(promise);
        }
        int dispatchAllWithCount = registry.dispatchAllWithCount();
        assertThat(dispatchAllWithCount).isEqualTo(MIN_DISPATCH_SIZE);
        Thread.sleep(10); // to let complete async batch load
        assertThat(loader.getNumLoads()).isEqualTo(MIN_DISPATCH_SIZE);
        for (int i = 0; i < promises.size(); i++) {
            assertThat(promises.get(i)).isCompletedWithValue(i*2);
        }
        // second iteration no load should happen:
        registry = dataLoaderRegistryFactory.build();
        assertThat(registry).isNotInstanceOf(ScheduledDataLoaderRegistry.class);
        env = mock(DataFetchingEnvironment.class);
        when(env.getDataLoaderRegistry()).thenReturn(registry);
        promises = new ArrayList<>();
        for(int i = 0; i < MIN_DISPATCH_SIZE; i++) {
            CompletableFuture<Integer> promise = loader.get(i, env);
            promises.add(promise);
        }
        dispatchAllWithCount = registry.dispatchAllWithCount();
        assertThat(dispatchAllWithCount).isEqualTo(MIN_DISPATCH_SIZE); // no new calls done to load function
        Thread.sleep(10); // to let complete async batch load
        for (int i = 0; i < promises.size(); i++) {
            assertThat(promises.get(i)).isCompletedWithValue(i*2);
        }
        assertThat(loader.getNumLoads()).isEqualTo(MIN_DISPATCH_SIZE);

        dataLoaderRegistryFactory.close();
    }

    @Test
    public void dispatchWithFutureCache() throws InterruptedException {
        int MIN_DISPATCH_SIZE = 5;
        LoaderWithFutureCache loader = new LoaderWithFutureCache();
        DataLoaderRegistryFactory dataLoaderRegistryFactory = new DataLoaderRegistryFactory(List.of(loader));
        List<CompletableFuture<Integer>> promises = new ArrayList<>();
        DataLoaderRegistry registry = dataLoaderRegistryFactory.build();
        assertThat(registry).isNotInstanceOf(ScheduledDataLoaderRegistry.class);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        when(env.getDataLoaderRegistry()).thenReturn(registry);
        for(int i = 0; i < MIN_DISPATCH_SIZE; i++) {
            CompletableFuture<Integer> promise = loader.get(i, env);
            promises.add(promise);
        }
        int dispatchAllWithCount = registry.dispatchAllWithCount();
        assertThat(dispatchAllWithCount).isEqualTo(MIN_DISPATCH_SIZE);
        Thread.sleep(10); // to let complete async batch load
        assertThat(loader.getNumLoads()).isEqualTo(MIN_DISPATCH_SIZE);
        for (int i = 0; i < promises.size(); i++) {
            assertThat(promises.get(i)).isCompletedWithValue(i*2);
        }
        // second iteration no load should happen:
        registry = dataLoaderRegistryFactory.build();
        assertThat(registry).isNotInstanceOf(ScheduledDataLoaderRegistry.class);
        env = mock(DataFetchingEnvironment.class);
        when(env.getDataLoaderRegistry()).thenReturn(registry);
        for(int i = 0; i < MIN_DISPATCH_SIZE; i++) {
            CompletableFuture<Integer> promise = loader.get(i, env);
            assertThat(promise).isCompletedWithValue(i*2);
        }
        dispatchAllWithCount = registry.dispatchAllWithCount();
        assertThat(dispatchAllWithCount).isEqualTo(0); // no new calls done to load function
        assertThat(loader.getNumLoads()).isEqualTo(MIN_DISPATCH_SIZE);

        dataLoaderRegistryFactory.close();
    }

    @Test
    public void testDispatchOnSize() throws InterruptedException {
        int MIN_DISPATCH_SIZE = 5;
        ScheduledLoader loader = new ScheduledLoader(1000_000, MIN_DISPATCH_SIZE);
        DataLoaderRegistryFactory dataLoaderRegistryFactory = new DataLoaderRegistryFactory(List.of(loader), 1_000, 1);
        List<CompletableFuture<Integer>> promises = new ArrayList<>();
        for(int i = 0; i < MIN_DISPATCH_SIZE; i++) {
            DataLoaderRegistry registry = dataLoaderRegistryFactory.build();
            assertThat(registry).isInstanceOf(ScheduledDataLoaderRegistry.class);
            DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
            when(env.getDataLoaderRegistry()).thenReturn(registry);
            CompletableFuture<Integer> promise = loader.get(i, env);
            promises.add(promise);
            int dispatchAllWithCount = registry.dispatchAllWithCount();
            if (i == (MIN_DISPATCH_SIZE-1)) {
                assertThat(dispatchAllWithCount).isEqualTo(MIN_DISPATCH_SIZE);
            } else {
                assertThat(dispatchAllWithCount).isEqualTo(0);
            }
        }
        Thread.sleep(10); // to let complete async batch load
        for (int i = 0; i < promises.size(); i++) {
            assertThat(promises.get(i)).isCompletedWithValue(i*2);
        }
        dataLoaderRegistryFactory.close();
    }

    @Test
    public void testDispatchOnTime() throws InterruptedException {
        int DISPATCH_SIZE = 5;
        int DURATION_SINCE_LAST_DISPATCH_IN_MILLIS = 2000;
        ScheduledLoader loader = new ScheduledLoader(DURATION_SINCE_LAST_DISPATCH_IN_MILLIS, 100);
        DataLoaderRegistryFactory dataLoaderRegistryFactory = new DataLoaderRegistryFactory(List.of(loader), 100, 1);
        DataLoader<Integer, Integer> scheduledDataLoader = loader.getSharedDataLoader();
        assertThat(scheduledDataLoader).isNotNull();
        List<CompletableFuture<Integer>> promises = new ArrayList<>();
        for(int i = 0; i < DISPATCH_SIZE; i++) {
            DataLoaderRegistry registry = dataLoaderRegistryFactory.build();
            assertThat(registry).isInstanceOf(ScheduledDataLoaderRegistry.class);
            DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
            when(env.getDataLoaderRegistry()).thenReturn(registry);
            CompletableFuture<Integer> promise = loader.get(i, env);
            promises.add(promise);
            int dispatchAllWithCount = registry.dispatchAllWithCount();
            assertThat(dispatchAllWithCount).isEqualTo(0);
        }
        Thread.sleep(DURATION_SINCE_LAST_DISPATCH_IN_MILLIS + 100);
        for (int i = 0; i < promises.size(); i++) {
            assertThat(promises.get(i)).isCompletedWithValue(i*2);
        }
        dataLoaderRegistryFactory.close();
    }
}

