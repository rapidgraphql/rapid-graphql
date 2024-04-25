package org.demographql.starwars.repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.demographql.starwars.model.Human;
import org.demographql.starwars.model.Droid;
import org.demographql.starwars.model.Episode;
import org.demographql.starwars.model.FilmCharacter;
import org.springframework.stereotype.Component;

import java.util.*;
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
            droidEntry(R_2_D_2, "R2-D2", "Astromech"),
            humanEntry(LUKE_SKYWALKER, "Luke Skywalker", 1.7f),
            humanEntry(LEIA_ORGANA, "Han Solo", 1.8f),
            humanEntry(HAN_SOLO, "Leia Organa", 1.5f)
    );

    private static Map.Entry<Long, FilmCharacter> humanEntry(Long id, String name, Float height) {
        return Map.entry(id, Human.builder().id(id).name(name).height(height).build());
    }

    private static Map.Entry<Long, FilmCharacter> droidEntry(Long id, String name, String primaryFunction) {
        return Map.entry(id, Droid.builder().id(id).name(name).primaryFunction(primaryFunction).build());
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
        return (List<Episode>)appearsInMultimap.asMap().getOrDefault(id, List.of());
    }

    @Override
    public FilmCharacter getCharacterById(Long id) {
        return characters.get(id);
    }

    @Override
    public List<FilmCharacter> getAllCharacters() {
        return new ArrayList<>(characters.values());
    }
}
