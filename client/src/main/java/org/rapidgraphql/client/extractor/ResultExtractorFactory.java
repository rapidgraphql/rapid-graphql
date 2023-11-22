package org.rapidgraphql.client.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rapidgraphql.client.exceptions.RapidGraphQLUsupportedReturnType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResultExtractorFactory {
    private static final Map<Type, ValueExtractor> valueExtractorCache = new ConcurrentHashMap<>();
    public static ResultExtractor createExtractor(String fieldName, Type fieldGenericType, ObjectMapper objectMapper) {
        ValueExtractor valueExtractor = valueExtractorCache.computeIfAbsent(fieldGenericType,
                type -> resolveValueExtractor(type, objectMapper));
        return new ObjectExtractor(fieldName, valueExtractor);
    }

    private static ValueExtractor resolveValueExtractor(Type fieldGenericType, ObjectMapper objectMapper) {
        if (fieldGenericType instanceof Class<?>) {
            return resolveValueExtractorFromClass((Class<?>) fieldGenericType, objectMapper);
        }
        if (fieldGenericType instanceof ParameterizedType) {
            ParameterizedType parameterizedFieldType = (ParameterizedType) fieldGenericType;
            if (parameterizedFieldType.getRawType().equals(List.class)) {
                return new ListValueExtractor(
                        resolveValueExtractor(parameterizedFieldType.getActualTypeArguments()[0], objectMapper));
            }
        }
        throw new RapidGraphQLUsupportedReturnType("Unsupported return type " + fieldGenericType.getTypeName());
    }

    private static ValueExtractor resolveValueExtractorFromClass(Class<?> fieldClass, ObjectMapper objectMapper) {
        if (fieldClass.isPrimitive()) {
            throw new RapidGraphQLUsupportedReturnType("Can't return primitive type");
        }
        if (fieldClass.isEnum()) {
            return new EnumValueExtractor(fieldClass);
        }
        if (SimpleValueExtractor.isSimpleType(fieldClass)) {
            return new SimpleValueExtractor(fieldClass);
        }
        return new ObjectValueExtractor(fieldClass, objectMapper);
    }
}
