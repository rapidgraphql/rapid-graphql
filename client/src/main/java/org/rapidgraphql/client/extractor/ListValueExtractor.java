package org.rapidgraphql.client.extractor;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ListValueExtractor implements ValueExtractor {
    private final ValueExtractor valueExtractor;

    public ListValueExtractor(ValueExtractor valueExtractor) {
        this.valueExtractor = valueExtractor;
    }

    @Override
    public Object extractNotNull(JSONObject data, String fieldName) {
        JSONArray array = data.getJSONArray(fieldName);
        if (array == null) {
            return null;
        }
        return IntStream.range(0, array.length())
                .mapToObj(index -> valueExtractor.extract(array, index))
                .collect(Collectors.toList());
    }

    @Override
    public Object extractNotNull(JSONArray array, int index) {
        JSONArray subArray = array.getJSONArray(index);
        if (subArray == null) {
            return null;
        }
        return IntStream.range(0, subArray.length())
                .mapToObj(subIndex -> valueExtractor.extract(subArray, subIndex))
                .collect(Collectors.toList());
    }
}
