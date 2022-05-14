package org.rapidgraphql.schemabuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class GraphQLSchemaResolverTest {
    @Data
    public static class Model {
        private LocalDate date;
    }
    @Test
    public void objectMapperDateDeserialization() throws JsonProcessingException {
        GraphQLSchemaResolver resolver = new GraphQLSchemaResolver();
        ObjectMapper objectMapper = resolver.objectMapper();
        Model model = objectMapper.readValue("{\"date\": \"2022-04-12\"}", Model.class);
        assertEquals(LocalDate.of(2022, 4, 12), model.getDate());
    }

}