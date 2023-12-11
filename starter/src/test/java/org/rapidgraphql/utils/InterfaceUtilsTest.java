package org.rapidgraphql.utils;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.rapidgraphql.annotations.GraphQLImplementation;
import org.rapidgraphql.annotations.GraphQLInterface;
import org.rapidgraphql.utils.InterfaceUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterfaceUtilsTest {
    @GraphQLInterface
    interface MyInterface1 {
        int getA();
    }

    @Data
    @GraphQLImplementation
    public static class MyImplementation1 implements MyInterface1 {
        int a;
        int b;
    }

    @GraphQLInterface
    public interface MyInterface2 {
        int getB();
    }
    @Getter
    @Setter
    public static abstract class MyAbstractImplementation2 implements MyInterface2 {
        int b;
    }

    @Getter
    @Setter
    @GraphQLImplementation
    public static class MyImplementation2 extends MyAbstractImplementation2 {
        int a;
    }

    @Getter
    @Setter
    @GraphQLImplementation
    public static class MyImplementation3 extends MyAbstractImplementation2 {
        int a;
        int c;
    }

    @Test
    public void testFindImplementations() {
        List<Class<?>> implementations = InterfaceUtils.findImplementations(MyInterface1.class);
        assertThat(implementations).contains(MyImplementation1.class);
        implementations = InterfaceUtils.findImplementations(MyInterface2.class);
        assertThat(implementations).containsExactlyInAnyOrder(MyImplementation2.class, MyImplementation3.class);
    }

}