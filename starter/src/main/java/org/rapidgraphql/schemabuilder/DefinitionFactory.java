package org.rapidgraphql.schemabuilder;

import graphql.VisibleForTesting;
import graphql.kickstart.tools.*;
import graphql.language.Type;
import graphql.language.*;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rapidgraphql.annotations.GraphQLIgnore;
import org.rapidgraphql.annotations.GraphQLInputType;
import org.rapidgraphql.directives.SecuredDirectiveWiring;
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
    private final Map<String, TypeKind> discoveredTypes = new HashMap<>();
    private final Queue<DiscoveredClass> discoveredTypesQueue = new ArrayDeque<>();
    private final Set<String> definedClasses = new HashSet<>();
    private final Map<TypeKind, Function<DiscoveredClass, Definition<?>>> definitionFactory = new HashMap<>();
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
        Class<?> resolverType = ClassUtils.getUserClass(resolver);
        boolean isSubscription = false;
        if (resolver instanceof GraphQLQueryResolver) {
            name = QUERY_TYPE;
        } else if(resolver instanceof GraphQLMutationResolver) {
            name = MUTATION_TYPE;
        } else if(resolver instanceof GraphQLSubscriptionResolver) {
            name = SUBSCRIPTION_TYPE;
            isSubscription = true;
        } else {
            Optional<DiscoveredClass> discoveredClass = extractResolverType(resolver);
            if (discoveredClass.isEmpty()) {
                throw new IllegalStateException("Invalid resolver provided " + resolverType.getName());
            }
            discoverType(discoveredClass.get(), TypeKind.OUTPUT_TYPE);
            name = discoveredClass.get().getName();
            sourceType = discoveredClass.get().getClazz();
        }
        LOGGER.info("Processing {} resolver: {}", name, resolverType.getName());

        final Class<?> finalSourceType = sourceType;
        boolean finalIsSubscription = isSubscription;
        Method[] resolverDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(resolverType,
                method -> resolverMethodFilter(finalSourceType, method, finalIsSubscription));
        List<FieldDefinition> typeFields = Arrays.stream(resolverDeclaredMethods)
                .map(method -> createFieldDefinition(method, skipFirstParameter))
                .collect(Collectors.toList());
        return createTypeDefinition(name, typeFields);
    }

    public List<Definition<?>> processTypesQueue() {
        List<Definition<?>> definitions = new ArrayList<>();
        while (!discoveredTypesQueue.isEmpty()) {
            DiscoveredClass discoveredClass = discoveredTypesQueue.remove();
            LOGGER.info("Begin processing {} {} as {}", discoveredClass.getTypeKind(), discoveredClass.getClazz().getName(), discoveredClass.getName());
            definitions.add(definitionFactory.get(discoveredClass.getTypeKind()).apply(discoveredClass));
            LOGGER.info("End processing {} {} as {}", discoveredClass.getTypeKind(), discoveredClass.getClazz().getName(), discoveredClass.getName());
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
        Method[] declaredMethods = getTypeMethods(discoveredClass);
        Set<String> ignoredFields = getOutputTypeIgnoredFields(discoveredClass);
        List<FieldDefinition> typeFields = Arrays.stream(declaredMethods)
                .map(method -> createFieldDefinition(method, false))
                .filter(fieldDefinition -> !ignoredFields.contains(fieldDefinition.getName()))
                .collect(Collectors.toList());
        return createTypeDefinition(discoveredClass.getName(), typeFields);
    }

    private Set<String> getOutputTypeIgnoredFields(DiscoveredClass discoveredClass) {
        Set<String> fieldsToIgnore = new HashSet<>();
        ReflectionUtils.doWithFields(discoveredClass.getClazz(),
                field -> fieldsToIgnore.add(field.getName()),
                field -> field.isAnnotationPresent(GraphQLIgnore.class));
        return fieldsToIgnore;
    }

    private Definition<?> createInputTypeDefinition(DiscoveredClass discoveredClass) {
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
        return definitionBuilder.build();
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

    @VisibleForTesting
    public Optional<DiscoveredClass> extractResolverType(GraphQLResolver<?> graphQLResolver) {
        return extractResolverTypeFromClass(
                ClassUtils.getUserClass(graphQLResolver.getClass()));
    }

    private Optional<DiscoveredClass> extractResolverTypeFromClass(Class<?> resolverClass) {
        Optional<AnnotatedType> resolverInterface = Arrays.stream(resolverClass.getAnnotatedInterfaces())
                .filter(annotatedType -> (annotatedType.getType() instanceof ParameterizedType) &&
                        ((ParameterizedType)annotatedType.getType()).getRawType().equals(GraphQLResolver.class))
                .findFirst();
        if (resolverInterface.isEmpty()) {
            Class<?> superclass = resolverClass.getSuperclass();
            return Optional.ofNullable(superclass).flatMap(this::extractResolverTypeFromClass);
        }
        AnnotatedParameterizedType annotatedParameterizedType = (AnnotatedParameterizedType) resolverInterface.get();
        Class<?> clazz = (Class<?>) annotatedParameterizedType.getAnnotatedActualTypeArguments()[0].getType();
        return Optional.of(DiscoveredClass.builder()
                .name(getTypeName(clazz, TypeKind.OUTPUT_TYPE))
                .clazz(clazz)
                .typeKind(TypeKind.OUTPUT_TYPE)
                .build());
    }

    @NonNull
    private String getTypeName(Class<?> clazz, TypeKind typeKind) {
        if (typeKind == TypeKind.INPUT_TYPE) {
            GraphQLInputType annotation = clazz.getAnnotation(GraphQLInputType.class);
            if (annotation != null) {
                return annotation.value();
            }
        }
        return clazz.getSimpleName();
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
