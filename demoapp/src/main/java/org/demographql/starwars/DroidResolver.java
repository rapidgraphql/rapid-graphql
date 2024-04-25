package org.demographql.starwars;

import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import org.demographql.starwars.model.Droid;
import org.demographql.starwars.model.Episode;
import org.demographql.starwars.model.FilmCharacter;
import org.rapidgraphql.annotations.NotNull;
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