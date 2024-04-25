package org.demographql.starwars;

import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import org.demographql.starwars.model.Episode;
import org.demographql.starwars.model.FilmCharacter;
import org.demographql.starwars.model.Human;
import org.rapidgraphql.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class HumanResolver implements GraphQLResolver<Human> {
    private final FilmCharacterResolver filmCharacterResolver;
    public @NotNull List<@NotNull Episode> getAppearsIn(Human human) {
        return filmCharacterResolver.getAppearsIn(human);
    }
    public List<@NotNull FilmCharacter> getFriends(Human human) {
        return filmCharacterResolver.getFriends(human);
    }
}