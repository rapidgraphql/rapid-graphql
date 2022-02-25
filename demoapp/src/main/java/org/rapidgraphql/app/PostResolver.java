package org.rapidgraphql.app;

import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

@Component
public class PostResolver implements GraphQLResolver<Post> {
  private final Repository repository;

  PostResolver(Repository repository) {
    this.repository = repository;
  }

  /** This will add following field to the Post type
  type Post {
      ...
      comments: [Comment!]!
   }
   */
  public @NotNull List<@NotNull Comment> getComments(Post post) {
    return repository.getCommentsByPostId(post.getId());
  }
}
