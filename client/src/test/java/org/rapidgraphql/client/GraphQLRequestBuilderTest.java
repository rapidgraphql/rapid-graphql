package org.rapidgraphql.client;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.Test;
import org.rapidgraphql.annotations.GraphQLInputType;
import org.rapidgraphql.annotations.NotNull;
import org.rapidgraphql.client.annotations.Bearer;
import org.rapidgraphql.client.annotations.GraphQLMutation;
import org.rapidgraphql.client.annotations.GraphQLQuery;
import org.rapidgraphql.client.annotations.HttpHeader;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQLRequestBuilderTest {
    @Value
    @Builder
    @Jacksonized
    @GraphQLInputType("MyInputType")
    public static class MyType {
        int a;
        int b;
        MyType myType;
    }
    interface TestApi {
        @GraphQLQuery
        List<Integer> intList(@NotNull List<@NotNull Integer> val);
        @GraphQLMutation("{{a, b}}")
        List<MyType> generateTypeList(Integer elements);
        @GraphQLMutation("{generateTypeList(elements: 10){a, b}}")
        List<MyType> generateListOf10();
        @GraphQLQuery
        List<Integer> queryWithHeaders(Integer iVal, @Bearer String token, @HttpHeader String xRequestId, String sVal);
        @GraphQLMutation
        MyType create(@NotNull MyType input);
        @GraphQLQuery("{{a b myType\n{\na\nb\n}\n}}")
        MyType getRecursive();
        @GraphQLQuery
        String basicTypes(int iVal, float fVal);
    }
    @Test
    public void intListQuery() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("intList", List.class);
        List<Integer> integerList = List.of(10);
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{integerList});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables)
                .containsExactly("intList", "query intList($val: [Int!]!){intList(val: $val)}",
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

    @Test
    public void queryWithHeaders() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("queryWithHeaders", Integer.class, String.class, String.class, String.class);
        Integer iVal = 10;
        String token = "token";
        String requestId = "request1234";
        String sVal = "string";
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{iVal, token, requestId, sVal});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables, GraphQLRequestBody::getHeaders)
                .containsExactly("queryWithHeaders",
                        "query queryWithHeaders($iVal: Int, $sVal: String){queryWithHeaders(iVal: $iVal, sVal: $sVal)}",
                        Map.of("iVal", iVal, "sVal", sVal),
                        Map.of("X-Request-Id", requestId, "Authorization", "Bearer " + token)
                );
    }

    @Test
    public void queryWithInputType() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("create", MyType.class);
        MyType input = MyType.builder().a(10).b(100).build();
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{input});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables, GraphQLRequestBody::getHeaders)
                .containsExactly("create",
                        "mutation create($input: MyInputType!){create(input: $input)}",
                        Map.of("input", input),
                        Map.of()
                );
    }

    @Test
    public void getRecursive() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("getRecursive");
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables, GraphQLRequestBody::getHeaders)
                .containsExactly("getRecursive",
                        "query getRecursive{getRecursive{a b myType\n{\na\nb\n}\n}}",
                        Map.of(),
                        Map.of()
                );
    }

    @Test
    public void basicTypes() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("basicTypes", Integer.TYPE, Float.TYPE);
        int iVal = 1;
        float fVal = 0.1f;
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{iVal, fVal});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables, GraphQLRequestBody::getHeaders)
                .containsExactly("basicTypes",
                        "query basicTypes($iVal: Int!, $fVal: Float!){basicTypes(iVal: $iVal, fVal: $fVal)}",
                        Map.of("iVal", iVal, "fVal", fVal),
                        Map.of()
                );
    }
}