package org.rapidgraphql.directives;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface RoleExtractor {
    Optional<String> getRole(HttpServletRequest request);
}
