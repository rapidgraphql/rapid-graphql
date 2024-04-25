package org.demographql.helloworld;

import graphql.kickstart.tools.GraphQLQueryResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.rapidgraphql.annotations.GraphQLDefault;
import org.rapidgraphql.annotations.GraphQLIgnore;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class HelloWorldQuery implements GraphQLQueryResolver {
    public String helloWorld() {
        return "Hello World!!";
    }

    public @NonNull String hello(@GraphQLDefault("World") @NonNull String name) {
        return "Hello " + name;
    }

    public @NotNull DayOfWeek getDayOfWeek() {
        return LocalDate.now().getDayOfWeek();
    }

    @GraphQLIgnore
    public String methodNotExposedToGraphQL() {
        return "Internal Data";
    }

    private String privateMethodIsNotExposedToGraphQL() {
        return "Internal Data";
    }
}
