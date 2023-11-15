package org.rapidgraphql.starwars.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.rapidgraphql.annotations.GraphQLImplementation;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@GraphQLImplementation
public class Droid extends FilmCharacter{
    String primaryFunction;
}
