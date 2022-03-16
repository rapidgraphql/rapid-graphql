package org.rapidgraphql.schemabuilder;

import graphql.kickstart.autoconfigure.tools.GraphQLJavaToolsAutoConfiguration;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.kickstart.tools.SchemaParser;
import graphql.kickstart.tools.SchemaParserOptions;
import graphql.kickstart.tools.TypeDefinitionFactory;
import graphql.language.Definition;
import graphql.language.ScalarTypeDefinition;
import graphql.scalars.ExtendedScalars;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
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
    private final List<? extends GraphQLResolver<?>> resolvers;

    private final DefinitionFactory definitionFactory;

    public GraphQLSchemaResolver(List<? extends GraphQLResolver<?>> resolvers) {
        this.resolvers = resolvers;
        definitionFactory = new DefinitionFactory(new DefaultValueAnnotationProcessorImpl());
    }

    class MyTypeDefinitionFactory implements TypeDefinitionFactory {
        @NotNull
        @Override
        public List<Definition<?>> create(final List<Definition<?>> existing) {
            return processResolvers();
        }
    }
    private List<Definition<?>> processResolvers() {
        List<Definition<?>> definitions = new ArrayList<>();
        definitions.addAll(definitionFactory.getScalars().stream()
                .map(scalar -> ScalarTypeDefinition.newScalarTypeDefinition().name(ExtendedScalars.GraphQLLong.getName()).build())
                .collect(Collectors.toList()));
        definitions.addAll(resolvers.stream().map(resolver -> definitionFactory.createTypeDefinition(resolver)).collect(Collectors.toList()));
        definitions.addAll(definitionFactory.processTypesQueue());
        return definitions;
    }

    @Bean
    public SchemaParser schemaParser() {
        SchemaParserOptions options = SchemaParserOptions.newOptions()
                .typeDefinitionFactory(new MyTypeDefinitionFactory())
                .build();
        return SchemaParser.newParser()
                .resolvers(resolvers)
                .options(options)
                .scalars(definitionFactory.getScalars())
                .build();
    }

}
