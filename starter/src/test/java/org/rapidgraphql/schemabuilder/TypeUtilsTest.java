package org.rapidgraphql.schemabuilder;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.NotNull;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.rapidgraphql.schemabuilder.TypeUtils.actualTypeArgument;
import static org.rapidgraphql.schemabuilder.TypeUtils.castToParameterizedType;
import static org.rapidgraphql.schemabuilder.TypeUtils.extractClassFieldAnnotatedType;
import static org.rapidgraphql.schemabuilder.TypeUtils.extractListElementType;
import static org.rapidgraphql.schemabuilder.TypeUtils.isListType;
import static org.junit.jupiter.api.Assertions.*;
import static org.rapidgraphql.schemabuilder.TypeUtils.isNotNullable;

class TypeUtilsTest {
    private @NotNull List<@NonNull List<String>> listListString;

    @Test
    public void validatesTypeIsList() {
        assertTrue(isListType(List.class));
        assertTrue(isListType(ArrayList.class));
    }

    @Test
    public void getTypeOfMissingField_returnsEmpty() {
        assertThat(extractClassFieldAnnotatedType(TypeUtilsTest.class, "abc")).isEqualTo(Optional.empty());
    }

    @Test
    public void listTypeExtractionTest() {
        Optional<AnnotatedType> annotatedType = extractClassFieldAnnotatedType(TypeUtilsTest.class, "listListString");
        assertTrue(annotatedType.isPresent());
        Optional<AnnotatedParameterizedType> annotatedParameterizedType = castToParameterizedType(annotatedType.get());
        assertTrue(annotatedParameterizedType.isPresent());
        assertTrue(isListType(annotatedParameterizedType.get()));
        AnnotatedType annotatedType1 = actualTypeArgument(annotatedParameterizedType.get(), 0);
        Optional<AnnotatedParameterizedType> annotatedParameterizedType1 = castToParameterizedType(annotatedType1);
        assertTrue(annotatedParameterizedType1.isPresent());
        assertTrue(isListType(annotatedParameterizedType1.get()));
        AnnotatedType annotatedType2 = actualTypeArgument(annotatedParameterizedType1.get(), 0);
        assertThat(annotatedType2.getType()).isEqualTo(String.class);
    }

    @Test
    public void listElementTypeExtractionTest_and_nonNullValidation() {
        Optional<AnnotatedType> annotatedType = extractClassFieldAnnotatedType(TypeUtilsTest.class, "listListString");
        assertTrue(annotatedType.isPresent());
        assertTrue(isNotNullable(annotatedType.get()));
        Optional<AnnotatedType> annotatedType1 = extractListElementType(annotatedType.get());
        assertTrue(annotatedType1.isPresent());
        assertTrue(isNotNullable(annotatedType1.get()));
        Optional<AnnotatedType> annotatedType2 = extractListElementType(annotatedType1.get());
        assertTrue(annotatedType2.isPresent());
        assertThat(annotatedType2.get().getType()).isEqualTo(String.class);
        assertFalse(isNotNullable(annotatedType2.get()));
        Optional<AnnotatedType> annotatedType3 = extractListElementType(annotatedType2.get());
        assertFalse(annotatedType3.isPresent());
    }
}