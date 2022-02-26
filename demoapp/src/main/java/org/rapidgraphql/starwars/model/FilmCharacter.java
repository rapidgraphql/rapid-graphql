package org.rapidgraphql.starwars.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilmCharacter {
    private Long id;
    private String name;
}
