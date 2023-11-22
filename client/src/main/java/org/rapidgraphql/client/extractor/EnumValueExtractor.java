package org.rapidgraphql.client.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnumValueExtractor implements ValueExtractor {
    private Map<String, Object> enumConstants = null;

    public EnumValueExtractor(Class<?> fieldClass) {
        if (fieldClass.isEnum()) {
            enumConstants = Arrays.stream(fieldClass.getEnumConstants())
                    .collect(Collectors.toMap(Object::toString, constant -> constant));
        }
    }

    @Override
    public Object extractNotNull(JSONObject data, String fieldName) {
        return Optional.ofNullable(data.getString(fieldName))
                .map(enumConstants::get)
                .orElse(null);
    }

    @Override
    public Object extractNotNull(JSONArray array, int index) {
        return Optional.ofNullable(array.getString(index))
                .map(enumConstants::get)
                .orElse(null);
    }
}
