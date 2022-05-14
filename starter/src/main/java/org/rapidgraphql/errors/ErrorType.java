package org.rapidgraphql.errors;

import graphql.ErrorClassification;

public enum ErrorType implements ErrorClassification {
    UNKNOWN,
    INTERNAL,
    NOT_FOUND,
    UNAUTHENTICATED,
    PERMISSION_DENIED,
    BAD_REQUEST,
    UNAVAILABLE,
    FAILED_PRECONDITION;

    private ErrorType() {
    }
}
