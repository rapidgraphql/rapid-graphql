package org.rapidgraphql.schemabuilder;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TypeUtils {
    private static final Predicate<String> notNullPredicate = Pattern.compile("\\b(NotNull|NonNull)\\b").asPredicate();
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
}
