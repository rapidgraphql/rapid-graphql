package org.rapidgraphql.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeScalar {
    public static GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
                .name("LocalDateTime")
                .description("Java LocalDateTime as scalar")
                .coercing(new Coercing<LocalDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime) {
                            return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } else {
                            throw new CoercingSerializeException("Invalid value '" + dataFetcherResult + "' for LocalDateTime");
                        }
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
                        try {
                            return LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid value '" + input + "' for LocalDateTime: " + e.getMessage());
                        }
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) throws CoercingParseValueException {
                        if (input instanceof StringValue) {
                            try {
                                return LocalDateTime.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } catch (Exception e) {
                                throw new CoercingParseValueException("Invalid value '" + input + "' for LocalDateTime: " + e.getMessage());
                            }
                        } else {
                            throw new CoercingParseValueException("Invalid value '" + input + "' for LocalDateTime");
                        }
                    }
                }).build();

}
