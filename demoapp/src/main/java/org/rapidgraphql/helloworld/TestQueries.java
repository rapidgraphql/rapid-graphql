package org.rapidgraphql.helloworld;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.kickstart.spring.error.ErrorContext;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static graphql.ErrorClassification.errorClassification;

@Service
public class TestQueries implements GraphQLQueryResolver {
    public Integer intValue(Integer val) {
        return val;
    }
    public Long longValue(Long val) {
        return val;
    }
    public String longValue(String val) {
        return val;
    }
    public List<String> stringList(List<String> val) {
        return val;
    }
    public String throwException(String message) {
        throw new IllegalStateException(message);
    }
    @ExceptionHandler(value = IllegalStateException.class)
    public GraphQLError toCustomError(IllegalStateException e, ErrorContext errorContext) {
        Map<String, Object> extensions =
                Optional.ofNullable(errorContext.getExtensions()).orElseGet(HashMap::new);
        extensions.put("my-custom-code", "some-custom-error");
        return GraphqlErrorBuilder.newError()
                .message(e.getMessage())
                .extensions(extensions)
                .errorType(errorClassification(e.getClass().getSimpleName()))
                .locations(errorContext.getLocations())
                .path(errorContext.getPath())
                .build();
    }
}
