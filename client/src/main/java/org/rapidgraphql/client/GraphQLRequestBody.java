package org.rapidgraphql.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;

@Data
@Builder
public class GraphQLRequestBody {
    @NonNull String query;
    Map<String,Object> variables;
    String operationName;
    @JsonIgnore
    String fieldName;
}
