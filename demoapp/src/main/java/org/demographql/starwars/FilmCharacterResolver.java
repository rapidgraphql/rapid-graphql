package org.demographql.starwars;

import graphql.kickstart.tools.GraphQLResolver;
import org.demographql.starwars.model.Episode;
import org.demographql.starwars.model.FilmCharacter;
import org.demographql.starwars.repository.FilmCharacterRepository;
import org.rapidgraphql.annotations.GraphQLDeprecated;
import org.rapidgraphql.annotations.GraphQLDescription;
import org.rapidgraphql.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FilmCharacterResolver implements GraphQLResolver<FilmCharacter> {
    private final FilmCharacterRepository filmCharacterRepository;

    public FilmCharacterResolver(FilmCharacterRepository filmCharacterRepository) {
        this.filmCharacterRepository = filmCharacterRepository;
    }

    @GraphQLDescription("returns list of episodes in which this character appears")
    public @NotNull List<@NotNull Episode> getAppearsIn(FilmCharacter character) {
        return filmCharacterRepository.getAppearsInById(character.getId());
    }

    @GraphQLDeprecated("Friendship is a fluent thing and may change from episode to episode, so we do not recommend to use this api")
    public List<@NotNull FilmCharacter> getFriends(FilmCharacter character) {
        return filmCharacterRepository.getFriendsById(character.getId()).stream()
                .map(filmCharacterRepository::getCharacterById)
                .collect(Collectors.toList());
    }

}
