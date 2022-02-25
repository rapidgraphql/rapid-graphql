package org.rapidgraphql.schemabuilder;

import graphql.language.Argument;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.NodeDirectivesBuilder;
import graphql.language.StringValue;
import org.rapidgraphql.annotations.GraphQLDeprecated;
import org.rapidgraphql.annotations.GraphQLDescription;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiConsumer;

public class AnnotationProcessor {

    static final Map<Class<? extends Annotation>, BiConsumer<MergedAnnotation<Annotation>, NodeDirectivesBuilder>> annotationDirectiveProcessors = Map.of(
            GraphQLDeprecated.class, AnnotationProcessor::addDeprecated,
            Deprecated.class, AnnotationProcessor::addJavaDeprecated,
            GraphQLDescription.class, AnnotationProcessor::addDescription
    );

    static public void applyAnnotations(AnnotatedElement element, NodeDirectivesBuilder builder) {
        MergedAnnotations.from(element).stream()
                .filter(mergedAnnotation -> annotationDirectiveProcessors.containsKey(mergedAnnotation.getType()))
                .forEach(mergedAnnotation -> annotationDirectiveProcessors.get(mergedAnnotation.getType()).accept(mergedAnnotation, builder));

    }
    private static void addJavaDeprecated(MergedAnnotation<Annotation> annotation, NodeDirectivesBuilder builder) {
        builder.directive(
                Directive.newDirective()
                        .name("deprecated")
                        .build());
    }
    private static void addDeprecated(MergedAnnotation<Annotation> annotation, NodeDirectivesBuilder builder) {
        builder.directive(
                Directive.newDirective()
                        .name("deprecated")
                        .argument(new Argument("reason", new StringValue(annotation.getString("reason"))))
                        .build());
    }
    private static void addDescription(MergedAnnotation<Annotation> annotation, NodeDirectivesBuilder builder) {
        Method method;
        try {
            method = builder.getClass().getDeclaredMethod("description", Description.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Builder class " + builder.getClass().getName() + " is missing description(Description) method", e);
        }
        String description = annotation.getString("value");
        try {
            method.invoke(builder, new Description(description, null, description.contains("\n")));
        } catch (IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException("Exception occurred while invoking " + builder.getClass().getName() + ".description(Description) method", e);
        }
    }

}
