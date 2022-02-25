package org.rapidgraphql.schemabuilder;

import org.rapidgraphql.annotations.GraphQLIgnore;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

import static java.lang.Character.isUpperCase;
import static org.slf4j.LoggerFactory.getLogger;

public class MethodsFilter {
    private static final Logger LOGGER = getLogger(MethodsFilter.class);
    private static final Set<String> objectMethodsToSkip = Set.of(
            "getClass", "equals", "hashCode", "toString", "clone", "notify", "notifyAll", "wait", "finalize", "compareTo");

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
        if (objectMethodsToSkip.contains(method.getName())) {
            return false;
        }
        if (isMethodPublicAndNotStatic(method.getModifiers())) {
            return false;
        }
        if (method.isAnnotationPresent(GraphQLIgnore.class)) {
            return false;
        }
        if (isSetMethod(method)) {
            return false;
        }
        if (method.getReturnType() == Void.TYPE) {
            LOGGER.info("Skipping method {}::{} because it is returning void", method.getDeclaringClass().getName(), method.getName());
            return false;
        }
        return true;
    }

    public static boolean inputTypeMethodFilter(Method method) {
        if (objectMethodsToSkip.contains(method.getName())) {
            return false;
        }
        if (isMethodPublicAndNotStatic(method.getModifiers())) {
            return false;
        }
        if (method.isAnnotationPresent(GraphQLIgnore.class)) {
            return false;
        }
        if (!isSetMethod(method)) {
            return false;
        }
        if (method.getParameterCount() != 1) {
            return false;
        }
        return true;
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
