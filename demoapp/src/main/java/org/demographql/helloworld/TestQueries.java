package org.demographql.helloworld;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.kickstart.spring.error.ErrorContext;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static graphql.ErrorClassification.errorClassification;

@Service
public class TestQueries implements GraphQLQueryResolver {
    public Integer intValue(Integer val) {
        return val;
    }
    public Long longValue(Long val) {
        return val;
    }
    public Float floatValue(Float val) { return val; }
    public Double doubleValue(Double val) { return val; }
    public BigDecimal bigDecimalValue(BigDecimal val) { return val; }
    public String stringValue(String val) {
        return val;
    }
    public List<String> stringList(List<String> val) {
        return val;
    }
    public String throwException(String message) {
        throw new IllegalStateException(message);
    }

    public List<MyValue> myValues(int range) {
        return IntStream.range(0,range)
                .mapToObj(i -> MyValue.builder().a(i).b(i*100).build())
                .collect(Collectors.toList());
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
