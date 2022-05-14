package org.rapidgraphql.schemabuilder;

import graphql.language.ArrayValue;
import graphql.language.EnumValue;
import graphql.language.InputValueDefinition;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.StringValue;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import org.rapidgraphql.annotations.GraphQLDefault;
import org.rapidgraphql.annotations.GraphQLDefaultNull;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultValueAnnotationProcessorImplTest {
    private DefaultValueAnnotationProcessor defaultValueAnnotationProcessor = new DefaultValueAnnotationProcessorImpl();
    private static final NullValue NULL_VALUE = NullValue.newNullValue().build();

    private void methodWithIntParam(@GraphQLDefault("100") int value) {}
    private void methodWithStringParam(@GraphQLDefault("abc") String value) {}
    private void methodWithEnumParam(@GraphQLDefault("MONDAY") DayOfWeek value) {}
    private void methodWithDoubleParamNullDefault(@GraphQLDefaultNull Double value) {}
    private void methodWithListParam(@GraphQLDefault("[]") List<String> value) {}
    private void methodWithListListParam(@GraphQLDefault("[[\"abc\"]]") List<List<String>> value) {}

    @Test
    public void validDefaultIntValue() throws NoSuchMethodException {
        InputValueDefinition inputValueDefinition = calculateInputValue("methodWithIntParam", Integer.TYPE);
        assertTrue(new IntValue(BigInteger.valueOf(100)).isEqualTo(inputValueDefinition.getDefaultValue()));
    }

    @Test
    public void validDefaultStringValue() throws NoSuchMethodException {
        InputValueDefinition inputValueDefinition = calculateInputValue("methodWithStringParam", String.class);
        assertTrue(new StringValue("abc").isEqualTo(inputValueDefinition.getDefaultValue()));
    }

    @Test
    public void validDefaultEnumValue() throws NoSuchMethodException {
        InputValueDefinition inputValueDefinition = calculateInputValue("methodWithEnumParam", DayOfWeek.class);
        assertTrue(new EnumValue("MONDAY").isEqualTo(inputValueDefinition.getDefaultValue()));
    }

    @Test
    public void validDefaultNullDoubleValue() throws NoSuchMethodException {
        InputValueDefinition inputValueDefinition = calculateInputValue("methodWithDoubleParamNullDefault", Double.class);
        assertTrue(NULL_VALUE.isEqualTo(inputValueDefinition.getDefaultValue()));
    }

    @Test
    public void validDefaultEmptyListValue() throws NoSuchMethodException {
        InputValueDefinition inputValueDefinition = calculateInputValue("methodWithListParam", List.class);
        assertTrue(new ArrayValue(List.of()).isEqualTo(inputValueDefinition.getDefaultValue()));
    }

    @Test
    public void validDefaultListListValue() throws NoSuchMethodException {
        InputValueDefinition inputValueDefinition = calculateInputValue("methodWithListListParam", List.class);
        assertTrue(new ArrayValue(List.of(new ArrayValue(List.of(new StringValue("abc"))))).isEqualTo(inputValueDefinition.getDefaultValue()));
    }

    @NonNull
    private InputValueDefinition calculateInputValue(String methodName, Class<?> methodType) throws NoSuchMethodException {
        Method method = DefaultValueAnnotationProcessorImplTest.class.getDeclaredMethod(methodName, methodType);
        InputValueDefinition.Builder builder = InputValueDefinition.newInputValueDefinition();
        defaultValueAnnotationProcessor.applyAnnotations(method.getParameters()[0], builder);
        return builder.build();
    }

}