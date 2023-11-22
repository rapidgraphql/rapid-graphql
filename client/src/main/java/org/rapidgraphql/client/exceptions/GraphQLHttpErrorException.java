package org.rapidgraphql.client.exceptions;

import kong.unirest.core.HttpResponse;

public class GraphQLHttpErrorException extends RapidGraphQLClientException {
    public GraphQLHttpErrorException(HttpResponse<?> response) {
        super(String.format("GraphQL server returned status %d (%s)", response.getStatus(), response.getStatusText()));
    }

}
