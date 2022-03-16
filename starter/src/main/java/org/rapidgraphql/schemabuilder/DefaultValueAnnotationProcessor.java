package org.rapidgraphql.schemabuilder;

import graphql.language.InputValueDefinition;

import java.lang.reflect.Parameter;

public interface DefaultValueAnnotationProcessor {

    void applyAnnotations(Parameter parameter, InputValueDefinition.Builder builder);
}
