package org.rapidgraphql.client.extractor;

import kong.unirest.core.json.JSONObject;

public class ObjectExtractor extends AbstractResultExtractor {
    private final String fieldName;
    private final ValueExtractor valueExtractor;

    public ObjectExtractor(String fieldName, ValueExtractor valueExtractor) {
        this.fieldName = fieldName;
        this.valueExtractor = valueExtractor;
    }

    @Override
    protected Object applyOnData(JSONObject data) {
        return valueExtractor.extract(data, fieldName);
    }
}
