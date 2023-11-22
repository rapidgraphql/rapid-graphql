package org.rapidgraphql.client.extractor;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import kong.unirest.core.json.JSONObjectUtils;
import org.rapidgraphql.client.exceptions.GraphQLInvalidResponseException;

public interface ValueExtractor {
    default Object extract(JSONObject data, String fieldName) {
        if (!data.has(fieldName)) {
            throw new GraphQLInvalidResponseException("Field " + fieldName + " is missing");
        }
        if (JSONObjectUtils.isNull(data, fieldName)) {
            return null;
        }
        return extractNotNull(data, fieldName);
    }
    Object extractNotNull(JSONObject data, String fieldName);
    default Object extract(JSONArray array, int index) {
        if (array.isNull(index)) {
            return null;
        }
        return extractNotNull(array, index);
    }
    Object extractNotNull(JSONArray array, int index);
}
