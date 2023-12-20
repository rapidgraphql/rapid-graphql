package org.rapidgraphql.client;

import org.rapidgraphql.annotations.GraphQLInputType;
import org.rapidgraphql.client.exceptions.RapidGraphQLQueryBuilderException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Map.entry;

public class TypeUtils {
    private static final Predicate<String> notNullPredicate = Pattern.compile("\\b(NotNull|NonNull)\\b").asPredicate();

    private static final Map<Type, String> graphQLTypeNames = Map.ofEntries(
            entry(Integer.class, "Int"),
            entry(Integer.TYPE, "Int!"),
            entry(String.class, "String"),
            entry(Boolean.class, "Boolean"),
            entry(Boolean.TYPE, "Boolean!"),
            entry(Float.class, "Float"),
            entry(Float.TYPE, "Float!"),
            entry(Double.class, "Float"),
            entry(Double.TYPE, "Float!"),
            entry(Short.class, "Short"),
            entry(Short.TYPE, "Short!"),
            entry(Long.class, "Long"),
            entry(Long.TYPE, "Long"),
            entry(Byte.class, "Byte"),
            entry(Byte.TYPE, "Byte!"),
            entry(Character.class, "Char"),
            entry(BigDecimal.class, "BigDecimal"),
            entry(BigInteger.class, "BigInteger"),
            entry(LocalDate.class, "Date"),
            entry(OffsetDateTime.class, "DateTime"),
            entry(java.sql.Timestamp.class, "Timestamp")
    );
    public static String declareType(AnnotatedType annotatedType) {
        String graphqlType;
        if (annotatedType instanceof AnnotatedParameterizedType) {
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) annotatedType;
            if (((ParameterizedType)parameterizedType.getType()).getRawType() == List.class) {
                graphqlType = "[" + declareType(parameterizedType.getAnnotatedActualTypeArguments()[0]) + "]";
            } else {
                throw new RapidGraphQLQueryBuilderException("Unsupported parameterized type " + annotatedType);
            }
        } else if(annotatedType.getType() instanceof Class<?>) {
            graphqlType = Optional.ofNullable(graphQLTypeNames.get(annotatedType.getType()))
                    .orElseGet(() -> processType((Class<?>)annotatedType.getType()));
        } else {
            throw new RapidGraphQLQueryBuilderException("Unsupported type " + annotatedType);
        }
        return !graphqlType.endsWith("!") && isNotNullable(annotatedType) ?
                graphqlType+"!" : graphqlType;
    }

    private static boolean isNotNullable(AnnotatedType annotatedType) {
        Annotation[] annotations = annotatedType.getAnnotations();
        return Arrays.stream(annotations)
                .anyMatch(annotation -> notNullPredicate.test(annotation.toString()));
    }
    private static String processType(Class<?> clazz) {
        GraphQLInputType graphQLInputType = clazz.getAnnotation(GraphQLInputType.class);
        if (graphQLInputType != null) {
            return graphQLInputType.value();
        }
        return clazz.getSimpleName();
    }
}
