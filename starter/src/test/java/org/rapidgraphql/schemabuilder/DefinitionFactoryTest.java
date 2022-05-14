package org.rapidgraphql.schemabuilder;

import graphql.kickstart.tools.GraphQLResolver;
import graphql.language.Definition;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class DefinitionFactoryTest {
    public static class MyPair<T1,T2> {
        private T1 left;
        private T2 right;

        public void setLeft(T1 left) {
            this.left = left;
        }

        public void setRight(T2 right) {
            this.right = right;
        }

        public T1 getLeft() {
            return left;
        }

        public T2 getRight() {
            return right;
        }

        public MyPair(T1 left, T2 right) {
            this.left = left;
            this.right = right;
        }
    }

    public static class MyPairIntString extends MyPair<Integer, String> {
        public MyPairIntString(Integer left, String right) {
            super(left, right);
        }
    }


    @Mock
    private DefaultValueAnnotationProcessor defaultValueAnnotationProcessor;
    @InjectMocks
    private DefinitionFactory definitionFactory;

    public static class ResolverLogic implements GraphQLResolver<String>
    {}
    public static class CglibResolverLogic extends ResolverLogic
    {}

    @Test
    public void extractResolverType() {
        CglibResolverLogic cglibResolverLogic = new CglibResolverLogic();
        Optional<DiscoveredClass> discoveredClass = definitionFactory.extractResolverType(cglibResolverLogic);
        assertTrue(discoveredClass.isPresent());
        DiscoveredClass expected = DiscoveredClass.builder()
                .name("String")
                .clazz(String.class)
                .typeKind(TypeKind.OUTPUT_TYPE)
                .build();
        assertEquals(expected, discoveredClass.get());
    }

    @Test
    public void outputTypeExtendsGenerics() {
        Definition<?> actual = definitionFactory.createTypeDefinition(
                new DiscoveredClass("MyPairIntString", MyPairIntString.class, TypeKind.OUTPUT_TYPE));
        System.out.println(actual);
    }

}