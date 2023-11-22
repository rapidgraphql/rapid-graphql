package org.rapidgraphql.client.extractor;

import kong.unirest.core.JsonNode;

public interface ResultExtractor {
    Object extract(JsonNode jsonNode);
}
