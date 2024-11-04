package org.rapidgraphql.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateScalar {
    public static GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
                .name("LocalDate")
                .description("Java LocalDate as scalar")
                .coercing(new Coercing<LocalDate, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDate) {
                            return ((LocalDate) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE);
                        } else {
                            throw new CoercingSerializeException("Invalid value '" + dataFetcherResult + "' for LocalDate");
                        }
                    }

                    @Override
                    public LocalDate parseValue(Object input) throws CoercingParseValueException {
                        try {
                            return LocalDate.parse(input.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid value '" + input + "' for LocalDateTime: " + e.getMessage());
                        }
                    }

                    @Override
                    public LocalDate parseLiteral(Object input) throws CoercingParseValueException {
                        if (input instanceof StringValue) {
                            try {
                                return LocalDate.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_LOCAL_DATE);
                            } catch (Exception e) {
                                throw new CoercingParseValueException("Invalid value '" + input + "' for LocalDate: " + e.getMessage());
                            }
                        } else {
                            throw new CoercingParseValueException("Invalid value '" + input + "' for LocalDate");
                        }
                    }
                }).build();
}