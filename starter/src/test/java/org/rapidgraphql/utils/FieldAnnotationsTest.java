package org.rapidgraphql.utils;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.rapidgraphql.annotations.GraphQLDescription;
import org.rapidgraphql.annotations.GraphQLIgnore;
import org.rapidgraphql.annotations.GraphQLInputType;
import org.rapidgraphql.annotations.NotNull;
import reactor.util.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

class FieldAnnotationsTest {
    @Data
    @GraphQLInputType(value = "TestDataInput", ignore = {"id"})
    static class TestData {
        @GraphQLIgnore
        @GraphQLDescription("description1")
        private @NotNull String notNullStringValue;
        @GraphQLIgnore
        @GraphQLDescription("description2")
        @NonNull Integer nonNullIntegerValue;
        Integer id;
    }
    private FieldAnnotations fieldAnnotations = new FieldAnnotations(TestData.class, TypeKind.OUTPUT_TYPE);
    private FieldAnnotations fieldAnnotationsInput = new FieldAnnotations(TestData.class, TypeKind.INPUT_TYPE);
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

    @Test
    public void inputTypeIgnoreFields() {
        assertThat(fieldAnnotations.isFieldIgnored("id")).isEqualTo(false);
        assertThat(fieldAnnotationsInput.isFieldIgnored("id")).isEqualTo(true);
    }
}