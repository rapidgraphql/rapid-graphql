package org.rapidgraphql.app;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Audit {
    private String status;
}
