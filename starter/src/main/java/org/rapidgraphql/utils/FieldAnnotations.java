package org.rapidgraphql.utils;

import org.rapidgraphql.annotations.GraphQLIgnore;
import org.rapidgraphql.annotations.GraphQLInputType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldAnnotations {
    private final Map<String, Annotation[]> fieldsWithAnnotations;
    private final Set<String> typeLevelIgnoredFields;
    private static final Predicate<String> notNullPredicate = Pattern.compile("\\b(NotNull|NonNull)\\b").asPredicate();

    public static boolean containsNotNullableAnnotation(Annotation[] annotations) {
        if (annotations == null) {
            return false;
        }
        return Arrays.stream(annotations)
                .anyMatch(annotation -> notNullPredicate.test(annotation.toString()));
    }

    public FieldAnnotations(Class<?> clazz, TypeKind typeKind) {
        if (clazz.isInterface()) {
            typeLevelIgnoredFields = Set.of();
            fieldsWithAnnotations = Map.of();
        } else {
            typeLevelIgnoredFields = getTypeLevelIgnoredFields(clazz, typeKind);
            fieldsWithAnnotations = getAllFields(clazz)
                    .map(field -> Map.entry(field.getName(), field.getAnnotations()))
                    .filter(e -> e.getValue().length > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private Stream<Field> getAllFields(Class<?> clazz) {
        Stream.Builder<Class<?>> classHierarchyStreamBuilder = Stream.builder();
        do {
            classHierarchyStreamBuilder.accept(clazz);
            clazz = clazz.getSuperclass();
        } while(clazz != Object.class);
        return classHierarchyStreamBuilder.build()
                .flatMap(c -> Arrays.stream(c.getDeclaredFields()));
    }
    private static Set<String> getTypeLevelIgnoredFields(Class<?> clazz, TypeKind typeKind) {
        if (typeKind == TypeKind.INPUT_TYPE) {
            GraphQLInputType annotation = clazz.getAnnotation(GraphQLInputType.class);
            if (annotation == null) {
                return Set.of();
            }
            return Set.of(annotation.ignore());
        }
        return  Set.of();
    }

    public boolean isFieldIgnored(String fieldName) {
        if (typeLevelIgnoredFields.contains(fieldName)) {
            return true;
        }
        return findAnnotation(fieldName, GraphQLIgnore.class).isPresent();
    }

    public boolean isFieldNotNull(String fieldName) {
        Annotation[] annotations = fieldsWithAnnotations.get(fieldName);
        return containsNotNullableAnnotation(annotations);
    }

    public Annotation[] getFieldAnnotations(String fieldName) {
        return fieldsWithAnnotations.get(fieldName);
    }

    public Optional<Annotation> findAnnotation(String fieldName, Class<?> annotationClass) {
        Annotation[] annotations = fieldsWithAnnotations.get(fieldName);
        if (annotations == null) {
            return Optional.empty();
        }
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationClass) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }
}
