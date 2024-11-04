package org.rapidgraphql.utils;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TypeUtilsTest {
    @SuppressWarnings("unused") // used by reflection
    private @NonNull final List<@NonNull List<String>> listListString = List.of();

    @Test
    public void validatesTypeIsList() {
        assertTrue(TypeUtils.isListType(List.class));
        assertTrue(TypeUtils.isListType(ArrayList.class));
    }

    @Test
    public void getTypeOfMissingField_returnsEmpty() {
        org.assertj.core.api.Assertions.assertThat(TypeUtils.extractClassFieldAnnotatedType(TypeUtilsTest.class, "abc")).isEqualTo(Optional.empty());
    }

    @Test
    public void listTypeExtractionTest() {
        Optional<AnnotatedType> annotatedType = TypeUtils.extractClassFieldAnnotatedType(TypeUtilsTest.class, "listListString");
        assertTrue(annotatedType.isPresent());
        Optional<AnnotatedParameterizedType> annotatedParameterizedType = TypeUtils.castToParameterizedType(annotatedType.get());
        assertTrue(annotatedParameterizedType.isPresent());
        assertTrue(TypeUtils.isListType(annotatedParameterizedType.get()));
        AnnotatedType annotatedType1 = TypeUtils.actualTypeArgument(annotatedParameterizedType.get(), 0);
        Optional<AnnotatedParameterizedType> annotatedParameterizedType1 = TypeUtils.castToParameterizedType(annotatedType1);
        assertTrue(annotatedParameterizedType1.isPresent());
        assertTrue(TypeUtils.isListType(annotatedParameterizedType1.get()));
        AnnotatedType annotatedType2 = TypeUtils.actualTypeArgument(annotatedParameterizedType1.get(), 0);
        assertThat(annotatedType2.getType()).isEqualTo(String.class);
    }
    @Test
    public void listElementTypeExtractionTest_and_nonNullValidation() {
        Optional<AnnotatedType> annotatedType = TypeUtils.extractClassFieldAnnotatedType(TypeUtilsTest.class, "listListString");
        assertTrue(annotatedType.isPresent());
        assertTrue(TypeUtils.isNotNullable(annotatedType.get()));
        Optional<AnnotatedType> annotatedType1 = TypeUtils.extractListElementType(annotatedType.get());
        assertTrue(annotatedType1.isPresent());
        assertTrue(TypeUtils.isNotNullable(annotatedType1.get()));
        Optional<AnnotatedType> annotatedType2 = TypeUtils.extractListElementType(annotatedType1.get());
        assertTrue(annotatedType2.isPresent());
        assertThat(annotatedType2.get().getType()).isEqualTo(String.class);
        Assertions.assertFalse(TypeUtils.isNotNullable(annotatedType2.get()));
        Optional<AnnotatedType> annotatedType3 = TypeUtils.extractListElementType(annotatedType2.get());
        assertFalse(annotatedType3.isPresent());
    }

}