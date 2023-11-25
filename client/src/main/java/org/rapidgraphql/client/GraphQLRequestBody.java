package org.rapidgraphql.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GraphQLRequestBody {
    String query;
    Map<String,Object> variables;
    String operationName;
    @JsonIgnore
    String fieldName;
}
