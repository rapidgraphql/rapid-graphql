package org.demographql.starwars.repository;

import org.demographql.starwars.model.Episode;
import org.demographql.starwars.model.FilmCharacter;

import java.util.List;

public interface FilmCharacterRepository {
    Long getHeroId(Episode episode);
    List<Long> getFriendsById(Long name);
    List<Episode> getAppearsInById(Long id);
    FilmCharacter getCharacterById(Long id);
    List<FilmCharacter> getAllCharacters();
}
