package org.rapidgraphql.schemabuilder;

import org.dataloader.*;
import org.dataloader.registries.DispatchPredicate;
import org.dataloader.registries.ScheduledDataLoaderRegistry;
import org.rapidgraphql.annotations.DataLoaderMethod;
import org.rapidgraphql.dataloaders.DataLoaderRegistrar;
import org.rapidgraphql.directives.GraphQLDataLoader;
import org.rapidgraphql.utils.MethodsFilter;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class DataLoaderRegistryFactory implements AutoCloseable {
    private static final Logger LOGGER = getLogger(DataLoaderRegistryFactory.class);
    private static final long DEFAULT_RESCHEDULE_INTERVAL_IN_MILLIS = 10;
    private static final int DEFAULT_SCHEDULER_POOL_SIZE = 1;

    private final List<DataLoaderRegistrar> registrars = new ArrayList<>();

    private final boolean createScheduledLoader;
    private final ScheduledExecutorService scheduledExecutionService;
    private final Duration scheduleDuration;
    private boolean closed = false;
    private final Set<String> scheduledDataLoaders;
    private final DispatchPredicate dispatchPredicate;

    public DataLoaderRegistryFactory(List<? extends GraphQLDataLoader> dataLoaders) {
        this(dataLoaders, DEFAULT_RESCHEDULE_INTERVAL_IN_MILLIS, DEFAULT_SCHEDULER_POOL_SIZE);
    }
    public DataLoaderRegistryFactory(List<? extends GraphQLDataLoader> dataLoaders,
                                     long rescheduleIntervalInMillis, int schedulerPoolSize) {
        for(GraphQLDataLoader graphQLDataLoader: dataLoaders) {
            if (graphQLDataLoader instanceof DataLoaderRegistrar) {
                registrars.add((DataLoaderRegistrar)graphQLDataLoader);
            } else {
                Method[] dataLoaderMethods = MethodsFilter.getDataLoaderMethods(graphQLDataLoader.getClass());
                Arrays.stream(dataLoaderMethods)
                        .map(method -> createRegistrar(method, graphQLDataLoader))
                        .filter(Objects::nonNull)
                        .forEach(registrars::add);
            }
        }
        createScheduledLoader = registrars.stream().anyMatch(DataLoaderRegistrar::isScheduled);
        scheduleDuration = Duration.ofMillis(rescheduleIntervalInMillis);
        if (createScheduledLoader) {
            scheduledExecutionService = Executors.newScheduledThreadPool(schedulerPoolSize);
            scheduledDataLoaders = registrars.stream()
                    .filter(DataLoaderRegistrar::isScheduled)
                    .map(DataLoaderRegistrar::getDataLoaderName)
                    .collect(Collectors.toSet());
            dispatchPredicate = ((dataLoaderKey, dataLoader) -> !scheduledDataLoaders.contains(dataLoaderKey));
        } else {
            scheduledExecutionService = null;
            scheduledDataLoaders = null;
            dispatchPredicate = null;
        }
    }

    public DataLoaderRegistry build() {
        DataLoaderRegistry dataLoaderRegistry;
        if (createScheduledLoader) {
            LOGGER.debug("building ScheduledDataLoaderRegistry");
            dataLoaderRegistry = ScheduledDataLoaderRegistry.newScheduledRegistry()
                        .scheduledExecutorService(scheduledExecutionService)
                        .schedule(scheduleDuration)
                        .dispatchPredicate(dispatchPredicate)
                        .build();
        } else {
            LOGGER.debug("building DataLoaderRegistry");
            dataLoaderRegistry = new DataLoaderRegistry();
        }
        registrars.stream()
                .filter(dataLoaderRegistrar -> !dataLoaderRegistrar.isLazyRegistrar())
                .forEach(registrar -> registrar.registerIn(dataLoaderRegistry));
        return dataLoaderRegistry;
    }

    private String getDataLoaderName(Method method) {
        return method.getAnnotation(DataLoaderMethod.class).value();
    }

    private DataLoaderRegistrar createRegistrar(Method method, GraphQLDataLoader dataLoader) {
        String dataLoaderName = getDataLoaderName(method);
        if (method.getReturnType().equals(List.class)) {
            return new BatchLoaderMethod(dataLoaderName, dataLoader, method);
        } else if (method.getReturnType().equals(Map.class)) {
            return new MappedBatchLoaderMethod(dataLoaderName, dataLoader, method);
        }
        LOGGER.error("Cannot create dataloader {} from method {}.{}, invalid return type {}",
                dataLoaderName, method.getDeclaringClass().getSimpleName(), method.getName(), method.getReturnType().getSimpleName());
        return null;

    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (scheduledExecutionService != null) {
                scheduledExecutionService.shutdown();
                try {
                    // Wait a while for existing tasks to terminate
                    if (!scheduledExecutionService.awaitTermination(1, TimeUnit.SECONDS)) {
                        scheduledExecutionService.shutdownNow(); // Cancel currently executing tasks
                        // Wait a while for tasks to respond to being cancelled
                        if (!scheduledExecutionService.awaitTermination(2, TimeUnit.SECONDS)) {
                            LOGGER.error("Scheduled execution service pool did not terminate correctly");
                        }
                    }
                } catch (InterruptedException ex) {
                    LOGGER.warn("Scheduled execution service shutdown was interrupted");
                    // (Re-)Cancel if current thread also interrupted
                    scheduledExecutionService.shutdownNow();
                    // Preserve interrupt status
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static class BatchLoaderMethod implements BatchLoader<Object, Object>, DataLoaderRegistrar<Object, Object> {
        private final GraphQLDataLoader graphQLDataLoader;
        private final Method method;
        private final String name;

        public BatchLoaderMethod(String name, GraphQLDataLoader graphQLDataLoader, Method method) {
            LOGGER.info("Registered batch dataloader {} for method {}.{}",
                    name, method.getDeclaringClass().getSimpleName(), method.getName());
            this.name = name;
            this.graphQLDataLoader = graphQLDataLoader;
            this.method = method;
        }

        @Override
        public CompletionStage<List<Object>> load(List<Object> keys) {
            return CompletableFuture.supplyAsync(() -> invoke(keys));
        }

        private List<Object> invoke(List<Object> keys) {
            try {
                return (List<Object>)method.invoke(graphQLDataLoader, keys);
            } catch (Exception e) {
                LOGGER.error("Method invocation error", e);
                Object[] arr = new Object[keys.size()];
                Arrays.fill(arr, null);
                return Arrays.asList(arr);
            }
        }

        @Override
        public DataLoader<Object, Object> registerIn(DataLoaderRegistry dataLoaderRegistry) {
            DataLoader<Object, Object> dataLoader = DataLoaderFactory.newDataLoader(this);
            dataLoaderRegistry.register(name, dataLoader);
            return dataLoader;
        }

        @Override
        public String getDataLoaderName() {
            return name;
        }
    }
    public static class MappedBatchLoaderMethod implements MappedBatchLoader<Object, Object>, DataLoaderRegistrar<Object, Object> {
        private final String name;
        private final GraphQLDataLoader graphQLDataLoader;
        private final Method method;

        public MappedBatchLoaderMethod(String name, GraphQLDataLoader graphQLDataLoader, Method method) {
            LOGGER.info("Registered mapped batch dataloader {} for method {}.{}",
                    name, method.getDeclaringClass().getSimpleName(), method.getName());
            this.name = name;
            this.graphQLDataLoader = graphQLDataLoader;
            this.method = method;
        }

        @Override
        public CompletionStage<Map<Object,Object>> load(Set<Object> keys) {
            return CompletableFuture.supplyAsync(() -> invoke(keys));
        }

        private Map<Object,Object> invoke(Set<Object> keys) {
            try {
                return (Map<Object, Object>)method.invoke(graphQLDataLoader, keys);
            } catch (Exception e) {
                LOGGER.error("Method invocation error", e);
                return Map.of();
            }
        }

        @Override
        public DataLoader<Object, Object> registerIn(DataLoaderRegistry dataLoaderRegistry) {
            DataLoader<Object, Object> dataLoader = DataLoaderFactory.newMappedDataLoader(this);
            dataLoaderRegistry.register(name, dataLoader);
            return dataLoader;
        }

        @Override
        public String getDataLoaderName() {
            return name;
        }
    }
}
