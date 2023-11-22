package org.rapidgraphql.client;

import org.junit.jupiter.api.Test;
import org.rapidgraphql.client.annotations.GraphQLQuery;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQLRequestBuilderTest {
    interface TestApi {
        @GraphQLQuery
        List<Integer> intList(List<Integer> val);
    }
    @Test
    public void intListQuery() throws NoSuchMethodException {
        Method method = TestApi.class.getMethod("intList", List.class);
        List<Integer> integerList = List.of(10);
        GraphQLRequestBody graphQLRequestBody = GraphQLRequestBuilder.build(method, new Object[]{integerList});
        assertThat(graphQLRequestBody)
                .isNotNull()
                .extracting(GraphQLRequestBody::getFieldName, GraphQLRequestBody::getQuery, GraphQLRequestBody::getVariables)
                .containsExactly("intList", "query intList($val: [Int]){intList(val: $val)}",
                        Map.of("val", integerList));
    }
}