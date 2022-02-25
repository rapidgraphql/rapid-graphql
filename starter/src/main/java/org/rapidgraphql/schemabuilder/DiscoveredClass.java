package org.rapidgraphql.schemabuilder;

import lombok.Data;

@Data
@lombok.Builder
public class DiscoveredClass {
    private String name;
    private Class<?> clazz;
    private TypeKind typeKind;
}
