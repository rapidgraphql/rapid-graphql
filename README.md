# Rapid GraphQL
Rapid GraphQL is a Java framework for rapid development of graphql service in the so called code-first approach.
In this approach GraphQL schema is derived automatically from the code. 

Most other Java frameworks choose schema-first approach in which first schema is designed first and then code should be written to match the schema.
Schema-first approach, although being more declarative, complicates development by requiring constant synchronization between the schema and code.
Code generation is used by some frameworks to simplify this synchronization, but it introduces other issues.

## Fast start guide

Rapid GraphQL framework is based on the spring boot framework and makes use of graphql-spring-boot-starter (this may change in future).
It doesn't require graphql schema files, and it automatically creates graphql endpoint (and graphiql if needed)

### Hello World
As in every spring application create an GraphQLApplication.java class:
```java
@SpringBootApplication
public class GraphQLApplication {

	public static void main(String[] args) {
		SpringApplication.run(GraphQLApplication.class, args);
	}

}

```

Then create HelloWorldQuery.java
```java
@Component
public class HelloWorldQuery implements GraphQLQueryResolver {
    public String helloWorld() {
        return "Hello World!!";
    }
}
```

To have graphiql running please add following configuration to the ``application.properties`` file
```properties
graphql.graphiql.enabled=true
```

In ``pom.xml`` you should add following dependencies:
```xml
    <dependencies>
        ...
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.7.15</version>
        </dependency>
        <dependency>
            <groupId>io.github.rapidgraphql</groupId>
            <artifactId>rapid-graphql-starter</artifactId>
            <version>0.0.8-SNAPSHOT</version>
        </dependency>

        <dependency> <!-- Recommended -->
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
```

That's it, hello world graphql application is ready.
You can go to http://localhost:8080/graphiql

## Supported Concepts
### Queries & Types
Schema is automatically generated from multiple Query Resolvers -> Query type, and Types and Type Resolvers, e.g:

```java
import jdk.jfr.DataAmount;

@Service
class Query1 implements GraphQLQueryResolver {
    public String helloWorld() {
        return "Hello World!!";
    }
}

@Data
class User {
    String name;
}

@Service
class Query2 implements GraphQLQueryResolver {
    public User hello(String name) {
        return new User(name);
    }
}

@Service
class UserResolver implements GraphQLResolver<User> {
    public String greeting(User user) {
        return "Mr. " + user.getName();
    }
}
```

Following schema would be generated:
```
type Query {
    helloWorld: String
    hello(name: String): User
}

type User {
    name: String
    greeting: String
}
```

### Mutations and input types
Input types are detected automatically if type appears as function parameter.

```java
@Data
class UserInput {
    String name;
}
@Service
class MyMutation implements GraphQLQueryResolver {
    User addUser(UserInput user) {
        return new User(user.getName());
    }
}
```
Generates following schema:
```
input UserInput {
    name: String
}
type Mutation {
    addUser(user: UserInput): User
}
```
### Subscriptions

```java
@Service
class MySubscription implements GraphQLSubscriptionResolver {

    public Publisher<Integer> hello(DataFetchingEnvironment env) {
        return Flux.range(0, 100)
                .delayElements(Duration.ofSeconds(1))
                .map(this::fun);
    }
    private Integer fun(Integer i) {
        Integer result = i*10;
        log.info("result={}", result);
        return result;
    }
}
```

### Not Null & Deprecated
```java
@Service
class MyQuery implements GraphQLQueryResolver {
    @GraphQLDeprecated("Use hi instead")
    public @NotNull String hello(@NotNull String name) {
        return "hello " + name;
    }
}
```

### Interfaces
```java
@GraphQLInterface
public class FilmCharacter {
    private String name;
}

@GraphQLImplementation
class Human extends FilmCharacter{
    Float height;
}

@Service
class MyQuery implements GraphQLQueryResolver {
    public @NotNull List<@NotNull FilmCharacter> characters(@NotNull String name) {
        return List.of(new Human());
    }
}

```

### Data Loaders
