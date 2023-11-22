package org.rapidgraphql.client.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.JsonNode;
import kong.unirest.core.json.JSONObject;
import org.rapidgraphql.client.exceptions.GraphQLError;
import org.rapidgraphql.client.exceptions.GraphQLErrorException;

import java.util.List;

public abstract class AbstractResultExtractor implements ResultExtractor {
    private static final ValueExtractor errorsExtractor = new ListValueExtractor(
            new ObjectValueExtractor(GraphQLError.class, new ObjectMapper())
    );
    @Override
    public Object extract(JsonNode jsonNode) {
        if (jsonNode.getObject().has("errors")) {
            List<GraphQLError> errors = (List<GraphQLError>)errorsExtractor.extract(jsonNode.getObject(), "errors");
            throw new GraphQLErrorException(errors);
        }
        JSONObject data = jsonNode.getObject().optJSONObject("data");
        return applyOnData(data);
    }

    protected abstract Object applyOnData(JSONObject data);
}
