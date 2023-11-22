package org.rapidgraphql.client.extractor;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.function.BiFunction;

public class SimpleValueExtractor implements ValueExtractor {

    @Value
    @AllArgsConstructor(staticName = "of")
    static class Extractors {
        BiFunction<JSONObject, String, Object> byName;
        BiFunction<JSONArray, Integer, Object> byIndex;
    }

    private static Map.Entry<Class<?>, Extractors> entry(Class<?> fieldClass,
                                                         BiFunction<JSONObject, String, Object> byName, 
                                                         BiFunction<JSONArray, Integer, Object> byIndex) {
        return Map.entry(fieldClass, Extractors.of(byName, byIndex));
    }
    private static Map<Class<?>, Extractors> simpleTypes = Map.ofEntries(
            entry(Boolean.class, JSONObject::getBoolean, JSONArray::getBoolean),
            entry(Character.class, SimpleValueExtractor::getCharFromObject, SimpleValueExtractor::getCharFromArray),
            entry(Byte.class, SimpleValueExtractor::getByteFromObject, SimpleValueExtractor::getByteFromArray),
            entry(Short.class, SimpleValueExtractor::getShortFromObject, SimpleValueExtractor::getShortFromArray),
            entry(Integer.class, JSONObject::getInt, JSONArray::getInt),
            entry(Long.class, JSONObject::getLong, JSONArray::getLong),
            entry(String.class, JSONObject::getString, JSONArray::getString),
            entry(Float.class, JSONObject::getFloat, JSONArray::getFloat),
            entry(Double.class, JSONObject::getDouble, JSONArray::getDouble),
            entry(BigDecimal.class, JSONObject::getBigDecimal, JSONArray::getBigDecimal),
            entry(BigInteger.class, JSONObject::getBigInteger, JSONArray::getBigInteger)
    );

    private static Short getShortFromObject(JSONObject object, String name) {
        return (short) object.getInt(name);
    }
    private static Short getShortFromArray(JSONArray array, Integer index) {
        return (short) array.getInt(index);
    }

    private static Character getCharFromObject(JSONObject object, String name) {
        String string = object.getString(name);
        if (string == null || string.isEmpty()) {
            return null;
        }
        return string.charAt(0);
    }

    private static Character getCharFromArray(JSONArray array, Integer index) {
        String string = array.getString(index);
        if (string == null || string.isEmpty()) {
            return null;
        }
        return string.charAt(0);
    }

    private static Byte getByteFromObject(JSONObject object, String name) {
        String string = object.getString(name);
        if (string == null || string.isEmpty()) {
            return null;
        }
        return string.getBytes()[0];
    }

    private static Byte getByteFromArray(JSONArray array, Integer index) {
        String string = array.getString(index);
        if (string == null || string.isEmpty()) {
            return null;
        }
        return string.getBytes()[0];
    }


    private final Class<?> fieldClass;

    public SimpleValueExtractor(Class<?> fieldClass) {
        this.fieldClass = fieldClass;
    }

    public static boolean isSimpleType(Class<?> fieldClass) {
        return simpleTypes.containsKey(fieldClass);
    }

    @Override
    public Object extractNotNull(JSONObject data, String fieldName) {
        return simpleTypes.get(fieldClass).byName.apply(data, fieldName);
    }

    @Override
    public Object extractNotNull(JSONArray array, int index) {
        return simpleTypes.get(fieldClass).byIndex.apply(array, index);
    }
}
