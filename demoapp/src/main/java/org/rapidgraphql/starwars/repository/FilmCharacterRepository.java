package org.rapidgraphql.starwars.repository;

import org.rapidgraphql.starwars.model.Episode;
import org.rapidgraphql.starwars.model.FilmCharacter;

import java.util.List;

public interface FilmCharacterRepository {
    Long getHeroId(Episode episode);
    List<Long> getFriendsById(Long name);
    List<Episode> getAppearsInById(Long id);
    FilmCharacter getCharacterById(Long id);
}
