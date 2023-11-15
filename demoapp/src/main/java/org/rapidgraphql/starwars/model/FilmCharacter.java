package org.rapidgraphql.starwars.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.rapidgraphql.annotations.GraphQLInterface;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@GraphQLInterface
public class FilmCharacter {
    private Long id;
    private String name;
}
