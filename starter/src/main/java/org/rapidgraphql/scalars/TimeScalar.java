package org.rapidgraphql.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.GraphQLScalarType;

import java.sql.Time;

public class TimeScalar {
    public static GraphQLScalarType INSTANCE;
    static {
        Coercing<Time, String> coercing = new Coercing<>() {
            @Override
            public String serialize(Object dataFetcherResult) {
                if (dataFetcherResult instanceof Time) {
                    return dataFetcherResult.toString();
                }
                return null;
            }

            @Override
            public Time parseValue(Object input) {
                if (input instanceof String) {
                    return Time.valueOf((String) input);
                    //return LocalTime.parse(input.toString(), DateTimeFormatter.ISO_LOCAL_TIME);
                }
                return null;
            }

            @Override
            public Time parseLiteral(Object input) throws CoercingParseLiteralException {
                if (input instanceof StringValue) {
                    return Time.valueOf(((StringValue) input).getValue());
                }
                throw new CoercingParseLiteralException("Invalid Time format");
            }
        };
        INSTANCE = GraphQLScalarType.newScalar()
                .name("Time")
                .description("An ISO-8601 compliant Time Scalar")
                .coercing(coercing)
                .build();
    }
}