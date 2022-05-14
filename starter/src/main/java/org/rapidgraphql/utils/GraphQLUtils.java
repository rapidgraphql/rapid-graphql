package org.rapidgraphql.utils;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.CoercingParseLiteralException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class GraphQLUtils {
    @SuppressWarnings("rawtypes")
    private static final Map<Class<? extends Value>, Function<Object, Object>> valueGetter = Map.of(
            NullValue.class, v -> null,
            StringValue.class, v -> ((StringValue) v).getValue(),
            EnumValue.class, v -> ((EnumValue) v).getName(),
            BooleanValue.class, v -> ((BooleanValue) v).isValue(),
            IntValue.class, v -> ((IntValue) v).getValue(),
            FloatValue.class, v -> ((FloatValue) v).getValue(),
            ArrayValue.class, v -> ((ArrayValue) v).getValues().stream().map(GraphQLUtils::parseLiteral).collect(toList()),
            ObjectValue.class, v -> ((ObjectValue) v).getObjectFields().stream().collect(toMap(ObjectField::getName, f -> parseLiteral(f.getValue())))
    );


    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object parseLiteral(Object input) throws CoercingParseLiteralException {
        Class<? extends Value> clazz = input == null ? NullValue.class : (Class<? extends Value>)input.getClass();
        return Optional.ofNullable(valueGetter.get(clazz)).orElseThrow(() -> new IllegalArgumentException(String.valueOf(input))).apply(input);
    }
}
