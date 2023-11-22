package org.rapidgraphql.client.exceptions;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Value
@Builder
@Jacksonized
public class GraphQLError {
    @Value
    @Builder
    @Jacksonized
    public static class SourceLocation {
        int line;
        int column;
        String sourceName;
    };
    String message;
    List<SourceLocation> locations;
    String errorType;
    List<Object> path;
    Map<String, Object> extensions;
}
