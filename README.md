# Tower Messaging

A Java command messaging framework with built-in support for CDI and Quarkus.

## Features

- **Message Bus** - Dispatch commands through a unified interface
- **CDI Integration** - Works with Jakarta EE CDI contexts (Jakarta EE 10+)
- **Quarkus Extension** - First-class support for Quarkus applications
- **Schema Generation** - Build-time schema generation using Jandex indexing
- **Type-Safe** - Strongly typed message handling with compile-time safety

## Requirements

- Java 21+
- Gradle 9.0+ (wrapper included)

## Quick Start

### 1. Add Dependency

For plain CDI applications:

```kotlin
dependencies {
    implementation("io.iamcyw.tower:messaging-cdi:0.1.0")
}
```

For Quarkus applications:

```kotlin
dependencies {
    implementation("io.iamcyw.tower:tower-quarkus:0.1.0")
}
```

### 2. Define Message Handlers

```java
@UseCase
public class UserUseCase {

    @CommandHandle
    public User createUser(@Parameter("name") String name,
                           @Parameter("email") String email) {
        // Create and return new user
        return userRepository.save(new User(name, email));
    }
}
```

### 3. Dispatch Messages

```java
@Inject
MessageGateway gateway;

// Command
gateway.send(new CreateUserCommand("John", "john@example.com"));
```

## Project Structure

```
├── common/                    # Shared utilities
├── messaging-core/            # Core messaging framework
├── messaging-cdi/             # CDI integration (Jakarta EE 10+)
├── messaging-dependencies/    # BOM for dependency management
├── schema/
│   ├── schema-model/          # Schema model classes
│   └── schema-builder/        # Schema builder using Jandex
└── support/
    └── quarkus/               # Quarkus extension
        ├── tower-quarkus/           # Runtime
        ├── tower-quarkus-deployment/# Build-time processing
        └── integration-tests/       # (temporarily disabled)
```

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

## Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@UseCase` | Marks a class containing message handlers |
| `@CommandHandle` | Marks a method as a command handler |
| `@CommandName` | Explicitly specifies a command's name for lookup |
| `@Predicate` | Conditional message handling |
| `@Parameter` | Maps method parameters to message fields |

### Command Names

Commands can be looked up by name using `HandlerRegistry.getCommandClass(String)`. Command names are resolved using these rules:

1. If the class has a `@CommandName` annotation, use its value
2. If the class name ends with "Command", remove that suffix
3. Otherwise, use the simple class name

```java
// Explicit name via annotation
@CommandName("createOrder")
public class CreateOrderCommand implements Command { }

// Auto-derived name: "UpdateOrder"
public class UpdateOrderCommand implements Command { }

// Lookup by name
Class<? extends Command> clazz = registry.getCommandClass("createOrder");
```

## Architecture

Messages flow through the system as follows:

1. **Dispatch** - `MessageBus.dispatch(Message<R>)` receives the message
2. **Route** - `DefaultMessageBus.route()` resolves the appropriate `MessageHandle`
3. **Intercept** - `DefaultInterceptorChain` processes registered interceptors
4. **Invoke** - `MessageHandle.handle()` calls the method via `OperationInvoker`

## Migration Notes

### Jakarta EE 10 Migration

This project uses Jakarta EE 10 with the `jakarta.*` namespace. Ensure your dependencies are updated:

- `javax.enterprise.*` → `jakarta.enterprise.*`
- `javax.inject.*` → `jakarta.inject.*`

### Quarkus 3.21+

Quarkus 3.x uses the Jakarta namespace and requires compatible dependencies.

## License

[License information to be added]
