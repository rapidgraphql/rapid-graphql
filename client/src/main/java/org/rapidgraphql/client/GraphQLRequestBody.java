package org.rapidgraphql.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@Builder
public class GraphQLRequestBody {
    String query;
    @Builder.Default
    Map<String,Object> variables = new HashMap<>();
    String operationName;
    @JsonIgnore
    String fieldName;
    @Builder.Default
    @JsonIgnore
    Map<String, String> headers = new HashMap<>();
    public void header(String headerName, Object value) {
        Optional.ofNullable(value).map(Object::toString).ifPresent(
                headerValue -> headers.put(headerName, headerValue)
        );
    }

    public void headerByParameterName(String parameterName, Object value) {
        header(convertCamelCaseToDashedString(parameterName), value);
    }

    public void bearer(Object value) {
        if (value != null) {
            String authValue = "Bearer " + value;
            header("Authorization", authValue);
        }
    }

    private static String convertCamelCaseToDashedString(String camelCaseString) {
        StringBuilder dashedStringBuilder = new StringBuilder();
        dashedStringBuilder.append(Character.toUpperCase(camelCaseString.charAt(0)));

        for (int i = 1; i < camelCaseString.length(); i++) {
            char currentChar = camelCaseString.charAt(i);

            // Add a dash before each uppercase letter (except the first one)
            if (Character.isUpperCase(currentChar)) {
                dashedStringBuilder.append('-');
            }

            // Convert the character to lowercase and append to the result
            dashedStringBuilder.append(currentChar);
        }

        return dashedStringBuilder.toString();
    }

    public void variable(String name, Object arg) {
        variables.put(name, arg);
    }
}
