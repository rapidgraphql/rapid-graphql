package org.rapidgraphql.starwars.repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.rapidgraphql.starwars.model.Episode;
import org.rapidgraphql.starwars.model.FilmCharacter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FilmCharacterRepositoryImpl implements FilmCharacterRepository {

    public static final Long R_2_D_2 = 2001L; //;
    public static final Long LUKE_SKYWALKER = 1000L; //;
    public static final Long HAN_SOLO = 1002L; //;
    public static final Long LEIA_ORGANA = 1003L; //;
    private static final Map<Episode,Long> heroIds = Map.of(
            Episode.NEWHOPE, R_2_D_2,
            Episode.EMPIRE, LUKE_SKYWALKER,
            Episode.JEDI, R_2_D_2
    );
    private static List<Pair<Long,Long>> friendsList = List.of(
            Pair.of(R_2_D_2, LUKE_SKYWALKER),
            Pair.of(R_2_D_2, HAN_SOLO),
            Pair.of(R_2_D_2, LEIA_ORGANA)
    );
    private static Multimap<Long, Episode> appearsInMultimap = ArrayListMultimap.create();
    {
        appearsInMultimap.put(R_2_D_2, Episode.NEWHOPE);
        appearsInMultimap.put(R_2_D_2, Episode.EMPIRE);
        appearsInMultimap.put(R_2_D_2, Episode.JEDI);
        appearsInMultimap.put(LUKE_SKYWALKER, Episode.NEWHOPE);
        appearsInMultimap.put(LUKE_SKYWALKER, Episode.EMPIRE);
        appearsInMultimap.put(LUKE_SKYWALKER, Episode.JEDI);
        appearsInMultimap.put(HAN_SOLO, Episode.NEWHOPE);
        appearsInMultimap.put(HAN_SOLO, Episode.EMPIRE);
        appearsInMultimap.put(HAN_SOLO, Episode.JEDI);
        appearsInMultimap.put(LEIA_ORGANA, Episode.NEWHOPE);
        appearsInMultimap.put(LEIA_ORGANA, Episode.EMPIRE);
        appearsInMultimap.put(LEIA_ORGANA, Episode.JEDI);
    }
    private static Map<Long, FilmCharacter> characters = Map.ofEntries(
            characterEntry(R_2_D_2, "R2-D2"),
            characterEntry(LUKE_SKYWALKER, "Luke Skywalker"),
            characterEntry(LEIA_ORGANA, "Han Solo"),
            characterEntry(HAN_SOLO, "Leia Organa")
    );

    private static Map.Entry<Long, FilmCharacter> characterEntry(Long id, String name) {
        return Map.entry(id, FilmCharacter.builder().id(id).name(name).build());
    }

    @Override
    public Long getHeroId(Episode episode) {
        return heroIds.get(episode);
    }

    @Override
    public List<Long> getFriendsById(Long id) {
        return friendsList.stream()
                .map(pair -> pair.getLeft().equals(id)? pair.getRight(): (pair.getRight().equals(id)? pair.getLeft() : null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getAppearsInById(Long id) {
        Collection<Episode> episodes = appearsInMultimap.get(id);
        return episodes!=null? (List<Episode>)episodes: List.of();
    }

    @Override
    public FilmCharacter getCharacterById(Long id) {
        return characters.get(id);
    }
}
