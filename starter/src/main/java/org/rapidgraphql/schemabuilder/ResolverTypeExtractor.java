package org.rapidgraphql.schemabuilder;

import graphql.kickstart.tools.GraphQLResolver;
import org.rapidgraphql.exceptions.GraphQLSchemaGenerationException;
import org.rapidgraphql.utils.TypeKind;
import org.slf4j.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.rapidgraphql.utils.TypeUtils.getTypeName;
import static org.slf4j.LoggerFactory.getLogger;

public class ResolverTypeExtractor {
    private static final Logger LOGGER = getLogger(ResolverTypeExtractor.class);

    public static Optional<DiscoveredClass> extractResolverType(GraphQLResolver<?> graphQLResolver) {
        return extractResolverTypeFromClass(graphQLResolver.getClass());
    }

    private static Optional<DiscoveredClass> extractResolverTypeFromClass(Class<?> resolverClass) {
        Optional<Type> resolvedType = Arrays.stream(resolverClass.getGenericInterfaces())
                .filter(genericInterface -> (genericInterface instanceof ParameterizedType))
                .map(genericInterface -> (ParameterizedType)genericInterface)
                .filter(parametrizedInterface -> parametrizedInterface.getRawType().equals(GraphQLResolver.class))
                .map(parameterizedType -> parameterizedType.getActualTypeArguments()[0])
                .findFirst();
        if (resolvedType.isPresent()) {
            if (resolvedType.get() instanceof Class) {
                Class<?> clazz = (Class<?>)resolvedType.get();
                return Optional.of(DiscoveredClass.builder()
                        .name(getTypeName(clazz, TypeKind.OUTPUT_TYPE))
                        .clazz(clazz)
                        .typeKind(TypeKind.OUTPUT_TYPE)
                        .build());
            } else {
                throw new GraphQLSchemaGenerationException("Failed to extract resolver class of " + resolverClass.getTypeName());
            }
        }
        return extractResolverTypeFromGenericType(resolverClass);
    }

    private static Optional<DiscoveredClass> extractResolverTypeFromGenericType(Class<?> resolverClass) {
        Type genericSuperclass = resolverClass.getGenericSuperclass();
        if (genericSuperclass instanceof Class<?>) {
            return extractResolverTypeFromClass((Class<?>) genericSuperclass);
        }
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType superclass = (ParameterizedType)genericSuperclass;
            LOGGER.info("superclass {}", superclass.getTypeName());
            return extractResolverTypeFromClass(superclass);
        }
        return Optional.empty();
    }

    private static Optional<DiscoveredClass> extractResolverTypeFromClass(ParameterizedType resolverSuperclass) {
        Class<?> rawType = (Class<?>)resolverSuperclass.getRawType();
        if (rawType == Object.class) {
            return Optional.empty();
        }
        Map<String, Class<?>> parametersMap = buildActualParametersMap(resolverSuperclass);
        Optional<Type> resolvedType = Arrays.stream(rawType.getGenericInterfaces())
                .filter(genericInterface -> (genericInterface instanceof ParameterizedType))
                .map(genericInterface -> (ParameterizedType)genericInterface)
                .filter(parametrizedInterface -> parametrizedInterface.getRawType().equals(GraphQLResolver.class))
                .map(parameterizedType -> parameterizedType.getActualTypeArguments()[0])
                .findFirst();
        if (resolvedType.isPresent()) {
            if (resolvedType.get() instanceof TypeVariable<?>) {
                TypeVariable<?> typeVariable = (TypeVariable<?>)resolvedType.get();
                Class<?> clazz = parametersMap.get(typeVariable.getName());
                return Optional.of(DiscoveredClass.builder()
                        .name(getTypeName(clazz, TypeKind.OUTPUT_TYPE))
                        .clazz(clazz)
                        .typeKind(TypeKind.OUTPUT_TYPE)
                        .build());
            } else {
                throw new GraphQLSchemaGenerationException("Failed to extract resolver class of " + resolverSuperclass.getTypeName());
            }
        }
        return extractResolverTypeFromGenericType(rawType);
    }

    private static Map<String, Class<?>> buildActualParametersMap(ParameterizedType type) {
        Class<?> rawType = (Class<?>)type.getRawType();
        Map<String, Class<?>> parametersMap = new HashMap<>();
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        int parameterId = 0;
        for(TypeVariable<?> typeVariable: typeParameters) {
            parametersMap.put(typeVariable.getName(), (Class<?>)type.getActualTypeArguments()[parameterId]);
            parameterId ++;
        }

        return parametersMap;
    }

}
