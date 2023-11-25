package org.rapidgraphql.client;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.Test;
import org.rapidgraphql.client.annotations.GraphQLMutation;
import org.rapidgraphql.client.annotations.GraphQLQuery;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQLRequestBuilderTest {
    @Value
    @Builder
    @Jacksonized
    public static class MyType {
        int a;
        int b;
    }
    interface TestApi {
        @GraphQLQuery
        List<Integer> intList(List<Integer> val);
        @GraphQLMutation("{{a, b}}")
        List<MyType> generateTypeList(Integer elements);
        @GraphQLMutation("{generateTypeList(elements: 10){a, b}}")
        List<MyType> generateListOf10();
    }
    @Test
    public void defaultQuery() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("intList", List.class);
        List<Integer> integerList = List.of(10);
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{integerList});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables)
                .containsExactly("intList", "query intList($val: [Int]){intList(val: $val)}",
                        Map.of("val", integerList));
    }

    @Test
    public void objectQuery() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("generateTypeList", Integer.class);
        Integer parameter = 10;
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{parameter});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables)
                .containsExactly("generateTypeList", "mutation generateTypeList($elements: Int){generateTypeList(elements: $elements){a, b}}",
                        Map.of("elements", parameter));
    }

    @Test
    public void customQuery() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("generateListOf10");
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables)
                .containsExactly("generateTypeList", "mutation generateListOf10{generateTypeList(elements: 10){a, b}}",
                        Map.of());
    }
}