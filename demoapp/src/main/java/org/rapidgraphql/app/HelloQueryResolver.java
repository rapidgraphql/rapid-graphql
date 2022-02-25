package org.rapidgraphql.app;

import graphql.kickstart.tools.GraphQLQueryResolver;

//@Component
public class HelloQueryResolver implements GraphQLQueryResolver {
//    public String hello(String name) {
//        return "Hello " + name;
//    }
//
//    @NotNull
//    public String helloWorld() {
//        return "Hello World";
//    }
//
//    public int count() {
//        return 10;
//    }
//
//    public int countList(List<@NotNull String> words) {
//        return words.size();
//    }
//
//    public @NotNull List<@NotNull String> getWords() {
//        return List.of("word1", "word2");
//    }

    public Post post() {
        return Post.builder().id(1L).text("post").build();
    }
}
