package org.rapidgraphql.client;

import kong.unirest.core.*;
import lombok.Builder;
import org.rapidgraphql.client.exceptions.GraphQLHttpErrorException;
import org.rapidgraphql.client.extractor.ResultExtractor;

public class GraphQLHttpClient {
    private final String url;
    UnirestInstance unirestInstance;
    @Builder
    public GraphQLHttpClient(String url, Config requestConfig) {
        this.url = url;
        this.unirestInstance = new UnirestInstance(requestConfig);
    }
    public Object exchange(GraphQLRequestBody graphQLRequestBody, ResultExtractor extractor) {
        HttpRequestWithBody requestWithBody = unirestInstance.post(url)
                .accept("application/json")
                .contentType("application/json");
        HttpResponse<JsonNode> jsonNodeHttpResponse = requestWithBody
                .body(graphQLRequestBody)
                .headers(graphQLRequestBody.getHeaders())
                .asJson();
        if (!jsonNodeHttpResponse.isSuccess()) {
            throw new GraphQLHttpErrorException(jsonNodeHttpResponse);
        }
        return jsonNodeHttpResponse.mapBody(extractor::extract);
    }
}
