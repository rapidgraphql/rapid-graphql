package org.rapidgraphql.app;

import lombok.Builder;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


@Data
@Builder
public class PostInput {
    private @NonNull String text;
    private List<@NonNull CommentInput> comments;
}
