package org.rapidgraphql.schemabuilder;

import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.NodeDirectivesBuilder;
import graphql.language.StringValue;
import graphql.language.Value;
import org.rapidgraphql.annotations.GraphQLDeprecated;
import org.rapidgraphql.annotations.GraphQLDescription;
import org.rapidgraphql.annotations.GraphQLSecured;
import org.rapidgraphql.directives.SecuredDirectiveWiring;
import org.slf4j.Logger;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class AnnotationProcessor {

    private static final Logger LOGGER = getLogger(AnnotationProcessor.class);
    static final Map<Class<? extends Annotation>, BiConsumer<MergedAnnotation<Annotation>, NodeDirectivesBuilder>> annotationDirectiveProcessors = Map.of(
            GraphQLDeprecated.class, AnnotationProcessor::addDeprecated,
            Deprecated.class, AnnotationProcessor::addJavaDeprecated,
            GraphQLDescription.class, AnnotationProcessor::addDescription,
            GraphQLSecured.class, AnnotationProcessor::addGraphQlSecurity
    );

    static public void applyAnnotations(AnnotatedElement element, NodeDirectivesBuilder builder) {
        applyMergedAnnotations(MergedAnnotations.from(element), builder);
    }

    static public void applyAnnotations(Annotation[] annotations, NodeDirectivesBuilder builder) {
        if (annotations == null || annotations.length == 0) {
            return;
        }
        applyMergedAnnotations(MergedAnnotations.from(annotations), builder);
    }

    static public void applyMergedAnnotations(MergedAnnotations mergedAnnotations, NodeDirectivesBuilder builder) {
        mergedAnnotations.stream()
                .filter(mergedAnnotation -> annotationDirectiveProcessors.containsKey(mergedAnnotation.getType()))
                .forEach(mergedAnnotation -> annotationDirectiveProcessors.get(mergedAnnotation.getType()).accept(mergedAnnotation, builder));

    }

    private static void addGraphQlSecurity(MergedAnnotation<Annotation> annotation, NodeDirectivesBuilder builder) {
        builder.directive(
                Directive.newDirective()
                        .name(SecuredDirectiveWiring.DIRECTIVE_NAME)
                        .argument(new Argument(SecuredDirectiveWiring.DIRECTIVE_ARGUMENT_NAME, getRolesValue(annotation)))
                        .build());
    }

    private static ArrayValue getRolesValue(MergedAnnotation<Annotation> annotation) {
        List<Value> rolesList = Arrays.stream(annotation.getStringArray("roles"))
                .map(role -> (Value) new StringValue(role))
                .collect(Collectors.toList());
        return new ArrayValue(rolesList);
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
