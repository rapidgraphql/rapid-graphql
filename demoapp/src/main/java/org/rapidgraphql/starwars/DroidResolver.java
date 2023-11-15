package org.rapidgraphql.starwars;

import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import org.rapidgraphql.annotations.NotNull;
import org.rapidgraphql.starwars.model.Droid;
import org.rapidgraphql.starwars.model.Episode;
import org.rapidgraphql.starwars.model.FilmCharacter;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class DroidResolver implements GraphQLResolver<Droid> {
    private final FilmCharacterResolver filmCharacterResolver;
    public @NotNull List<@NotNull Episode> getAppearsIn(Droid droid) {
        return filmCharacterResolver.getAppearsIn(droid);
    }
    public List<@NotNull FilmCharacter> getFriends(Droid droid) {
        return filmCharacterResolver.getFriends(droid);
    }
}