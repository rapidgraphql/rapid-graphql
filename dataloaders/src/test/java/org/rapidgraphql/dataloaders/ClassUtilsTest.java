package org.rapidgraphql.dataloaders;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassUtilsTest {
    interface ClassInterface {
        String hello();
    }
    static class Class$$impl implements ClassInterface {
        @Override
        public String hello() {
            return "hello";
        }
    }

    @Test
    public void userClassOfImplReturnsInterface() {
        assertEquals(ClassInterface.class, ClassUtils.getUserClass(Class$$impl.class));
    }

    @Test
    public void proxyClassReturnsInterface() {
        Object proxyInstance = Proxy.newProxyInstance(
                Class$$impl.class.getClassLoader(),
                new Class[]{ClassInterface.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                }
        );
        assertEquals(ClassInterface.class, ClassUtils.getUserClass(proxyInstance));
    }

    @Test
    public void anonymousClassReturnsInterface() {
        Object anonymousInstance = new ClassInterface() {
            @Override
            public String hello() {
                return null;
            }
        };
        assertEquals(ClassInterface.class, ClassUtils.getUserClass(anonymousInstance));

    }
}
