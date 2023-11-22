package org.rapidgraphql.client.exceptions;

import kong.unirest.core.json.JSONObject;
import lombok.Getter;

import java.util.List;

public class GraphQLErrorException extends RapidGraphQLClientException {
    @Getter
    private final List<GraphQLError> errors;
    public GraphQLErrorException(List<GraphQLError> errors) {
        super(extractMessage(errors));
        this.errors = errors;
    }

    private static String extractMessage(List<GraphQLError> errors) {
        if (errors==null || errors.isEmpty()) {
            return "No 'errors' object provided in graphql response";
        }
        return errors.get(0).getMessage();
    }

}
