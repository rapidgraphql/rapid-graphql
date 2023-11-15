package org.rapidgraphql.schemabuilder;

import graphql.kickstart.tools.GraphQLResolver;
import graphql.language.Definition;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.stream.Stream;

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


    @Test
    public void outputTypeExtendsGenerics() {
        Stream<Definition<?>> actual = definitionFactory.createOutputTypeDefinition(
                DiscoveredClass.builder().name("MyPairIntString")
                        .clazz(MyPairIntString.class).typeKind(TypeKind.OUTPUT_TYPE).build());
        Optional<Definition<?>> definition = actual.findFirst();
        assertTrue(definition.isPresent());
        System.out.println(definition.get());
    }

}