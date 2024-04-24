package org.demographql.helloworld;

import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.rapidgraphql.annotations.DataLoaderMethod;
import org.rapidgraphql.directives.GraphQLDataLoader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ChatQuery implements GraphQLQueryResolver, GraphQLDataLoader {
    public CompletableFuture<List<Chat>> chat(int numMessages, DataFetchingEnvironment environment) {
        DataLoader<String, Chat> dataLoader = environment.getDataLoader("chat");
        return dataLoader.loadMany(IntStream.range(1, numMessages+1).mapToObj(String::valueOf).collect(Collectors.toList()));
    }

    @DataLoaderMethod("chat")
    public List<Chat> loadChatMessages(List<String> messages) {
        return messages.stream()
                .map(msg -> Chat.builder().youSaid(msg).iSay("Got it " + msg).build())
                .collect(Collectors.toList());
    }
}
