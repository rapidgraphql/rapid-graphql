package org.rapidgraphql.app;

import lombok.Builder;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Following schema will generated for Post type
 * <pre>
 * {@code
 * type Post {
 *   id: Long!
 *   test: String
 * }
 * }</pre>
 */
@Data
@Builder
public class Post {

  private @NonNull Long id;
  private String text;
}
