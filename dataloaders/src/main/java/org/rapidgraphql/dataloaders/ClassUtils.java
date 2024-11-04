package org.rapidgraphql.dataloaders;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ClassUtils {
    private static final Predicate<String> anonymousClassPredicate = Pattern.compile("\\$(Proxy)?[0-9]+$").asPredicate();
    // updated from spring code
    public static Class<?> getUserClass(Object instance) {
        return getUserClass(instance.getClass());
    }

    // copied from spring code
    public static Class<?> getUserClass(Class<?> clazz) {
        String clazzName = clazz.getName();
        if (clazzName.contains("$$") || anonymousClassPredicate.test(clazzName)) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == Object.class || superclass == Proxy.class) {
                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces.length > 0) {
                    return interfaces[0];
                }
            } else if (superclass != null) {
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
