package org.rapidgraphql.client;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import lombok.Builder;
import org.rapidgraphql.client.exceptions.GraphQLHttpErrorException;
import org.rapidgraphql.client.extractor.ResultExtractor;

@Builder
public class GraphQLHttpClient {
    private final String url;
    public Object exchange(GraphQLRequestBody graphQLRequestBody, ResultExtractor extractor) {
        HttpResponse<JsonNode> jsonNodeHttpResponse = Unirest.post(url)
                .header("accept", "application/json")
                .body(graphQLRequestBody)
                .asJson();
        if (!jsonNodeHttpResponse.isSuccess()) {
            throw new GraphQLHttpErrorException(jsonNodeHttpResponse);
        }
        return jsonNodeHttpResponse.mapBody(extractor::extract);
    }
}
