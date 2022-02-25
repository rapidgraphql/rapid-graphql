package org.rapidgraphql.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
class Comment {

  private Long id;
  private String description;
}
