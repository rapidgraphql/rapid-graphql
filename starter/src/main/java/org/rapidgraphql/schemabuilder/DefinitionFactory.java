package org.rapidgraphql.schemabuilder;

import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.kickstart.tools.SchemaError;
import graphql.language.Definition;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ObjectTypeExtensionDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.rapidgraphql.schemabuilder.MethodsFilter.normalizeGetName;
import static org.rapidgraphql.schemabuilder.MethodsFilter.normalizeSetName;
import static org.rapidgraphql.schemabuilder.MethodsFilter.resolverMethodFilter;
import static org.rapidgraphql.schemabuilder.TypeUtils.actualTypeArgument;
import static org.rapidgraphql.schemabuilder.TypeUtils.baseType;
import static org.rapidgraphql.schemabuilder.TypeUtils.castToParameterizedType;
import static org.rapidgraphql.schemabuilder.TypeUtils.isListType;
import static org.rapidgraphql.schemabuilder.TypeUtils.isNotNullable;
import static org.slf4j.LoggerFactory.getLogger;

public class DefinitionFactory {

    private static final Logger LOGGER = getLogger(DefinitionFactory.class);
    public static final String QUERY_TYPE = "Query";
    public static final String MUTATION_TYPE = "Mutation";
    private static final List<GraphQLScalarType> scalars = List.of(ExtendedScalars.GraphQLLong);
    private static final Map<java.lang.reflect.Type, Type<?>> scalarTypes = Map.ofEntries(
            entry(Integer.TYPE, nonNullType("Int")),
            entry(Integer.class, nullableType("Int")),
            entry(String.class, nullableType("String")),
            entry(Boolean.TYPE, nonNullType("Boolean")),
            entry(Boolean.class, nullableType("Boolean")),
            entry(Float.TYPE, nonNullType("Float")),
            entry(Float.class, nullableType("Float")),
            entry(Double.TYPE, nonNullType("Float")),
            entry(Double.class, nullableType("Float")),
            entry(Long.TYPE, nullableType(ExtendedScalars.GraphQLLong.getName())),
            entry(Long.class, nullableType(ExtendedScalars.GraphQLLong.getName()))
    );
    private Map<String, TypeKind> discoveredTypes = new HashMap<>();
    private Queue<DiscoveredClass> discoveredTypesQueue = new ArrayDeque<>();
    private Set<String> definedClasses = new HashSet<>();
    private Map<TypeKind, Function<DiscoveredClass, Definition<?>>> definitionFactory = new HashMap<>();
    private static final Set<Class<?>> WRAPPER_CLASSES = Set.of(Optional.class, Future.class, CompletableFuture.class);
    private final DefaultValueAnnotationProcessor defaultValueAnnotationProcessor;

    public DefinitionFactory(DefaultValueAnnotationProcessor defaultValueAnnotationProcessor) {
        this.defaultValueAnnotationProcessor = defaultValueAnnotationProcessor;
        definitionFactory.put(TypeKind.OUTPUT_TYPE, this::createTypeDefinition);
        definitionFactory.put(TypeKind.INPUT_TYPE, this::createInputTypeDefinition);
        definitionFactory.put(TypeKind.ENUM_TYPE, this::createEnumTypeDefinition);
    }

    public List<GraphQLScalarType> getScalars() {
        return scalars;
    }

    static private Type<?> nullableType(String typeName) {
        return new TypeName(typeName);
    }

    static private Type<?> nonNullType(String typeName) {
        return new NonNullType(nullableType(typeName));
    }

    public Definition<?> createTypeDefinition(GraphQLResolver<?> resolver) {
        boolean upperLevelResolver = resolver instanceof GraphQLQueryResolver || resolver instanceof GraphQLMutationResolver;
        boolean skipFirstParameter = !upperLevelResolver;
        String name;
        Class<?> sourceType = null;
        if (resolver instanceof GraphQLQueryResolver) {
            name = QUERY_TYPE;
        } else if(resolver instanceof GraphQLMutationResolver) {
            name = MUTATION_TYPE;
        } else {
            Optional<DiscoveredClass> discoveredClass = extractResolverType(resolver);
            if (discoveredClass.isEmpty()) {
                throw new IllegalStateException("Invalid resolver provided " + resolver.getClass().getName());
            }
            discoverType(discoveredClass.get(), TypeKind.OUTPUT_TYPE);
            name = discoveredClass.get().getName();
            sourceType = discoveredClass.get().getClazz();
        }

        final Class<?> finalSourceType = sourceType;
        Method[] resolverDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(resolver.getClass(),
                method -> resolverMethodFilter(finalSourceType, method));
        List<FieldDefinition> typeFields = Arrays.stream(resolverDeclaredMethods)
                .map(method -> createFieldDefinition(method, skipFirstParameter))
                .collect(Collectors.toList());
        return createTypeDefinition(name, typeFields);
    }

