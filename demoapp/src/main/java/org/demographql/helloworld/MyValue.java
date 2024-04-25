package org.demographql.helloworld;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.rapidgraphql.annotations.GraphQLInputType;

@GraphQLInputType("MyInputValue")
@Value
@Builder
@Jacksonized
public class MyValue {
    int a;
    int b;
}
