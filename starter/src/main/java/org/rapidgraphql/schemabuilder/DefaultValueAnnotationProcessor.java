package org.rapidgraphql.schemabuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.InputValueDefinition;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import org.rapidgraphql.annotations.GraphQLDefault;
import org.rapidgraphql.annotations.GraphQLDefaultNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rapidgraphql.schemabuilder.TypeUtils.isListType;

public class DefaultValueAnnotationProcessor {
    private static final Map<Class<?>, Function<String, Value<?>>> strValueFactory = Stream.of(
            strFunctionEntry(String.class, StringValue::new),
            strFunctionEntry(Integer.class, value -> new IntValue(new BigInteger(value))),
            strFunctionEntry(Integer.TYPE, value -> new IntValue(new BigInteger(value))),
            strFunctionEntry(Long.class, value -> new IntValue(new BigInteger(value))),
            strFunctionEntry(Long.TYPE, value -> new IntValue(new BigInteger(value))),
            strFunctionEntry(Float.class, value -> new FloatValue(new BigDecimal(value))),
            strFunctionEntry(Float.TYPE, value -> new FloatValue(new BigDecimal(value))),
            strFunctionEntry(Double.class, value -> new FloatValue(new BigDecimal(value))),
            strFunctionEntry(Double.TYPE, value -> new FloatValue(new BigDecimal(value))),
            strFunctionEntry(Boolean.class, value -> new BooleanValue(Boolean.parseBoolean(value))),
            strFunctionEntry(Boolean.TYPE, value -> new BooleanValue(Boolean.parseBoolean(value)))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private static final Map<Class<?>, Function<Object, Value<?>>> objectValueFactory = Map.of(
            String.class, value -> new StringValue((String)value),
            Integer.class, value -> new IntValue(BigInteger.valueOf((Integer)value)),
            Long.class, value -> new IntValue(BigInteger.valueOf((Long)value)),
            Float.class, value -> new FloatValue(BigDecimal.valueOf((Float)value)),
            Double.class, value -> new FloatValue(BigDecimal.valueOf((Double)value)),
            Boolean.class, value -> new BooleanValue((Boolean)value)
    );
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void applyAnnotations(Parameter parameter, InputValueDefinition.Builder builder) {
        Optional<Value<?>> defaultValue = Optional.empty();
        if (parameter.isAnnotationPresent(GraphQLDefaultNull.class)) {
            defaultValue = Optional.of(NullValue.newNullValue().build());
        } else {
            GraphQLDefault annotation = parameter.getAnnotation(GraphQLDefault.class);
            if (annotation != null) {
                defaultValue = createDefaultValue(parameter.getType(), annotation.value());
            }
        }
        defaultValue.ifPresent(builder::defaultValue);
    }

    private static Optional<Value<?>> createDefaultValue(Class<?> type, String strValue) {
        Optional<Value<?>> value = Optional.ofNullable(strValueFactory.get(type)).map(f -> f.apply(strValue));
        if (value.isEmpty()) {
            value = Optional.of(createComplexValue(type, strValue));
        }
        return value;
    }
    private static Value<?> createComplexValue(Class<?> type, String strValue) {
        if (isListType(type)) {
            return createListValue(type, strValue);
        } else {
            return createObjectValue(type, strValue);
        }
    }

    private static Value<?> createListValue(Class<?> type, String strValue) {
        List<Object> list;
        try {
            list = objectMapper.readValue(strValue, List.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't parse default value parameter as list", e);
        }
        return createListValue(list);
    }

    private static Value<?> createObjectValue(Class<?> type, String strValue) {
        Map<String, Object> map;
        try {
            objectMapper.readValue(strValue, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't parse default value parameter as expected type " + type.getClass().getName(), e);
        }
        try {
            map = objectMapper.readValue(strValue, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't parse default value parameter as map", e);
        }
        return createMapValue(map);
    }

    private static ObjectField createObjectField(String name, Object objValue) {
        Value<?> value = null;
        if (objValue == null) {
            value = NullValue.newNullValue().build();
        }
        if (value == null) {
            value = Optional.ofNullable(objectValueFactory.get(objValue.getClass())).map(f -> f.apply(objValue)).orElse(null);
            if (value == null) {
                value = createComplexValue(objValue);
            }
        }
        return ObjectField.newObjectField().name(name).value(value).build();
    }

    @Nullable
    private static Value<?> createComplexValue(Object objValue) {
        if (objValue instanceof List) {
            return createListValue((List<Object>) objValue);
        } else if (objValue instanceof Map) {
            return createMapValue((Map<String, Object>) objValue);
        } else {
            throw new IllegalArgumentException("Can't convert object of type "+objValue.getClass().getName()+" to graphql Value");
        }
    }

    private static Value<?> createMapValue(Map<String, Object> map) {
        List<ObjectField> fields = map.entrySet().stream()
                .map(entry -> createObjectField(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new ObjectValue(fields);
    }

    private static Value<?> createListValue(List<Object> list) {
        List<Value> valueList = list.stream()
                .map(DefaultValueAnnotationProcessor::createComplexValue)
                .collect(Collectors.toList());
        return new ArrayValue(valueList);
    }

    private static Map.Entry<Class<?>, Function<String, Value<?>>> strFunctionEntry(Class<?> clazz, Function<String, Value<?>> function) {
        return Map.entry(clazz, function);
    }
}