    public List<Definition<?>> processTypesQueue() {
        List<Definition<?>> definitions = new ArrayList<>();
        while (!discoveredTypesQueue.isEmpty()) {
            DiscoveredClass discoveredClass = discoveredTypesQueue.remove();
            definitions.add(definitionFactory.get(discoveredClass.getTypeKind()).apply(discoveredClass));
        }
        return definitions;
    }
    private Definition<?> createTypeDefinition(String name, List<FieldDefinition> typeFields) {
        if (definedClasses.contains(name)) {
            return ObjectTypeExtensionDefinition.newObjectTypeExtensionDefinition()
                    .name(name)
                    .fieldDefinitions(typeFields)
                    .build();
        } else {
            definedClasses.add(name);
            return ObjectTypeDefinition.newObjectTypeDefinition()
                    .name(name)
                    .fieldDefinitions(typeFields)
                    .build();
        }
    }

    public Definition<?> createTypeDefinition(DiscoveredClass discoveredClass) {
        Method[] declaredMethods = ReflectionUtils.getUniqueDeclaredMethods(discoveredClass.getClazz(),
                MethodsFilter::typeMethodFilter);
        List<FieldDefinition> typeFields = Arrays.stream(declaredMethods)
                .map(method -> createFieldDefinition(method, false))
                .collect(Collectors.toList());
        return createTypeDefinition(discoveredClass.getName(), typeFields);
    }

    private Definition<?> createInputTypeDefinition(DiscoveredClass discoveredClass) {
        InputObjectTypeDefinition.Builder definitionBuilder = InputObjectTypeDefinition.newInputObjectDefinition();
        definitionBuilder.name(discoveredClass.getName());
        Method[] declaredMethods = ReflectionUtils.getUniqueDeclaredMethods(discoveredClass.getClazz(),
                MethodsFilter::inputTypeMethodFilter);
        if (declaredMethods.length == 0) {
            throw new SchemaError(format("No fields were discovered for input type %s", discoveredClass.getClazz().getName()), null);
        }
        List<InputValueDefinition> inputDefinitions = Arrays.stream(declaredMethods)
                .map(this::createInputValueDefinition)
                .collect(Collectors.toList());
        definitionBuilder.inputValueDefinitions(inputDefinitions);
        return definitionBuilder.build();
    }

    private InputValueDefinition createInputValueDefinition(Method method) {
        return InputValueDefinition.newInputValueDefinition()
                .name(normalizeSetName(method.getName()).get())
                .type(convertToGraphQLType(method.getAnnotatedParameterTypes()[0], TypeKind.INPUT_TYPE))
                .build();
    }

    private Definition<?> createEnumTypeDefinition(DiscoveredClass discoveredClass) {
        List<EnumValueDefinition> enumValueDefinitions = getEnumValueDefinitions(discoveredClass.getClazz());
        EnumTypeDefinition definition = EnumTypeDefinition.newEnumTypeDefinition()
                .name(discoveredClass.getName())
                .enumValueDefinitions(enumValueDefinitions)
                .build();
        return definition;
    }

    private List<EnumValueDefinition> getEnumValueDefinitions(Class<?> clazz) {
        return Arrays.stream(clazz.getEnumConstants()).map(Object::toString)
                .map(name -> EnumValueDefinition.newEnumValueDefinition().name(name).build())
                .collect(Collectors.toList());
    }

    private Optional<DiscoveredClass> extractResolverType(GraphQLResolver<?> graphQLResolver) {
        Optional<AnnotatedType> resolverInterface = Arrays.stream(graphQLResolver.getClass().getAnnotatedInterfaces())
                .filter(annotatedType -> (annotatedType.getType() instanceof ParameterizedType) &&
                        ((ParameterizedType)annotatedType.getType()).getRawType().equals(GraphQLResolver.class))
                .findFirst();
        if (resolverInterface.isEmpty()) {
            return Optional.empty();
        }
        AnnotatedParameterizedType annotatedParameterizedType = (AnnotatedParameterizedType) resolverInterface.get();
        Class<?> clazz = (Class<?>) annotatedParameterizedType.getAnnotatedActualTypeArguments()[0].getType();
        return Optional.of(DiscoveredClass.builder().name(clazz.getSimpleName()).clazz(clazz).build());
    }

