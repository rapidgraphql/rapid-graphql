package org.rapidgraphql.schemabuilder;

import graphql.kickstart.tools.*;
import graphql.language.Type;
import graphql.language.*;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.rapidgraphql.annotations.GraphQLIgnore;
import org.rapidgraphql.annotations.GraphQLInputType;
import org.rapidgraphql.annotations.GraphQLInterface;
import org.rapidgraphql.directives.SecuredDirectiveWiring;
import org.rapidgraphql.exceptions.GraphQLSchemaGenerationException;
import org.rapidgraphql.scalars.TimestampScalar;
import org.slf4j.Logger;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.rapidgraphql.schemabuilder.MethodsFilter.*;
import static org.rapidgraphql.schemabuilder.ResolverTypeExtractor.extractResolverType;
import static org.rapidgraphql.schemabuilder.TypeUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

public class DefinitionFactory {

    private static final Logger LOGGER = getLogger(DefinitionFactory.class);
    public static final String QUERY_TYPE = "Query";
    public static final String MUTATION_TYPE = "Mutation";
    public static final String SUBSCRIPTION_TYPE = "Subscription";
    private static final List<GraphQLScalarType> scalars = List.of(
            ExtendedScalars.GraphQLLong,
            ExtendedScalars.Date,
            ExtendedScalars.DateTime,
            TimestampScalar.INSTANCE
    );
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
            entry(Short.TYPE, nonNullType(ExtendedScalars.GraphQLShort.getName())),
            entry(Short.class, nullableType(ExtendedScalars.GraphQLShort.getName())),
            entry(Long.TYPE, nonNullType(ExtendedScalars.GraphQLLong.getName())),
            entry(Long.class, nullableType(ExtendedScalars.GraphQLLong.getName())),
            entry(Byte.TYPE, nonNullType(ExtendedScalars.GraphQLByte.getName())),
            entry(Byte.class, nullableType(ExtendedScalars.GraphQLByte.getName())),
            entry(Character.TYPE, nonNullType(ExtendedScalars.GraphQLChar.getName())),
            entry(Character.class, nullableType(ExtendedScalars.GraphQLChar.getName())),
            entry(BigDecimal.class, nullableType(ExtendedScalars.GraphQLBigDecimal.getName())),
            entry(BigInteger.class, nullableType(ExtendedScalars.GraphQLBigInteger.getName())),
            entry(LocalDate.class, nullableType(ExtendedScalars.Date.getName())),
            entry(OffsetDateTime.class, nullableType(ExtendedScalars.DateTime.getName())),
            entry(java.sql.Timestamp.class, nullableType(TimestampScalar.INSTANCE.getName()))
    );
    private final Map<String, DiscoveredClass> discoveredTypes = new HashMap<>();
    private final Queue<DiscoveredClass> discoveredTypesQueue = new ArrayDeque<>();
    private final Set<String> definedClasses = new HashSet<>();
    private final Map<TypeKind, Function<DiscoveredClass, Stream<Definition<?>>>> definitionFactory = new HashMap<>();
    private static final Set<Class<?>> WRAPPER_CLASSES = Set.of(Optional.class, Future.class, CompletableFuture.class);
    private final DefaultValueAnnotationProcessor defaultValueAnnotationProcessor;
    private final Map<String, Class<?>> implementationDictionary = new HashMap<>();
    private Map<String, String> interfacesCreatedFromResolvers = new HashMap<>();

    public DefinitionFactory(DefaultValueAnnotationProcessor defaultValueAnnotationProcessor) {
        this.defaultValueAnnotationProcessor = defaultValueAnnotationProcessor;
        definitionFactory.put(TypeKind.OUTPUT_TYPE, this::createOutputTypeDefinition);
        definitionFactory.put(TypeKind.INPUT_TYPE, this::createInputTypeDefinition);
        definitionFactory.put(TypeKind.ENUM_TYPE, this::createEnumTypeDefinition);
        definitionFactory.put(TypeKind.INTERFACE_TYPE, this::createInterfaceTypeDefinition);
    }

    public List<GraphQLScalarType> getScalars() {
        return scalars;
    }

    public Map<String, Class<?>> getImplementationDictionary() {
        return implementationDictionary;
    }

    static private Type<?> nullableType(String typeName) {
        return new TypeName(typeName);
    }

    static private Type<?> nonNullType(String typeName) {
        return new NonNullType(nullableType(typeName));
    }

    public Stream<Definition<?>> createTypeDefinition(GraphQLResolver<?> resolver) {
        boolean upperLevelResolver = resolver instanceof GraphQLQueryResolver || resolver instanceof GraphQLMutationResolver;
        boolean skipFirstParameter = !upperLevelResolver;
        String name;
        Class<?> sourceType = null;
        Class<?> resolverType = ClassUtils.getUserClass(resolver);
        TypeKind typeKind = TypeKind.OUTPUT_TYPE;
        String implementsInterface = null;
        boolean isSubscription = false;
        Optional<DiscoveredClass> discoveredClass = Optional.empty();
        if (resolver instanceof GraphQLQueryResolver) {
            name = QUERY_TYPE;
        } else if(resolver instanceof GraphQLMutationResolver) {
            name = MUTATION_TYPE;
        } else if(resolver instanceof GraphQLSubscriptionResolver) {
            name = SUBSCRIPTION_TYPE;
            isSubscription = true;
        } else {
            discoveredClass = extractResolverType(resolver);
            if (discoveredClass.isEmpty()) {
                throw new GraphQLSchemaGenerationException("Invalid resolver provided " + resolverType.getName());
            }
            discoverType(discoveredClass.get(), typeKind);
            name = discoveredClass.get().getName();
            sourceType = discoveredClass.get().getClazz();
            typeKind = discoveredClass.get().getTypeKind();
            implementsInterface = discoveredClass.get().getImplementsInterface();
        }
        LOGGER.info("Processing {} resolver: {}", name, resolverType.getName());

        final Class<?> finalSourceType = sourceType;
        boolean finalIsSubscription = isSubscription;
        Method[] resolverDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(resolverType,
                method -> resolverMethodFilter(finalSourceType, method, finalIsSubscription));
        List<FieldDefinition> typeFields = Arrays.stream(resolverDeclaredMethods)
                .map(method -> createFieldDefinition(method, skipFirstParameter))
                .collect(Collectors.toList());
        if (typeKind == TypeKind.OUTPUT_TYPE) {
            return createTypeDefinition(name, typeFields, implementsInterface);
        } else {
            if (interfacesCreatedFromResolvers.containsKey(name)) {
                throw new GraphQLSchemaGenerationException(
                        String.format("Multimple resolvers are not allowed for interface: %s already has resolver %s, new resolver discovered %s",
                                name, interfacesCreatedFromResolvers.get(name), resolverType.getName()));
            }
            interfacesCreatedFromResolvers.put(name, resolverType.getName());
            discoveredClass.ifPresent(dClass -> typeFields.addAll(getFieldDefinitions(dClass)));
            return createInterfaceTypeDefinition(name, sourceType, typeFields, false);
        }
    }

    public List<Definition<?>> processTypesQueue() {
        List<Definition<?>> definitions = new ArrayList<>();
        while (!discoveredTypesQueue.isEmpty()) {
            DiscoveredClass discoveredClass = discoveredTypesQueue.remove();
            LOGGER.info("Begin processing {} {} as {}", discoveredClass.getTypeKind(), discoveredClass.getClazz().getName(), discoveredClass.getName());
            definitions.addAll(definitionFactory.get(discoveredClass.getTypeKind()).apply(discoveredClass).collect(Collectors.toList()));
            LOGGER.info("End processing {} {} as {}", discoveredClass.getTypeKind(), discoveredClass.getClazz().getName(), discoveredClass.getName());
        }
        return definitions;
    }
    private Stream<Definition<?>> createTypeDefinition(String name, List<FieldDefinition> typeFields, String implementsInterface) {
        Type implementsType = Optional.ofNullable(implementsInterface)
                .map(interfaceName -> TypeName.newTypeName(interfaceName).build())
                .orElse(null);
        TypeName.newTypeName().name(implementsInterface).build();
        if (definedClasses.contains(name)) {
            ObjectTypeExtensionDefinition.Builder builder = ObjectTypeExtensionDefinition.newObjectTypeExtensionDefinition()
                    .name(name)
                    .fieldDefinitions(typeFields);
            if (implementsType != null) {
                builder = builder.implementz(implementsType);
            }
            return Stream.of(builder.build());
        } else {
            definedClasses.add(name);
            ObjectTypeDefinition.Builder builder = ObjectTypeDefinition.newObjectTypeDefinition()
                    .name(name)
                    .fieldDefinitions(typeFields);
            if (implementsType != null) {
                builder = builder.implementz(implementsType);
            }
            return Stream.of(builder.build());
        }
    }

    public Stream<Definition<?>> createOutputTypeDefinition(DiscoveredClass discoveredClass) {
        List<FieldDefinition> typeFields = getFieldDefinitions(discoveredClass);
        return createTypeDefinition(discoveredClass.getName(), typeFields, discoveredClass.getImplementsInterface());
    }

    public Stream<Definition<?>> createInterfaceTypeDefinition(DiscoveredClass discoveredClass) {
        if (interfacesCreatedFromResolvers.containsKey(discoveredClass.getName())) {
            // skip since Interface resolver creates full interface type including type fields
            return Stream.empty();
        }
        List<FieldDefinition> typeFields = getFieldDefinitions(discoveredClass);
        GraphQLInterface graphQLInterface = discoveredClass.getClazz().getAnnotation(GraphQLInterface.class);
        if (graphQLInterface == null) {
            throw new GraphQLSchemaGenerationException("interface should be marked with @GraphQLInterface annotation");
        }
        return createInterfaceTypeDefinition(discoveredClass.getName(), discoveredClass.getClazz(), typeFields, false);
    }

    private void discoverImplementations(DiscoveredClass discoveredInterfaceClass) {
        if (discoveredInterfaceClass.getTypeKind() != TypeKind.INTERFACE_TYPE) {
            return;
        }
        List<Class<?>> implementations = InterfaceUtils.findImplementations(discoveredInterfaceClass.getClazz());
        implementations.stream()
                .map(implementation -> DiscoveredClass.builder()
                        .name(implementation.getSimpleName())
                        .clazz(implementation)
                        .typeKind(TypeKind.OUTPUT_TYPE)
                        .implementsInterface(discoveredInterfaceClass.getName())
                        .build())
                .forEach(discovered -> {
                    implementationDictionary.put(discovered.getName(), discovered.getClazz());
                    discoverType(discovered, TypeKind.OUTPUT_TYPE);
                });
    }

    @NotNull
    private Stream<Definition<?>> createInterfaceTypeDefinition(String name, Class<?> clazz,
                                                                  List<FieldDefinition> typeFields,
                                                                  boolean createImplementations) {
        Stream<Definition<?>> definitionsStream = Stream.of();
        if (definedClasses.contains(name)) {
            throw new GraphQLSchemaGenerationException("Extending of interface " + name + " is not supported");
        } else {
            definedClasses.add(name);
            definitionsStream = Stream.of(InterfaceTypeDefinition.newInterfaceTypeDefinition()
                    .name(name)
                    .definitions(typeFields)
                    .build());
        }
        if (createImplementations) {
            List<Class<?>> implementations = InterfaceUtils.findImplementations(clazz);
            definitionsStream = Stream.concat(definitionsStream,
                    implementations.stream()
                            .flatMap(implementation -> createTypeDefinition(implementation.getSimpleName(), typeFields, name))
            );
        }
        return definitionsStream;
    }

    @NotNull
    private List<FieldDefinition> getFieldDefinitions(DiscoveredClass discoveredClass) {
        Method[] declaredMethods = getTypeMethods(discoveredClass);
        Set<String> ignoredFields = getOutputTypeIgnoredFields(discoveredClass);
        List<FieldDefinition> typeFields = Arrays.stream(declaredMethods)
                .filter(method -> !ignoredFields.contains(normalizeGetName(method.getName())))
                .map(method -> createFieldDefinition(method, false))
                .collect(Collectors.toList());
        return typeFields;
    }

    private Set<String> getOutputTypeIgnoredFields(DiscoveredClass discoveredClass) {
        Set<String> fieldsToIgnore = new HashSet<>();
        ReflectionUtils.doWithFields(discoveredClass.getClazz(),
                field -> fieldsToIgnore.add(field.getName()),
                field -> field.isAnnotationPresent(GraphQLIgnore.class));
        return fieldsToIgnore;
    }

    private Stream<Definition<?>> createInputTypeDefinition(DiscoveredClass discoveredClass) {
        InputObjectTypeDefinition.Builder definitionBuilder = InputObjectTypeDefinition.newInputObjectDefinition();
        definitionBuilder.name(discoveredClass.getName());
        Method[] declaredMethods = getInputTypeMethods(discoveredClass);
        if (declaredMethods.length == 0) {
            throw new SchemaError(format("No fields were discovered for input type %s", discoveredClass.getClazz().getName()), null);
        }
        Set<String> ignoredFields = getInputTypeIgnoredFields(discoveredClass);
        List<InputValueDefinition> inputDefinitions = Arrays.stream(declaredMethods)
                .filter(method -> !ignoredFields.contains(method.getName()))
                .map(this::createInputValueDefinition)
                .collect(Collectors.toList());
        definitionBuilder.inputValueDefinitions(inputDefinitions);
        return Stream.of(definitionBuilder.build());
    }

    private Set<String> getInputTypeIgnoredFields(DiscoveredClass discoveredClass) {
        GraphQLInputType annotation = discoveredClass.getClazz().getAnnotation(GraphQLInputType.class);
        if (annotation == null) {
            return Set.of();
        }
        return Set.of(annotation.ignore());
    }

    private InputValueDefinition createInputValueDefinition(Method method) {
        return InputValueDefinition.newInputValueDefinition()
                .name(normalizeSetName(method.getName()).get())
                .type(convertToGraphQLType(method.getAnnotatedParameterTypes()[0], TypeKind.INPUT_TYPE))
                .build();
    }

    private Stream<Definition<?>> createEnumTypeDefinition(DiscoveredClass discoveredClass) {
        List<EnumValueDefinition> enumValueDefinitions = getEnumValueDefinitions(discoveredClass.getClazz());
        EnumTypeDefinition definition = EnumTypeDefinition.newEnumTypeDefinition()
                .name(discoveredClass.getName())
                .enumValueDefinitions(enumValueDefinitions)
                .build();
        return Stream.of(definition);
    }

    private List<EnumValueDefinition> getEnumValueDefinitions(Class<?> clazz) {
        return Arrays.stream(clazz.getEnumConstants()).map(Object::toString)
                .map(name -> EnumValueDefinition.newEnumValueDefinition().name(name).build())
                .collect(Collectors.toList());
    }

    private void discoverType(DiscoveredClass discoveredClass, TypeKind typeKind) {
        if (discoveredClass.getClazz().isInterface() && typeKind==TypeKind.INPUT_TYPE) {
            throw new GraphQLSchemaGenerationException("Input type can't be interface: " + discoveredClass.getClazz().getName());
        } else if (discoveredClass.getClazz().isEnum()) {
            discoveredClass.setTypeKind(TypeKind.ENUM_TYPE);
        } else if (typeKind==TypeKind.INTERFACE_TYPE || discoveredClass.getClazz().isAnnotationPresent(GraphQLInterface.class)) {
            discoveredClass.setTypeKind(TypeKind.INTERFACE_TYPE);
        } else {
            discoveredClass.setTypeKind(typeKind);
        }
        DiscoveredClass alreadyDiscovered = discoveredTypes.get(discoveredClass.getName());
        if (alreadyDiscovered == null) {
            LOGGER.info("Discovered new type {} of kind {}", discoveredClass.getName(), discoveredClass.getTypeKind());
            discoveredTypes.put(discoveredClass.getName(), discoveredClass);
            discoverInterface(discoveredClass);
            discoveredTypesQueue.add(discoveredClass);
            discoverImplementations(discoveredClass);
        } else if (alreadyDiscovered.getTypeKind() != discoveredClass.getTypeKind()) {
            throw new GraphQLSchemaGenerationException(format("Type %s was already discovered with different type kind %s (previous) != %s (new)",
                    discoveredClass.getName(), alreadyDiscovered.getTypeKind(), discoveredClass.getTypeKind()));
        } else {
            discoveredClass.setImplementsInterface(alreadyDiscovered.getImplementsInterface());
        }
    }

    private void discoverInterface(DiscoveredClass discoveredClass) {
        Optional<Class<?>> interfaceType = InterfaceUtils.getGraphQLInterface(discoveredClass.getClazz());
        discoveredClass.setImplementsInterface(interfaceType.map(Class::getSimpleName).orElse(null));
        interfaceType.map(type->DiscoveredClass.builder()
                            .name(type.getSimpleName())
                            .clazz(type)
                            .build())
                        .ifPresent(discovered -> discoverType(discovered, TypeKind.INTERFACE_TYPE));
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

    @NonNull
    private Type<?> convertToOutputGraphQLType(AnnotatedType annotatedType) {
        Optional<AnnotatedParameterizedType> parameterizedType = castToParameterizedType(annotatedType);
        if (parameterizedType.isPresent() && WRAPPER_CLASSES.contains(baseType(parameterizedType.get()))) {
            annotatedType = actualTypeArgument(parameterizedType.get(), 0);
        }
        return convertToGraphQLType(annotatedType, TypeKind.OUTPUT_TYPE);
    }

    @NonNull
    private Type<?> convertToInputGraphQLType(AnnotatedType annotatedType) {
        return convertToGraphQLType(annotatedType, TypeKind.INPUT_TYPE);
    }

    private Type<?> convertToGraphQLType(AnnotatedType annotatedType, TypeKind typeKind) {
        Optional<AnnotatedParameterizedType> parameterizedType = castToParameterizedType(annotatedType);
        Type<?> graphqlType;
        if (typeKind == TypeKind.OUTPUT_TYPE && parameterizedType.isPresent() && isPublisherType(parameterizedType.get())) {
            AnnotatedType typeOfParameter = actualTypeArgument(parameterizedType.get(), 0);
            graphqlType = convertToGraphQLType(typeOfParameter, typeKind);
        } else if (parameterizedType.isPresent() && isListType(parameterizedType.get())) {
            AnnotatedType typeOfParameter = actualTypeArgument(parameterizedType.get(), 0);
            graphqlType = new ListType(convertToGraphQLType(typeOfParameter, typeKind));
        } else {
            java.lang.reflect.Type type = annotatedType.getType();
            graphqlType = scalarTypes.get(type);
            if (graphqlType == null) {
                Optional<DiscoveredClass> simpleClass = tryGetSimpleClass(type, typeKind);
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

    private Optional<DiscoveredClass> tryGetSimpleClass(java.lang.reflect.Type type, TypeKind typeKind) {
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
        return Optional.of(DiscoveredClass.builder().name(getTypeName(clazz, typeKind)).clazz(clazz).build());
    }

    public Definition<?> createRoleDirectiveDefinition() {
        return DirectiveDefinition.newDirectiveDefinition()
                .name(SecuredDirectiveWiring.DIRECTIVE_NAME)
                .inputValueDefinition(
                        new InputValueDefinition(SecuredDirectiveWiring.DIRECTIVE_ARGUMENT_NAME, new ListType(nonNullType("String"))))
                .directiveLocation(new DirectiveLocation("FIELD_DEFINITION"))
                .build();
    }
}
