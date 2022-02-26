package org.rapidgraphql.helloworld;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Chat {
    private String youSaid;
    private String iSay;
}
