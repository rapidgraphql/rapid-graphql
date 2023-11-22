package org.rapidgraphql.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Proxy;

public class RapidGraphQLClient {
    public static class Builder {
        private ObjectMapper objectMapper;

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }
        public <T> T target(Class<T> apiClass, String url) {
            GraphQLHttpClient graphQLHttpClient = new GraphQLHttpClient(url);
            ObjectMapper objectMapper = this.objectMapper==null ? new ObjectMapper() : this.objectMapper;
            return (T) Proxy.newProxyInstance(
                    apiClass.getClassLoader(),
                    new Class[]{apiClass},
                    new GraphQLInvocationHandler(graphQLHttpClient, objectMapper)
            );
       }
    }

    public static Builder builder() {
        return new Builder();
    }
}
