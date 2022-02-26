package org.rapidgraphql.helloworld;

import graphql.kickstart.tools.GraphQLMutationResolver;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class ChatMutation implements GraphQLMutationResolver {
    public @NotNull Chat message(@NotNull String message) {
        String reverse = new StringBuilder(message).reverse().toString();
        return Chat.builder().youSaid(message).iSay(reverse).build();
    }
}
