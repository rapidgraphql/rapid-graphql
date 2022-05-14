package org.rapidgraphql.directives;

import graphql.schema.idl.SchemaDirectiveWiring;

public interface GraphQLDirectiveWiring extends SchemaDirectiveWiring {
    String getName();
}
