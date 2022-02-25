package org.rapidgraphql.app;

import graphql.kickstart.tools.GraphQLQueryResolver;
import org.rapidgraphql.annotations.GraphQLDefault;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldQuery implements GraphQLQueryResolver {
    public String hello(@GraphQLDefault("World") String name) {
        return "Hello " + name;
    }

}
