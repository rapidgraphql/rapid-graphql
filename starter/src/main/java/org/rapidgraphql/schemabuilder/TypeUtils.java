package org.rapidgraphql.schemabuilder;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.StringValue;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class TypeUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Predicate<String> notNullPredicate = Pattern.compile("\\b(NotNull|NonNull)\\b").asPredicate();
    private static final Set<Type> simpleTypes = Set.of(
            String.class,
            Integer.class,
            Integer.TYPE,
            Long.class,
            Long.TYPE,
            Float.class,
            Float.TYPE,
            Double.class,
            Double.TYPE,
            Boolean.class,
            Boolean.TYPE,
            Character.class,
            Character.TYPE,
            Short.TYPE,
            Short.class);


    public static boolean isNotNullable(AnnotatedType annotatedType) {
        return Arrays.stream(annotatedType.getAnnotations())
                .anyMatch(annotation -> notNullPredicate.test(annotation.toString()));
    }

    public static Optional<AnnotatedParameterizedType> castToParameterizedType(AnnotatedType annotatedType) {
        if (annotatedType instanceof AnnotatedParameterizedType) {
            return Optional.of((AnnotatedParameterizedType)annotatedType);
        } else {
            return Optional.empty();
        }
    }
    public static boolean isListType(AnnotatedParameterizedType type) {
        Class<?> clazz = baseType(type);
        return clazz.getTypeParameters().length==1 && List.class.isAssignableFrom(clazz);
    }

    public static boolean isListType(Class<?> type) {
        return List.class.isAssignableFrom(type);
    }

    public static Class<?> baseType(AnnotatedParameterizedType annotatedParameterizedType) {
        Type rawType = ((ParameterizedType) annotatedParameterizedType.getType()).getRawType();
        if (!(rawType instanceof Class<?>)) {
            throw new RuntimeException("Parameterized type " + rawType.getTypeName() + " can't be processed");
        }
        return (Class<?>)rawType;
    }

    public static AnnotatedType actualTypeArgument(AnnotatedParameterizedType annotatedParameterizedType, int typeArgumentId) {
        return  annotatedParameterizedType.getAnnotatedActualTypeArguments()[typeArgumentId];
    }

    public static Optional<AnnotatedType> extractListElementType(AnnotatedType annotatedType) {
        return castToParameterizedType(annotatedType)
                .filter(TypeUtils::isListType)
                .map(parameterizedType -> actualTypeArgument(parameterizedType, 0));
    }

    public static Optional<AnnotatedType> extractClassFieldAnnotatedType(Class<?> clazz, String fieldName) {
        try {
            return Optional.of(clazz.getDeclaredField(fieldName).getAnnotatedType());
        } catch (NoSuchFieldException e) {
            return Optional.empty();
        }
    }

    public static Optional<Class<?>> tryGetClass(AnnotatedType annotatedType) {
        if (annotatedType.getType() instanceof Class<?>) {
            return Optional.of((Class<?>)annotatedType.getType());
        } else {
            return Optional.empty();
        }
    }

    public static Class<?> castToClass(Type type) {
        if (!(type instanceof Class<?>)) {
            throw new IllegalArgumentException(format("%s should be class but is not", type.getTypeName()));
        }
        return (Class<?>) type;
    }

    public static JavaType constructJavaType(AnnotatedType annotatedType) {
        Optional<AnnotatedParameterizedType> annotatedParameterizedType = castToParameterizedType(annotatedType);
        if (annotatedParameterizedType.isPresent()) {
            if (isListType(annotatedParameterizedType.get())) {
                return objectMapper.getTypeFactory().constructCollectionType(List.class,
                        constructJavaType(actualTypeArgument(annotatedParameterizedType.get(), 0)
                ));
            } else {
                throw new IllegalArgumentException(format("Parameterized type %s is not supported", annotatedType.getType().getTypeName()));
            }
        }
        return objectMapper.getTypeFactory().constructType(annotatedType.getType());
    }

    public enum ValueType {
        SIMPLE_VALUE, LIST_VALUE, OBJECT_VALUE, ENUM_VALUE
    }
    public static ValueType detectValueType(AnnotatedType annotatedType) {
        if (simpleTypes.contains(annotatedType.getType())) {
            return ValueType.SIMPLE_VALUE;
        }
        Optional<AnnotatedParameterizedType> annotatedParameterizedType = castToParameterizedType(annotatedType);
        if (annotatedParameterizedType.isPresent()) {
            if (isListType(annotatedParameterizedType.get())) {
                return ValueType.LIST_VALUE;
            } else {
                throw new IllegalArgumentException(format("Parameterized type %s is not supported", annotatedType.getType().getTypeName()));
            }
        }
        Class<?> clazz = castToClass(annotatedType.getType());
        if (clazz.isEnum()) {
            return ValueType.ENUM_VALUE;
        }
        return ValueType.OBJECT_VALUE;

    }
}
