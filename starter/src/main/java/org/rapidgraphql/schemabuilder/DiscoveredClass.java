package org.rapidgraphql.schemabuilder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscoveredClass {
    private String name;
    private Class<?> clazz;
    private TypeKind typeKind;
    private String implementsInterface;
}
