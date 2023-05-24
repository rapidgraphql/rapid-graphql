package org.rapidgraphql.schemabuilder;

import org.jetbrains.annotations.NotNull;
import org.rapidgraphql.annotations.DataLoaderMethod;
import org.rapidgraphql.annotations.GraphQLIgnore;
import org.rapidgraphql.directives.GraphQLDataLoader;
import org.slf4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.Character.isUpperCase;
import static org.slf4j.LoggerFactory.getLogger;

public class MethodsFilter {
    private static final Logger LOGGER = getLogger(MethodsFilter.class);
    private static final Set<String> objectMethodsToSkip = Set.of(
            "getClass", "equals", "hashCode", "toString", "clone", "notify", "notifyAll", "wait", "finalize", "compareTo",
            //cglib methods
            "isFrozen", "getAdvisorCount", "isProxyTarget", "isExposeProxy", "isPreFiltered", "toProxyConfigString", "isProxyTargetClass");

    private static final Set<Class<?>> unsupportedClasses = Set.of(
            Object.class, Class.class, Exception.class);

    private static final Predicate<String> unsupportedClassNames = Pattern.compile("org.springframework|org.aopalliance|java.lang.Class").asPredicate();

    @NotNull
    public static Method[] getTypeMethods(DiscoveredClass discoveredClass) {
        return ReflectionUtils.getUniqueDeclaredMethods(discoveredClass.getClazz(),
                MethodsFilter::typeMethodFilter);
    }

    @NotNull
    public static Method[] getInputTypeMethods(DiscoveredClass discoveredClass) {
        return ReflectionUtils.getUniqueDeclaredMethods(discoveredClass.getClazz(),
                MethodsFilter::inputTypeMethodFilter);
    }

    @NotNull
    public static Method[] getDataLoaderMethods(Class<? extends GraphQLDataLoader> clazz) {
        return ReflectionUtils.getUniqueDeclaredMethods(clazz,
                MethodsFilter::dataLoaderMethodFilter);
    }

    private static boolean dataLoaderMethodFilter(Method method) {
        return method.isAnnotationPresent(DataLoaderMethod.class);
    }

    public static boolean resolverMethodFilter(Class<?> sourceType, Method method) {
        if (!typeMethodFilter(method)) {
            return false;
        }
        if (sourceType!=null && (method.getParameterCount()<1 || !method.getParameterTypes()[0].equals(sourceType))) {
            LOGGER.warn("Skipping method {}::{} because it's first parameter should match resolver source type {}",
                    method.getDeclaringClass().getName(), method.getName(), sourceType.getName());
            return false;
        }
        return true;
    }

    public static boolean isMethodPublicAndNotStatic(int modifiers) {
        return !Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers);
    }

    public static boolean typeMethodFilter(Method method) {
        if (notAllowedGraphQlMethod(method)) {
            return false;
        }
        if (isSetMethod(method)) {
            return false;
        }
        if (hasInvalidParameters(method)) {
            return false;
        }
        if (hasInvalidReturnType(method)) {
            return false;
        }
        if (method.getReturnType() == Void.TYPE) {
            LOGGER.info("Skipping method {}::{} because it is returning void", method.getDeclaringClass().getName(), method.getName());
            return false;
        }
        return true;
    }

    private static boolean hasInvalidReturnType(Method method) {
        return unsupportedClasses.contains(method.getReturnType())
                || unsupportedClassNames.test(method.getReturnType().getName());
    }

    private static boolean hasInvalidParameters(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .anyMatch(clazz -> unsupportedClasses.contains(clazz)
                        || unsupportedClassNames.test(clazz.getName())
                );
    }

    public static boolean inputTypeMethodFilter(Method method) {
        if (notAllowedGraphQlMethod(method)) {
            return false;
        }
        if (!isSetMethod(method)) {
            return false;
        }
        if (method.getParameterCount() != 1) {
            return false;
        }
        if (Object.class.equals(method.getParameterTypes()[0])) {
             return false; // This is a patch to work around problem of generic classes
        }
        return true;
    }

    private static boolean notAllowedGraphQlMethod(Method method) {
        if (objectMethodsToSkip.contains(method.getName())) {
            return true;
        }
        if (isMethodPublicAndNotStatic(method.getModifiers())) {
            return true;
        }
        if (method.isAnnotationPresent(GraphQLIgnore.class)) {
            return true;
        }
        if (method.isAnnotationPresent(DataLoaderMethod.class)) {
            return true;
        }
        return false;
    }

    private static boolean isSetMethod(Method method) {
        if (method.getReturnType() != Void.TYPE) {
            return false;
        }
        String methodName = method.getName();
        return methodName.length()>=4 && methodName.startsWith("set") && isUpperCase(methodName.charAt(3));
    }

    public static String normalizeGetName(String name) {
        if (name.length()>3 && name.startsWith("get") && isUpperCase(name.charAt(3))) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        return name;
    }
    public static Optional<String> normalizeSetName(String name) {
        if (name.length()>=4 && name.startsWith("set")) {
            return Optional.of(Character.toLowerCase(name.charAt(3)) + name.substring(4));
        }
        return Optional.empty();
    }
}
