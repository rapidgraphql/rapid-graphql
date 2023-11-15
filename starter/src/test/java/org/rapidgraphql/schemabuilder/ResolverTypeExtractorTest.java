package org.rapidgraphql.schemabuilder;

import graphql.kickstart.tools.GraphQLResolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResolverTypeExtractorTest {

    public static class ResolverLogic implements GraphQLResolver<String>
    {}
    public static class CglibResolverLogic extends ResolverLogic
    {}

    @Test
    public void extractResolverType() {
        CglibResolverLogic cglibResolverLogic = new CglibResolverLogic();
        Optional<DiscoveredClass> discoveredClass = ResolverTypeExtractor.extractResolverType(cglibResolverLogic);
        assertTrue(discoveredClass.isPresent());
        DiscoveredClass expected = DiscoveredClass.builder()
                .name("String")
                .clazz(String.class)
                .typeKind(TypeKind.OUTPUT_TYPE)
                .build();
        assertEquals(expected, discoveredClass.get());
    }

    public static class MyResolver<T> implements GraphQLResolver<T> {
    }

    @Test
    public void testGeneric() {
        MyResolver<String> myResolver = new MyResolver<>() {};
        Optional<DiscoveredClass> discoveredClass = ResolverTypeExtractor.extractResolverType(myResolver);
        assertTrue(discoveredClass.isPresent());
        DiscoveredClass expected = DiscoveredClass.builder()
                .name("String")
                .clazz(String.class)
                .typeKind(TypeKind.OUTPUT_TYPE)
                .build();
        assertEquals(expected, discoveredClass.get());
    }
}