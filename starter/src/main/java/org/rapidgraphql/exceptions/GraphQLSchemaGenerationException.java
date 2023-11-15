package org.rapidgraphql.exceptions;

public class GraphQLSchemaGenerationException extends RuntimeException {
    public GraphQLSchemaGenerationException(String message) {
        super(message);
    }
    public GraphQLSchemaGenerationException(String message, Exception e) {
        super(message, e);
    }
}
