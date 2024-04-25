package org.demographql.starwars;

import graphql.kickstart.tools.GraphQLQueryResolver;
import org.demographql.starwars.model.Episode;
import org.demographql.starwars.model.FilmCharacter;
import org.demographql.starwars.repository.FilmCharacterRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StarWarsQuery implements GraphQLQueryResolver {
    private final FilmCharacterRepository filmCharacterRepository;

    public StarWarsQuery(FilmCharacterRepository filmCharacterRepository) {
        this.filmCharacterRepository = filmCharacterRepository;
    }

    public FilmCharacter hero(Episode episode) {
        return filmCharacterRepository.getCharacterById(filmCharacterRepository.getHeroId(episode));
    }

    public FilmCharacter getCharacter(Long id) {
        return  filmCharacterRepository.getCharacterById(id);
    }

    public List<FilmCharacter> getCharacters() {
        return  filmCharacterRepository.getAllCharacters();
    }


}
