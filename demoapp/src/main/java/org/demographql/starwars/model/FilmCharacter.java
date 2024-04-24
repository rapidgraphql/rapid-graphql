package org.demographql.starwars.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.rapidgraphql.annotations.GraphQLInterface;
import org.rapidgraphql.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@GraphQLInterface
public class FilmCharacter {

    private @NotNull Long id;

    private @NotNull String name;

    private @NotNull List<@NotNull String> names = new ArrayList<>();
}
