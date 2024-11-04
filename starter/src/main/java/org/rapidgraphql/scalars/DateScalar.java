package org.rapidgraphql.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.GraphQLScalarType;

import java.sql.Date;

public class DateScalar {
    public static GraphQLScalarType INSTANCE;
    static {
        Coercing<Date, String> coercing = new Coercing<>() {
            @Override
            public String serialize(Object dataFetcherResult) {
                if (dataFetcherResult instanceof Date) {
                    return dataFetcherResult.toString();
                }
                return null;
            }

            @Override
            public Date parseValue(Object input) {
                if (input instanceof String) {
                    return Date.valueOf(input.toString());
                }
                return null;
            }

            @Override
            public Date parseLiteral(Object input) throws CoercingParseLiteralException {
                if (input instanceof StringValue) {
                    return Date.valueOf(((StringValue) input).getValue());
                }
                throw new CoercingParseLiteralException("Invalid Date format");
            }
        };
        INSTANCE = GraphQLScalarType.newScalar()
                .name("Date")
                .description("An ISO-8601 compliant Date Scalar")
                .coercing(coercing)
                .build();
    }
}