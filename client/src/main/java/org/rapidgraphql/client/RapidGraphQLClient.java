package org.rapidgraphql.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import kong.unirest.core.Config;
import kong.unirest.modules.jackson.JacksonObjectMapper;

import java.lang.reflect.Proxy;

public class RapidGraphQLClient {
    public static class Builder {
        private static final ObjectMapper DEFAULT_OBJECT_MAPPER = JsonMapper.builder() // or different mapper for other format
                .addModule(new ParameterNamesModule())
                .addModule(new Jdk8Module())
                .addModule(new JavaTimeModule())
                .build();
        private ObjectMapper objectMapper = DEFAULT_OBJECT_MAPPER;
        private final Config requestConfig = new Config().setObjectMapper(new JacksonObjectMapper(DEFAULT_OBJECT_MAPPER));

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            requestConfig.setObjectMapper(new JacksonObjectMapper(objectMapper));
            return this;
        }
        public Builder connectTimeoutMs(int millies) {
            requestConfig.connectTimeout(millies);
            return this;
        }
        public Builder requestTimeoutMs(int millies) {
            requestConfig.requestTimeout(millies);
            return this;
        }
        public <T> T target(Class<T> apiClass, String url) {
            GraphQLHttpClient graphQLHttpClient = new GraphQLHttpClient(url, requestConfig);
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
