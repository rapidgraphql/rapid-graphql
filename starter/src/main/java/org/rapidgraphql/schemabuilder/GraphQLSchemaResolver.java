package org.rapidgraphql.schemabuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.kickstart.autoconfigure.tools.GraphQLJavaToolsAutoConfiguration;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.kickstart.tools.*;
import graphql.language.Definition;
import graphql.language.ScalarTypeDefinition;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rapidgraphql.dataloaders.DataLoaderRegistryFactory;
import org.rapidgraphql.dataloaders.GraphQLDataLoader;
import org.rapidgraphql.directives.GraphQLDirectiveWiring;
import org.rapidgraphql.directives.RoleExtractor;
import org.rapidgraphql.directives.SecuredDirectiveWiring;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureBefore(GraphQLJavaToolsAutoConfiguration.class)
@AutoConfiguration
public class GraphQLSchemaResolver {
    private static final Logger LOGGER = getLogger(GraphQLSchemaResolver.class);
    private final DefinitionFactory definitionFactory= new DefinitionFactory(new DefaultValueAnnotationProcessorImpl());

    private SchemaParser schemaParser = null;

    @Value("${rapidgraphql.dataloaders.reschedule-interval-in-millis:10}")
    private Long dataloadersRescheduleIntervalInMillis;

    @Value("${rapidgraphql.dataloaders.scheduler-pool-size:1}")
    private int dataloadersSchedulerPoolSize;

    @Value("${rapidgraphql.parsed-queries-cache-size:100}")
    private int parsedQueriesCacheSize;

    static class MyTypeDefinitionFactory implements TypeDefinitionFactory {
        private final List<? extends GraphQLResolver<?>> resolvers;
        private final List<Definition<?>> definitions;

        MyTypeDefinitionFactory(List<? extends GraphQLResolver<?>> resolvers, List<Definition<?>> definitions) {
            this.resolvers = resolvers;
            this.definitions = definitions;
        }

        @NonNull
        @Override
        public List<Definition<?>> create(final List<Definition<?>> existing) {
            return definitions;
        }
    }
    private List<Definition<?>> processResolvers(List<? extends GraphQLResolver<?>> resolvers) {
        List<Definition<?>> definitions = new ArrayList<>();
        definitions.add(definitionFactory.createRoleDirectiveDefinition());
        definitions.addAll(definitionFactory.getScalars().stream()
                .map(scalar -> ScalarTypeDefinition.newScalarTypeDefinition().name(scalar.getName()).build())
                .toList());
        definitions.addAll(resolvers.stream()
                .flatMap(definitionFactory::createTypeDefinition)
                .toList());
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

    @ConditionalOnMissingBean(PreparsedDocumentProvider.class)
    @Bean
    public PreparsedDocumentProvider getPreparsedDocumentProvider() {
        Cache<String, PreparsedDocumentEntry> cache = Caffeine.newBuilder().maximumSize(parsedQueriesCacheSize).build();

        PreparsedDocumentProvider preparsedCache = (executionInput, parseAndValidateFunction) -> {
            Function<String, PreparsedDocumentEntry> mapCompute = key -> parseAndValidateFunction.apply(executionInput);
            return cache.get(executionInput.getQuery(), mapCompute);
        };
        return preparsedCache;
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
        List<Definition<?>> definitions = processResolvers(resolvers);
        SchemaParserOptions options = SchemaParserOptions.newOptions()
                .typeDefinitionFactory(new MyTypeDefinitionFactory(resolvers, definitions))
                .objectMapperProvider(perFieldObjectMapperProvider)
                .build();
        SchemaParserBuilder schemaParserBuilder = SchemaParser.newParser()
                .resolvers(resolvers)
                .options(options)
                .scalars(definitionFactory.getScalars());
        addDirectives(schemaParserBuilder, directives);
        addDictionary(schemaParserBuilder, definitionFactory.getImplementationDictionary());
        schemaParser = schemaParserBuilder.build();
        return schemaParser;
    }

    @Bean
    public DataLoaderRegistryFactory dataLoaderRegistryFactory(List<? extends GraphQLDataLoader> dataLoaders) {
        return new DataLoaderRegistryFactory(dataLoaders, dataloadersRescheduleIntervalInMillis, dataloadersSchedulerPoolSize);
    }
    @Bean
    public GraphQLServletContextBuilder getGraphQLServletContextBuilder(DataLoaderRegistryFactory dataLoaderRegistryFactory) {
        return new RapidGraphQLContextBuilder(dataLoaderRegistryFactory);
    }

    private void addDirectives(SchemaParserBuilder schemaParserBuilder, List<GraphQLDirectiveWiring> directives) {
        for (GraphQLDirectiveWiring directiveWiring: directives) {
            schemaParserBuilder.directive(directiveWiring.getName(), directiveWiring);
        }
    }

    private void addDictionary(SchemaParserBuilder schemaParserBuilder, Map<String, Class<?>> implementationDictionary) {
        implementationDictionary.forEach((name, clazz) -> {
            LOGGER.info("Adding {} -> {} to dictionary", name, clazz.getCanonicalName());
            schemaParserBuilder.dictionary(name, clazz);
        });
    }
}
