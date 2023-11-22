package org.rapidgraphql.client.exceptions;

import kong.unirest.core.HttpResponse;

public class RapidGraphQLClientException extends RuntimeException {
    public RapidGraphQLClientException(String message) {
        super(message);
    }
    public RapidGraphQLClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
