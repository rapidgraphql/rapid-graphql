package org.rapidgraphql.app;

import graphql.kickstart.tools.GraphQLQueryResolver;
import org.rapidgraphql.annotations.GraphQLDeprecated;
import org.rapidgraphql.annotations.GraphQLDescription;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class Query implements GraphQLQueryResolver {
  private final Repository repository;

  Query(Repository repository) {
    this.repository = repository;
  }

//  public CompletableFuture<Post> getPost(Long id) {
//    return CompletableFuture.supplyAsync(() -> repository.getPostById(id));
//  }
  @GraphQLDeprecated(reason = "Deprecated for test")
  public Post getPost(Long id) {
    return repository.getPostById(id);
  }
  public DayOfWeek getDayOfWeek() {
    return LocalDate.now().getDayOfWeek();
  }

  @GraphQLDescription("returns an item")
  public Item item() {
    return Item.builder().name("WalkingPad").price(1199.).build();
  }

  public Audit getAudit() {
    return Audit.builder().status("SUCCESS").build();
  }

  public void abc() {}

}