    private void discoverType(DiscoveredClass discoveredClass, TypeKind typeKind) {
        if (discoveredClass.getClazz().isInterface()) {
            if (typeKind == TypeKind.INPUT_TYPE) {
                throw new SchemaError("Input type can't be interface: " + discoveredClass.getClazz().getName(), null);
            } else {
                throw new SchemaError("Interfaces are not yet supported: " + discoveredClass.getClazz().getName(), null);
            }
        } else if (discoveredClass.getClazz().isEnum()) {
            discoveredClass.setTypeKind(TypeKind.ENUM_TYPE);
        } else {
            discoveredClass.setTypeKind(typeKind);
        }
        TypeKind existingTypeKind = discoveredTypes.get(discoveredClass.getName());
        if (existingTypeKind == null) {
            LOGGER.info("Discovered new type {} of kind {}", discoveredClass.getName(), discoveredClass.getTypeKind());
            discoveredTypes.put(discoveredClass.getName(), discoveredClass.getTypeKind());
            discoveredTypesQueue.add(discoveredClass);
        } else if (existingTypeKind != discoveredClass.getTypeKind()) {
            throw new SchemaError(format("Type %s was already discovered with different type kind %s (previous) != %s (new)",
                    discoveredClass.getName(), existingTypeKind, discoveredClass.getTypeKind()), null);
        }

    }

    private FieldDefinition createFieldDefinition(Method method, boolean skipFirst) {
        Parameter[] parameters = method.getParameters();
        Stream<Parameter> parameterStream = Arrays.stream(parameters);
        if (parameters.length >= 1 && DataFetchingEnvironment.class.isAssignableFrom(parameters[parameters.length-1].getType())) {
            parameterStream = parameterStream.limit(parameters.length-1);
        }
        if (skipFirst) {
            parameterStream = parameterStream.skip(1);
        }
        List<InputValueDefinition> inputValueDefinitions = parameterStream
                .map(this::toInputValue)
                .collect(Collectors.toList());
        FieldDefinition.Builder builder = FieldDefinition.newFieldDefinition()
                .name(normalizeGetName(method.getName()))
                .type(convertToOutputGraphQLType(method.getAnnotatedReturnType()))
                .inputValueDefinitions(inputValueDefinitions);
        AnnotationProcessor.applyAnnotations(method, builder);
        return builder.build();
    }

    private InputValueDefinition toInputValue(Parameter parameter) {

        InputValueDefinition.Builder builder = InputValueDefinition.newInputValueDefinition()
                .name(parameter.getName())
                .type(convertToInputGraphQLType(parameter.getAnnotatedType()));
        defaultValueAnnotationProcessor.applyAnnotations(parameter, builder);
        return builder.build();
    }

    @NotNull
    private Type<?> convertToOutputGraphQLType(AnnotatedType annotatedType) {
        Optional<AnnotatedParameterizedType> parameterizedType = castToParameterizedType(annotatedType);
        if (parameterizedType.isPresent() && WRAPPER_CLASSES.contains(baseType(parameterizedType.get()))) {
            annotatedType = actualTypeArgument(parameterizedType.get(), 0);
        }
        return convertToGraphQLType(annotatedType, TypeKind.OUTPUT_TYPE);
    }

    @NotNull
    private Type<?> convertToInputGraphQLType(AnnotatedType annotatedType) {
        return convertToGraphQLType(annotatedType, TypeKind.INPUT_TYPE);
    }

    private Type<?> convertToGraphQLType(AnnotatedType annotatedType, TypeKind typeKind) {
        Optional<AnnotatedParameterizedType> parameterizedType = castToParameterizedType(annotatedType);
        Type<?> graphqlType;
        if (parameterizedType.isPresent() && isListType(parameterizedType.get())) {
            AnnotatedType typeOfParameter = actualTypeArgument(parameterizedType.get(), 0);
            graphqlType = new ListType(convertToGraphQLType(typeOfParameter, typeKind));
        } else {
            java.lang.reflect.Type type = annotatedType.getType();
            graphqlType = scalarTypes.get(type);
            if (graphqlType == null) {
                Optional<DiscoveredClass> simpleClass = tryGetSimpleClass(type);
                if (simpleClass.isEmpty()) {
                    throw new RuntimeException("Can't resolve type " + type.getTypeName());
                }
                discoverType(simpleClass.get(), typeKind);
                graphqlType = nullableType(simpleClass.get().getName());
            }
        }
        if (isNotNullable(annotatedType) && !(graphqlType instanceof NonNullType)) {
            return new NonNullType(graphqlType);
        }
        return graphqlType;
    }

    private Optional<DiscoveredClass> tryGetSimpleClass(java.lang.reflect.Type type) {
        if (type instanceof  ParameterizedType) {
            return Optional.empty();
        }
        if (!(type instanceof Class<?>)) {
            return Optional.empty();
        }
        Class<?> clazz = (Class<?>) type;
        if (Object.class.equals(clazz) || Map.class.isAssignableFrom(clazz) || Collection.class.isAssignableFrom(clazz)) {
            return Optional.empty();
        }
        return Optional.of(DiscoveredClass.builder().name(clazz.getSimpleName()).clazz(clazz).build());
    }


}
