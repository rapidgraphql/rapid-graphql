package org.rapidgraphql.app;

import graphql.kickstart.tools.GraphQLMutationResolver;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class Mutation implements GraphQLMutationResolver {

  private static final Logger LOGGER = getLogger(Mutation.class);
  private final Repository repository;

  public Mutation(Repository repository) {
    this.repository = repository;
    Method[] methods = PostInput.class.getDeclaredMethods();
    LOGGER.info("{} methods in PostInput", methods.length);

  }

  public Post createPost(String text) {
    return addPost(PostInput.builder().text(text).build());
  }

  public Post addPost(PostInput post) {
    Long postId = repository.createPost(post);
    post.setText(null);
    return repository.getPostById(postId);
  }
}
