package org.rapidgraphql.schemabuilder;

import lombok.Builder;
import lombok.Data;
import org.rapidgraphql.utils.TypeKind;

@Data
@Builder
public class DiscoveredClass {
    private String name;
    private Class<?> clazz;
    private TypeKind typeKind;
    private String implementsInterface;
}
