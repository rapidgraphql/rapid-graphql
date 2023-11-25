package org.rapidgraphql.client;

import org.rapidgraphql.client.annotations.GraphQL;
import org.rapidgraphql.client.annotations.GraphQLMutation;
import org.rapidgraphql.client.annotations.GraphQLQuery;
import org.rapidgraphql.client.exceptions.RapidGraphQLQueryBuilderException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class GraphQLRequestBuilder {
    public static GraphQLRequestBody build(Method method, Object[] args) {
        GraphQLRequestBody request = initializeRequest(method);
        request.setVariables(buildVariables(method, args));
        return request;
    }

    private static GraphQLRequestBody initializeRequest(Method method) {
        Optional<GraphQLRequestBody> request;
        request = Optional.ofNullable(method.getAnnotation(GraphQL.class))
                .map(graphQL -> initializeRequest(graphQL, method) );
        if (request.isPresent()) {
            return request.get();
        }
        request = Optional.ofNullable(method.getAnnotation(GraphQLQuery.class))
                .map(graphQLQuery -> initializeRequest(graphQLQuery, method) );
        if (request.isPresent()) {
            return request.get();
        }
        request = Optional.ofNullable(method.getAnnotation(GraphQLMutation.class))
                .map(graphQLMutation -> initializeRequest(graphQLMutation, method) );
        if (request.isPresent()) {
            return request.get();
        }
        throw new RuntimeException("No @GraphQL/@GraphQL annotation defined on method " + method.getName());
    }

    private static GraphQLRequestBody initializeRequest(GraphQLQuery graphQLQuery, Method method) {
        return initializeRequest("query", graphQLQuery.value(), method);
    }
    private static GraphQLRequestBody initializeRequest(GraphQLMutation graphQLQuery, Method method) {
        return initializeRequest("mutation", graphQLQuery.value(), method);
    }

    private static GraphQLRequestBody initializeRequest(String queryType, String query, Method method) {
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBody.builder()
                .fieldName(method.getName())
                .build();
        StringBuilder queryBuilder = new StringBuilder(queryType);
        queryBuilder.append(' ');
        queryBuilder.append(method.getName());
        addVariablesDeclaration(queryBuilder, method);
        queryBuilder.append('{');
        if (!(tryDefaultQueryOfSimpleType(query, method, queryBuilder) ||
              tryQueryOfObjectType(query, method, queryBuilder) ||
              tryCustomQuery(query, method, queryBuilder, graphQLRequestBody))) {
            throw new RapidGraphQLQueryBuilderException("Invalid query spacified in annotation");
        }
        queryBuilder.append('}');
        graphQLRequestBody.setQuery(queryBuilder.toString());
        return graphQLRequestBody;
    }

    private static final Pattern DEFAULT_QUERY = Pattern.compile("^\\s*\\{\\s*}\\s*$");
    private static final Predicate<String> defaultQueryPredicate = DEFAULT_QUERY.asMatchPredicate();
    private static boolean tryDefaultQueryOfSimpleType(String query, Method method, StringBuilder queryBuilder) {
        if (!defaultQueryPredicate.test(query)) {
            return false;
        }
        queryBuilder.append(method.getName());
        addVariablesReference(queryBuilder, method);
        return true;
    }

    private static final Pattern OBJECT_TYPE_QUERY = Pattern.compile("^\\s*\\{\\s*(\\{.*})\\s*}\\s*$");
    private static boolean tryQueryOfObjectType(String query, Method method, StringBuilder queryBuilder) {
        Matcher matcher = OBJECT_TYPE_QUERY.matcher(query);
        if (!matcher.matches()) {
            return false;
        }
        queryBuilder.append(method.getName());
        addVariablesReference(queryBuilder, method);
        queryBuilder.append(matcher.group(1));
        return true;
    }

    private static final Pattern CUSTOM_QUERY = Pattern.compile("^\\s*\\{\\s*(([a-zA-Z_]\\w*).*)\\s*}\\s*$");
    private static boolean tryCustomQuery(String query, Method method, StringBuilder queryBuilder, GraphQLRequestBody graphQLRequestBody) {
        Matcher matcher = CUSTOM_QUERY.matcher(query);
        if (!matcher.matches()) {
            return false;
        }
        queryBuilder.append(matcher.group(1));
        graphQLRequestBody.setFieldName(matcher.group(2));
        return true;
    }

    private static void addVariablesDeclaration(StringBuilder queryBuilder, Method method) {
        if (method.getParameterCount() == 0) {
            return;
        }
        queryBuilder.append('(');
        queryBuilder.append(Arrays.stream(method.getParameters())
                .map(p -> String.format("$%s: %s", p.getName(), declareType(p.getParameterizedType())))
                .collect(Collectors.joining(", ")));

        queryBuilder.append(')');
    }

    private static void addVariablesReference(StringBuilder queryBuilder, Method method) {
        if (method.getParameterCount() == 0) {
            return;
        }
        queryBuilder.append('(');
        queryBuilder.append(Arrays.stream(method.getParameters())
                .map(p -> String.format("%s: $%s", p.getName(), p.getName()))
                .collect(Collectors.joining(", ")));

        queryBuilder.append(')');
    }

    private static final Map<Type, String> graphQLTypeNames = Map.ofEntries(
            entry(Integer.class, "Int"),
            entry(String.class, "String"),
            entry(Boolean.class, "Boolean"),
            entry(Float.class, "Float"),
            entry(Double.class, "Float"),
            entry(Short.class, "Short"),
            entry(Long.class, "Long"),
            entry(Byte.class, "Byte"),
            entry(Character.class, "Char"),
            entry(BigDecimal.class, "BigDecimal"),
            entry(BigInteger.class, "BigInteger"),
            entry(LocalDate.class, "Date"),
            entry(OffsetDateTime.class, "DateTime"),
            entry(java.sql.Timestamp.class, "Timestamp")
    );
    private static String declareType(Type type) {
        if (type instanceof Class<?>) {
            return Optional.ofNullable(graphQLTypeNames.get(type))
                    .orElseThrow(() -> new RapidGraphQLQueryBuilderException("Simple type " + ((Class<?>)type).getName() + " is not supported for parameter type"));
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == List.class) {
                return "[" + declareType(parameterizedType.getActualTypeArguments()[0]) + "]";
            }
        }
        throw new RapidGraphQLQueryBuilderException("Can't generate query for parameter type " + type.getTypeName());
    }

    private static GraphQLRequestBody initializeRequest(GraphQL graphQL, Method method) {
        String fieldName = graphQL.fieldName();
        if (fieldName==null || fieldName.isEmpty()) {
            fieldName = method.getName();
        }
        return GraphQLRequestBody.builder().query(graphQL.query()).fieldName(fieldName).build();
    }

    private static Map<String, Object> buildVariables(Method method, Object[] args) {
        Map<String, Object> graphQLVariables = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i=0; i<parameters.length; i++) {
            graphQLVariables.put(parameters[i].getName(), args[i]);
        }
        return graphQLVariables;
    }


}
