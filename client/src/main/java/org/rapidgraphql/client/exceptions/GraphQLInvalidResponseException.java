package org.rapidgraphql.client.exceptions;

public class GraphQLInvalidResponseException extends RapidGraphQLClientException{
    public GraphQLInvalidResponseException(String message) {
        super(message);
    }
    public GraphQLInvalidResponseException(String message, Throwable t) {
        super(message, t);
    }
}
