package org.rapidgraphql.schemabuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
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
import org.rapidgraphql.utils.TypeUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.rapidgraphql.utils.TypeUtils.castToClass;
import static org.rapidgraphql.utils.TypeUtils.extractClassFieldAnnotatedType;
import static org.rapidgraphql.utils.TypeUtils.extractListElementType;
import static org.rapidgraphql.utils.TypeUtils.tryGetClass;

public class DefaultValueAnnotationProcessorImpl implements DefaultValueAnnotationProcessor {

    private static final Map<Type, Function<String, Value<?>>> strValueFactory = Stream.of(
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

    private static final Map<Type, Function<Object, Value<?>>> objectValueFactory = Map.of(
            String.class, value -> new StringValue((String)value),
            Integer.class, value -> new IntValue(BigInteger.valueOf((Integer)value)),
            Long.class, value -> new IntValue(BigInteger.valueOf((Long)value)),
            Float.class, value -> new FloatValue(BigDecimal.valueOf((Float)value)),
            Double.class, value -> new FloatValue(BigDecimal.valueOf((Double)value)),
            Boolean.class, value -> new BooleanValue((Boolean)value)
    );
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final NullValue NULL_VALUE = NullValue.newNullValue().build();

    @Override
    public void applyAnnotations(Parameter parameter, InputValueDefinition.Builder builder) {
        Optional<Value<?>> defaultValue = Optional.empty();
        if (parameter.isAnnotationPresent(GraphQLDefaultNull.class)) {
            defaultValue = Optional.of(NULL_VALUE);
        } else {
            GraphQLDefault annotation = parameter.getAnnotation(GraphQLDefault.class);
            if (annotation != null) {
                defaultValue = Optional.of(createDefaultValue(parameter.getAnnotatedType(), annotation.value()));
            }
        }
        defaultValue.ifPresent(builder::defaultValue);
    }

    private Value<?> createDefaultValue(AnnotatedType annotatedType, String strValue) {
        switch (TypeUtils.detectValueType(annotatedType)) {
            case SIMPLE_VALUE: {
                return Optional.ofNullable(strValueFactory.get(annotatedType.getType()))
                        .map(f -> f.apply(strValue))
                        .orElseThrow(() -> new IllegalArgumentException("Unsupported simple type " + annotatedType.getType().getTypeName()));
            }
            case LIST_VALUE: {
                tryReadValue(annotatedType, strValue);
                return createListValue(extractListElementType(annotatedType).get(), strValue);
            }
            case OBJECT_VALUE: {
                tryReadValue(annotatedType, strValue);
                createObjectValue(tryGetClass(annotatedType).get(), strValue);
            }
            case ENUM_VALUE: {
                validateEnumValue(annotatedType, strValue);
                return createEnumValue(strValue);
            }
            default:
                throw new IllegalArgumentException("Unrecognized type " + annotatedType.getType().getTypeName());
        }
    }

    private void tryReadValue(AnnotatedType annotatedType, String strValue) {
        JavaType javaType = TypeUtils.constructJavaType(annotatedType);
        try {
            objectMapper.readValue(strValue, javaType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse value for type " +  annotatedType.getType().getTypeName(), e);
        }
    }

    private void validateEnumValue(AnnotatedType annotatedType, String strValue) {
        Class<?> clazz = castToClass(annotatedType.getType());
        if (!clazz.isEnum()) {
            throw new IllegalArgumentException(format("class %s is expected to be enum but is not", clazz.getName()));
        }
        boolean matches = Arrays.stream(clazz.getEnumConstants())
                .map(Object::toString)
                .anyMatch(constantName -> constantName.equals(strValue));
        if (!matches) {
            throw new IllegalArgumentException(format("%s doesn't belong to the enum %s", strValue, clazz.getName()));
        }
    }

    private Value<?> createComplexValue(AnnotatedType annotatedType, String strValue) {
        Optional<AnnotatedType> listElementType = extractListElementType(annotatedType);
        if (listElementType.isPresent()) {
            return createListValue(listElementType.get(), strValue);
        }
        Class<?> clazz = castToClass(annotatedType.getType());
        if (clazz.isEnum()) {
            return createEnumValue(strValue);
        } else {
            return createObjectValue(clazz, strValue);
        }
    }

    private Value<?> createEnumValue(String strValue) {
        return new EnumValue(strValue);
    }

    private Value<?> createListValue(AnnotatedType elementType, String strValue) {
        List<Object> list;
        try {
            list = objectMapper.readValue(strValue, List.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Can't parse default value parameter as list", e);
        }
        return createListValue(list, elementType);
    }

    private Value<?> createObjectValue(Class<?> type, String strValue) {
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
        return createMapValue(map, type);
    }

    private ObjectField createObjectField(String name, Object objValue, AnnotatedType annotatedType) {
        Value<?> value = getValueMapper(annotatedType).apply(objValue);
        return ObjectField.newObjectField()
                .name(name)
                .value(value)
                .build();
    }

    private Value<?> createMapValue(Map<String, Object> map, Class<?> clazz) {
        List<ObjectField> fields = map.entrySet().stream()
                .map(entry -> createObjectField(entry.getKey(), entry.getValue(),
                        extractClassFieldAnnotatedType(clazz, entry.getKey()).orElseThrow(() -> new IllegalArgumentException(format("Class %s is missing field %s", clazz.getName(), entry.getKey())))))
                .collect(Collectors.toList());
        return new ObjectValue(fields);
    }

    private Value<?> createListValue(List<Object> list, AnnotatedType elementType) {
        List<Value<?>> valueList = list.stream()
                    .map(getValueMapper(elementType))
                    .collect(Collectors.toList());
        return new ArrayValue((List)valueList);
    }

    private Function<Object, Value<?>> getValueMapper(AnnotatedType elementType) {
        switch(TypeUtils.detectValueType(elementType)) {
            case SIMPLE_VALUE: {
                return object -> {
                    if (object == null) {
                        return NULL_VALUE;
                    } else {
                        return Optional.ofNullable(objectValueFactory.get(object.getClass()))
                                .map(f -> f.apply(object))
                                .orElseThrow(() -> new IllegalArgumentException("Can't detect simple value"));
                    }
                };
            }
            case OBJECT_VALUE: {
                return object -> {
                    if (object == null) {
                        return NULL_VALUE;
                    } else if (!(object instanceof Map)) {
                        throw new IllegalArgumentException("Expected map value");
                    } else {
                        return createMapValue((Map<String, Object>)object, castToClass(elementType.getType()));
                    }
                };
            }
            case LIST_VALUE: {
                return object -> {
                    if (object == null) {
                        return NULL_VALUE;
                    } else if (!(object instanceof List)) {
                        throw new IllegalArgumentException("Expected list value");
                    } else {
                        return createListValue((List<Object>)object, extractListElementType(elementType).get());
                    }
                };
            }
            case ENUM_VALUE: {
                return object -> {
                    if (object == null) {
                        return NULL_VALUE;
                    } else if (!(object instanceof String)) {
                        throw new IllegalArgumentException("Expected string value to be mapped to enum");
                    } else {
                        return createEnumValue((String) object);
                    }
                };
            }
            default:
                throw new IllegalArgumentException("Unsupported value type");
        }
    }

    private static Map.Entry<Type, Function<String, Value<?>>> strFunctionEntry(Class<?> clazz, Function<String, Value<?>> function) {
        return Map.entry(clazz, function);
    }
}
