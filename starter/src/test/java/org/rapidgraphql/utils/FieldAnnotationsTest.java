package org.rapidgraphql.utils;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.rapidgraphql.annotations.GraphQLDescription;
import org.rapidgraphql.annotations.GraphQLIgnore;
import org.rapidgraphql.annotations.NotNull;
import reactor.util.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

class FieldAnnotationsTest {
    @Data
    static class TestData {
        @GraphQLIgnore
        @GraphQLDescription("description1")
        private @NotNull String notNullStringValue;
        @GraphQLIgnore
        @GraphQLDescription("description2")
        @NonNull Integer nonNullIntegerValue;
    }
    private FieldAnnotations fieldAnnotations = new FieldAnnotations(TestData.class);
    @Test
    public void notNullStringValueAnnotations() {
        assertThat(fieldAnnotations.isFieldIgnored("notNullStringValue")).isEqualTo(true);
        assertThat(fieldAnnotations.isFieldNotNull("notNullStringValue")).isEqualTo(true);
        assertThat(fieldAnnotations.findAnnotation("notNullStringValue", GraphQLDescription.class))
                .isPresent();
    }

    @Test
    public void nonNullIntegerValueAnnotations() {
        assertThat(fieldAnnotations.isFieldIgnored("nonNullIntegerValue")).isEqualTo(true);
        assertThat(fieldAnnotations.isFieldNotNull("nonNullIntegerValue")).isEqualTo(true);
        assertThat(fieldAnnotations.findAnnotation("nonNullIntegerValue", GraphQLDescription.class))
                .isPresent();
    }
}