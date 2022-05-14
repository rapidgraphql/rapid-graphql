package org.rapidgraphql.directives;

import graphql.GraphqlErrorException;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import graphql.language.ArrayValue;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.rapidgraphql.errors.ErrorType.UNAUTHENTICATED;
import static org.rapidgraphql.utils.GraphQLUtils.parseLiteral;

public class SecuredDirectiveWiring implements GraphQLDirectiveWiring {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredDirectiveWiring.class);

    public static final String DIRECTIVE_NAME = "secured";
    public static final String DIRECTIVE_ARGUMENT_NAME = "roles";

    private final boolean authEnabled;
    private final List<RoleExtractor> roleExtractors;

    public SecuredDirectiveWiring(boolean authEnabled, List<RoleExtractor> roleExtractors) {
        this.authEnabled = authEnabled;
        this.roleExtractors = roleExtractors;
    }

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        GraphQLFieldDefinition field = environment.getElement();

        if (!authEnabled || field.getDirective(DIRECTIVE_NAME) == null) {
            return field;
        }

        setDataFetcher(environment, field);

        return field;
    }

    private void setDataFetcher(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment, GraphQLFieldDefinition field) {
        GraphQLFieldsContainer parentType = environment.getFieldsContainer();
        GraphQLCodeRegistry.Builder registry = environment.getCodeRegistry();
        // build a data fetcher that first checks authorisation roles before then calling the original data fetcher
        @SuppressWarnings("rawtypes")
        DataFetcher originalDataFetcher = registry.getDataFetcher(parentType, field);
        registry.dataFetcher(parentType, field, dataFetchingEnvironment -> evaluateUserRoleAndReturnResultOrError(field, originalDataFetcher, dataFetchingEnvironment));
    }

    @SuppressWarnings("rawtypes")
    private Object evaluateUserRoleAndReturnResultOrError(GraphQLFieldDefinition field, DataFetcher originalDataFetcher, DataFetchingEnvironment dataFetchingEnvironment)
            throws Exception {
        Optional<String> role = extractRoleFromRequest(dataFetchingEnvironment);

        List<String> allowedRoles = getQueryClearance(dataFetchingEnvironment);

        if (role.isPresent() && allowedRoles.contains(role.get())) {
            return originalDataFetcher.get(dataFetchingEnvironment);
        } else {
            logAuthenticationFailure(field.getName(), role, allowedRoles);
            return buildErrorResult(field, dataFetchingEnvironment);
        }
    }

    private Optional<String> extractRoleFromRequest(DataFetchingEnvironment dataFetchingEnvironment) {
        if (roleExtractors == null) {
            return Optional.empty();
        }
        DefaultGraphQLServletContext context = dataFetchingEnvironment.getContext();
        return roleExtractors.stream()
                .map(roleExtractor -> roleExtractor.getRole(context.getHttpServletRequest()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private void logAuthenticationFailure(String fieldName, Optional<String> role, List<String> allowedRoles) {
        LOGGER.warn("Access to {} was blocked because {} is not one of required roles: {}",
                fieldName, role.orElse("empty role"), allowedRoles);
    }

    @SuppressWarnings("unchecked")
    private List<String> getQueryClearance(DataFetchingEnvironment dataFetchingEnvironment) {
        ArrayValue arrayValue = (ArrayValue) dataFetchingEnvironment.getFieldDefinition()
                .getDirective(DIRECTIVE_NAME)
                .getArgument(DIRECTIVE_ARGUMENT_NAME)
                .getArgumentValue()
                .getValue();

        return (List<String>)requireNonNull(parseLiteral(arrayValue));
    }

    private DataFetcherResult<Object> buildErrorResult(GraphQLFieldDefinition field, DataFetchingEnvironment dataFetchingEnvironment) {
        return DataFetcherResult.newResult().error(
                new GraphqlErrorException.Builder()
                        .errorClassification(UNAUTHENTICATED)
                        .message("Authentication required")
                        .path(dataFetchingEnvironment.getExecutionStepInfo().getPath().toList())
                        .sourceLocation(field.getDefinition().getSourceLocation())
                        .build())
                .build();
    }

    @Override
    public String getName() {
        return DIRECTIVE_NAME;
    }
}
