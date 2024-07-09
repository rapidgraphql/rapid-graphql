package org.rapidgraphql.dataloaders;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

class TypeUtils {
    // copied from spring code
    public static Class<?> getUserClass(Object instance) {
        return getUserClass(instance.getClass());
    }

    // copied from spring code
    public static Class<?> getUserClass(Class<?> clazz) {
        if (clazz.getName().contains("$$")) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }

        return clazz;
    }

    public static Stream<Method> streamDeclaredMethods(Class<?> leafClass, Predicate<Method> mf) {
        return Arrays.stream(getUserClass(leafClass).getDeclaredMethods())
                .filter(mf);
    }
}
