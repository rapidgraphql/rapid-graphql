package org.rapidgraphql.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rapidgraphql.client.extractor.ResultExtractor;
import org.rapidgraphql.client.extractor.ResultExtractorFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class GraphQLInvocationHandler implements InvocationHandler {
    private final GraphQLHttpClient graphQLHttpClient;
    private final ObjectMapper objectMapper;

    public GraphQLInvocationHandler(GraphQLHttpClient graphQLHttpClient, ObjectMapper objectMapper) {
        this.graphQLHttpClient = graphQLHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            // Call the default method using reflection
            return MethodHandles.lookup()
                    .in(method.getDeclaringClass())
                    .unreflectSpecial(method, method.getDeclaringClass())
                    .bindTo(proxy)
                    .invokeWithArguments(args);
        } else {
            // Handle non-default methods
            GraphQLRequestBody request = GraphQLRequestBuilder.build(method, args);
            ResultExtractor extractor = ResultExtractorFactory.createExtractor(request.getFieldName(), method.getGenericReturnType(), objectMapper);
            return graphQLHttpClient.exchange(request, extractor);
        }
    }
}
