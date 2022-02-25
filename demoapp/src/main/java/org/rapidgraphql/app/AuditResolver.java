package org.rapidgraphql.app;

import graphql.kickstart.tools.GraphQLResolver;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

@Component
public class AuditResolver implements GraphQLResolver<Audit> {
    public int getCount(Audit audit, @NotNull List<String> words) {
        return words.size();
    }

    public int abc() {
        return 0;
    }
}
