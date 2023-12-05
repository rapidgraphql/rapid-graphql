package org.rapidgraphql.client;

import kong.unirest.core.*;
import lombok.Builder;
import org.rapidgraphql.client.exceptions.GraphQLHttpErrorException;
import org.rapidgraphql.client.extractor.ResultExtractor;

@Builder
public class GraphQLHttpClient {
    private final String url;
    private final Config requestConfig;
    public Object exchange(GraphQLRequestBody graphQLRequestBody, ResultExtractor extractor) {
        HttpRequestWithBody requestWithBody = Unirest.post(url)
                .accept("application/json");
        if (requestConfig != null) {
            requestWithBody = requestWithBody.connectTimeout(requestConfig.getConnectionTimeout());
        }
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
