package org.rapidgraphql.app;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Item {
    private String name;
    private Double price;
}
