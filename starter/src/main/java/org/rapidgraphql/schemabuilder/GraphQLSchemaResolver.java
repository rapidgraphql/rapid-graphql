package org.rapidgraphql.schemabuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import graphql.kickstart.autoconfigure.tools.GraphQLJavaToolsAutoConfiguration;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.kickstart.tools.PerFieldObjectMapperProvider;
import graphql.kickstart.tools.SchemaParser;
import graphql.kickstart.tools.SchemaParserBuilder;
import graphql.kickstart.tools.SchemaParserOptions;
import graphql.kickstart.tools.TypeDefinitionFactory;
import graphql.language.Definition;
import graphql.language.ScalarTypeDefinition;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rapidgraphql.directives.GraphQLDataLoader;
import org.rapidgraphql.directives.GraphQLDirectiveWiring;
import org.rapidgraphql.directives.RoleExtractor;
import org.rapidgraphql.directives.SecuredDirectiveWiring;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureBefore(GraphQLJavaToolsAutoConfiguration.class)
@Configuration
public class GraphQLSchemaResolver {
    private static final Logger LOGGER = getLogger(GraphQLSchemaResolver.class);
    private final DefinitionFactory definitionFactory;

    private SchemaParser schemaParser = null;

    public GraphQLSchemaResolver() {
        definitionFactory = new DefinitionFactory(new DefaultValueAnnotationProcessorImpl());
    }

    class MyTypeDefinitionFactory implements TypeDefinitionFactory {
        private final List<? extends GraphQLResolver<?>> resolvers;

        MyTypeDefinitionFactory(List<? extends GraphQLResolver<?>> resolvers) {
            this.resolvers = resolvers;
        }

        @NonNull
        @Override
        public List<Definition<?>> create(final List<Definition<?>> existing) {
            return processResolvers(resolvers);
        }
    }
    private List<Definition<?>> processResolvers(List<? extends GraphQLResolver<?>> resolvers) {
        List<Definition<?>> definitions = new ArrayList<>();
        definitions.add(definitionFactory.createRoleDirectiveDefinition());
        definitions.addAll(definitionFactory.getScalars().stream()
                .map(scalar -> ScalarTypeDefinition.newScalarTypeDefinition().name(scalar.getName()).build())
                .collect(Collectors.toList()));
        definitions.addAll(resolvers.stream()
                .map(definitionFactory::createTypeDefinition)
                .collect(Collectors.toList()));
        definitions.addAll(definitionFactory.processTypesQueue());
        return definitions;
    }

    @ConditionalOnBean(RoleExtractor.class)
    @Bean
    public SecuredDirectiveWiring securedDirectiveWiring(List<RoleExtractor> roleExtractors) {
        return new SecuredDirectiveWiring(true, roleExtractors);
    }

    @Bean("rapidGraphQLObjectMapper")
    public ObjectMapper objectMapper() {
        return JsonMapper.builder() // or different mapper for other format
                .addModule(new ParameterNamesModule())
                .addModule(new Jdk8Module())
                .addModule(new JavaTimeModule())
                .build();
    }

    @ConditionalOnMissingBean(PerFieldObjectMapperProvider.class)
    @Bean
    public PerFieldObjectMapperProvider getPerFieldObjectMapperProvider(@Qualifier("rapidGraphQLObjectMapper") ObjectMapper objectMapper) {
        return fieldDefinition -> objectMapper;
    }

    @Bean
    public SchemaParser schemaParser(List<? extends GraphQLResolver<?>> resolvers,
            List<GraphQLDirectiveWiring> directives,
            PerFieldObjectMapperProvider perFieldObjectMapperProvider
    ) {
        if (schemaParser != null) {
            LOGGER.error("Recreating SchemaParser bean");
            return schemaParser;
        }
        LOGGER.info("{} resolvers and {} directives found", resolvers.size(), directives.size());
        SchemaParserOptions options = SchemaParserOptions.newOptions()
                .typeDefinitionFactory(new MyTypeDefinitionFactory(resolvers))
                .objectMapperProvider(perFieldObjectMapperProvider)
                .build();
        SchemaParserBuilder schemaParserBuilder = SchemaParser.newParser()
                .resolvers(resolvers)
                .options(options)
                .scalars(definitionFactory.getScalars());
        addDirectives(schemaParserBuilder, directives);
        schemaParser = schemaParserBuilder.build();
        return schemaParser;
    }

    @Bean
    public GraphQLServletContextBuilder getGraphQLServletContextBuilder(List<? extends GraphQLDataLoader> dataLoaders) {
        return new RapidGraphQLContextBuilder(new DataLoaderRegistryFactory(dataLoaders));
    }

    private void addDirectives(SchemaParserBuilder schemaParserBuilder, List<GraphQLDirectiveWiring> directives) {
        for (GraphQLDirectiveWiring directiveWiring: directives) {
            schemaParserBuilder.directive(directiveWiring.getName(), directiveWiring);
        }
    }
}
