package org.rapidgraphql.utils;

import org.rapidgraphql.annotations.GraphQLImplementation;
import org.rapidgraphql.annotations.GraphQLInterface;
import org.rapidgraphql.exceptions.GraphQLSchemaGenerationException;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class InterfaceUtils {
    private static final Logger LOGGER = getLogger(InterfaceUtils.class);

    private static List<Class<?>> allImplementations;
    public static List<Class<?>> findImplementations(Class<?> interfaceClass) {
        if (allImplementations == null) {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);

            scanner.addIncludeFilter(new AnnotationTypeFilter(GraphQLImplementation.class, false, true));
            allImplementations = new ArrayList<>();
            for (BeanDefinition bd : scanner.findCandidateComponents(ClassUtils.getPackageName(interfaceClass))) {
                LOGGER.info("Discovered interface implementation {}", bd.getBeanClassName());
                try {
                    allImplementations.add(ClassUtils.forName(bd.getBeanClassName(), interfaceClass.getClassLoader()));
                } catch (ClassNotFoundException e) {
                    throw new GraphQLSchemaGenerationException("Failed to resolve implementation class for interface", e);
                }
            }
        }
        return allImplementations.stream()
                .filter(implementation -> interfaceClass.isAssignableFrom(implementation))
                .filter(implementation -> implementation.isAnnotationPresent(GraphQLImplementation.class))
                .collect(Collectors.toList());
    }

    public static Optional<Class<?>> getGraphQLInterface(Class<?> implementation) {
        if (!implementation.isAnnotationPresent(GraphQLImplementation.class)) {
            return Optional.empty();
        }
        Class<?> superclass = implementation.getSuperclass();
        while(superclass != Object.class) {
            if (superclass.isAnnotationPresent(GraphQLInterface.class)) {
                return Optional.of(superclass);
            }
        }
        return Optional.empty();
    }

}
