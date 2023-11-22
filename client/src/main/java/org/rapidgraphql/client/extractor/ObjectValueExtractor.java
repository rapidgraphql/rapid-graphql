package org.rapidgraphql.client.extractor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.rapidgraphql.client.exceptions.GraphQLInvalidResponseException;

public class ObjectValueExtractor implements ValueExtractor {
    private final Class<?> fieldClass;
    private final ObjectMapper objectMapper;

    public ObjectValueExtractor(Class<?> fieldClass, ObjectMapper objectMapper) {
        this.fieldClass = fieldClass;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object extractNotNull(JSONObject data, String fieldName) {
        JSONObject fieldValue = data.getJSONObject(fieldName);
        if (fieldValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(fieldValue.toString(), fieldClass);
        } catch (JsonProcessingException e) {
            throw new GraphQLInvalidResponseException("Failed to deserialize return value", e);
        }
    }

    @Override
    public Object extractNotNull(JSONArray array, int index) {
        JSONObject fieldValue = array.getJSONObject(index);
        if (fieldValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(fieldValue.toString(), fieldClass);
        } catch (JsonProcessingException e) {
            throw new GraphQLInvalidResponseException("Failed to deserialize return value", e);
        }
    }
}
