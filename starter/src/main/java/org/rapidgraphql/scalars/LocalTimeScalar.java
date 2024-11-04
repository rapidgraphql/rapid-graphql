package org.rapidgraphql.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeScalar {
    public static GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
                .name("LocalTime")
                .description("Java LocalTime as scalar")
                .coercing(new Coercing<LocalTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalTime) {
                            return ((LocalTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_TIME);
                        } else {
                            throw new CoercingSerializeException("Invalid value '" + dataFetcherResult + "' for LocalTime");
                        }
                    }

                    @Override
                    public LocalTime parseValue(Object input) throws CoercingParseValueException {
                        try {
                            return LocalTime.parse(input.toString(), DateTimeFormatter.ISO_LOCAL_TIME);
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid value '" + input + "' for LocalTime: " + e.getMessage());
                        }
                    }

                    @Override
                    public LocalTime parseLiteral(Object input) throws CoercingParseValueException {
                        if (input instanceof StringValue) {
                            try {
                                return LocalTime.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_LOCAL_TIME);
                            } catch (Exception e) {
                                throw new CoercingParseValueException("Invalid value '" + input + "' for LocalTime: " + e.getMessage());
                            }
                        } else {
                            throw new CoercingParseValueException("Invalid value '" + input + "' for LocalDateTime");
                        }
                    }
                }).build();
}