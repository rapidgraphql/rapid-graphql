package org.demographql.helloworld;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Chat {
    private String youSaid;
    private String iSay;
}
