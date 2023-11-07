package org.rapidgraphql.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.GraphQLScalarType;

import java.time.OffsetDateTime;

public class TimestampScalar {
    public static GraphQLScalarType INSTANCE;
    static {
        Coercing<java.sql.Timestamp, String> coercing = new Coercing<>() {
            @Override
            public String serialize(Object dataFetcherResult) {
                if (dataFetcherResult instanceof java.sql.Timestamp) {
                    return ((java.sql.Timestamp) dataFetcherResult).toInstant().toString();
                }
                return null;
            }

            @Override
            public java.sql.Timestamp parseValue(Object input) {
                if (input instanceof String) {
                    return java.sql.Timestamp.from(java.time.Instant.parse((String) input));
                }
                return null;
            }

            @Override
            public java.sql.Timestamp parseLiteral(Object input) throws CoercingParseLiteralException {
                if (input instanceof StringValue) {
                    return java.sql.Timestamp.from(java.time.Instant.parse(((StringValue) input).getValue()));
                }
                throw new CoercingParseLiteralException("Invalid Timestamp format");
            }
        };
        INSTANCE = GraphQLScalarType.newScalar()
                .name("Timestamp")
                .description("An ISO-8601 compliant Timestamp Scalar")
                .coercing(coercing)
                .build();
    }
}