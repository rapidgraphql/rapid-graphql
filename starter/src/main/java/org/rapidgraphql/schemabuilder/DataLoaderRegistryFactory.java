package org.rapidgraphql.schemabuilder;

import org.dataloader.*;
import org.rapidgraphql.annotations.DataLoaderMethod;
import org.rapidgraphql.dataloaders.DataLoaderRegistrar;
import org.rapidgraphql.directives.GraphQLDataLoader;
import org.rapidgraphql.utils.MethodsFilter;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.slf4j.LoggerFactory.getLogger;

public class DataLoaderRegistryFactory {
    private static final Logger LOGGER = getLogger(DataLoaderRegistryFactory.class);

    private final List<DataLoaderRegistrar> registrars = new ArrayList<>();

    public DataLoaderRegistryFactory(List<? extends GraphQLDataLoader> dataLoaders) {
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
    }

    public DataLoaderRegistry build() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        registrars.forEach(registrar -> registrar.registerIn(dataLoaderRegistry));
        return dataLoaderRegistry;
    }

    private String getDataLoaderName(Method method) {
        return method.getAnnotation(DataLoaderMethod.class).value();
    }

    private org.rapidgraphql.dataloaders.DataLoaderRegistrar createRegistrar(Method method, GraphQLDataLoader dataLoader) {
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

    public static class BatchLoaderMethod implements BatchLoader<Object, Object>, DataLoaderRegistrar {
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
        public void registerIn(DataLoaderRegistry dataLoaderRegistry) {
            dataLoaderRegistry.register(name, DataLoaderFactory.newDataLoader(this));
        }
    }
    public static class MappedBatchLoaderMethod implements MappedBatchLoader<Object, Object>, DataLoaderRegistrar {
        private String name;
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
        public void registerIn(DataLoaderRegistry dataLoaderRegistry) {
            dataLoaderRegistry.register(name, DataLoaderFactory.newMappedDataLoader(this));
        }
    }
}
