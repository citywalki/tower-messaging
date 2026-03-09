# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Tower Messaging is a Java framework implementing command messaging patterns. It provides a type-safe message gateway for dispatching commands to handlers, with support for CDI and Quarkus integration.

The architecture is built around the `CommandHandler<C, R>` interface - handlers implement this interface to declare which command type they process and what result type they return. This provides compile-time type safety compared to annotation-based approaches.

## Build System

This is a Gradle multi-module project using Kotlin DSL for build scripts.

**Prerequisites:**
- Java 25+ (configured in `java-conventions.gradle.kts`)
- Gradle 9.0+ (via wrapper)

**Common Commands:**

```bash
# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :messaging-core:test

# Run a single test class
./gradlew :messaging-core:test --tests "io.iamcyw.tower.messaging.handle.CommandTest"

# Run a single test method
./gradlew :messaging-core:test --tests "io.iamcyw.tower.messaging.handle.CommandTest.shouldExecuteHandlerAndReturnResult"

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Clean build outputs
./gradlew clean

# Check JReleaser configuration (for releases)
./gradlew jreleaserConfig
```

**Note:** Tests require `--enable-preview` JVM argument for Java 25 preview features. This is configured in `java-conventions.gradle.kts`.

## Project Structure

The project is organized into the following modules:

### Core Modules

- **`common`** - Shared utilities (NullSafety, StringFormatter, TypeInspector, Preconditions, StringPool, FutureComposition, Deadlines, ClassNames, ListOperations)
- **`messaging-core`** - Core messaging framework with `MessageGateway`, `CommandHandler` interface, `HandlerRegistry`, and interceptor chain
- **`messaging-cdi`** - CDI integration with producers for `HandlerRegistry` and `MessageGateway`

### Quarkus Support

- **`support:quarkus:tower-quarkus`** - Quarkus runtime extension with `QuarkusHandlerRegistry`
- **`support:quarkus:tower-quarkus-deployment`** - Quarkus build-time deployment processor using Jandex for handler discovery and bytecode generation
- **`support:quarkus:integration-tests`** - Quarkus integration tests (currently excluded from build)

## Architecture

### Command Flow

1. **Dispatch** - `MessageGateway.send(Command)` or `sendAsync(Command, Class<R>)` receives the command
2. **Resolve** - `DefaultMessageGateway` looks up handlers via `HandlerRegistry.getHandlersForCommand(command)`
3. **Filter** - Registry filters handlers by type compatibility and `canHandle(command)` predicate
4. **Intercept** - `DefaultInterceptorChain` processes registered `CommandInterceptor`s in order
5. **Execute** - The handler's `handle(command)` method is invoked

### Core Interfaces

**CommandHandler<C extends Command, R>**
- `R handle(C command)` - Processes the command and returns a result
- `default boolean canHandle(C command)` - Returns `true` by default; override for conditional routing

**HandlerMetadata<C, R>**
- `Class<C> commandType()` - The command type this handler processes
- `Class<R> resultType()` - The result type this handler produces
- `Class<? extends CommandHandler<C, R>> handlerClass()` - The handler implementation class

**HandlerRegistry**
- `List<CommandHandler<C, R>> getHandlersForCommand(C command)` - Returns handlers that can process the given command

**CommandInterceptor**
- `int order()` - Lower values execute first (default: 0)
- `<C extends Command, R> R intercept(C command, CommandInterceptorChain chain)` - Intercept command execution

### Handler Registration Patterns

**Plain Java (messaging-core):**
```java
// Create handlers and metadata manually
List<CommandHandler<?, ?>> handlers = List.of(new CreateOrderHandler());
List<HandlerMetadata<?, ?>> metadata = List.of(new CreateOrderHandlerMetadata());
HandlerRegistry registry = new DefaultHandlerRegistry(handlers, metadata);
MessageGateway gateway = new DefaultMessageGateway(registry, interceptors);
```

**CDI (messaging-cdi):**
```java
@Inject
MessageGateway gateway;

@Inject
CommandHandler<MyCommand, MyResult> handler; // Handlers are CDI beans
```

**Quarkus (tower-quarkus):**
- Handlers are discovered at build time via Jandex
- `HandlerMetadata` classes are generated using bytecode generation (`HandlerMetadataGenerator`)
- `QuarkusHandlerRegistry` collects all handlers via CDI injection

### Type Resolution (Quarkus Extension)

The `HandlerTypeResolver` in the deployment module extracts generic type parameters from `CommandHandler<C, R>` implementations using Jandex. This enables build-time metadata generation without reflection at runtime.

### Batch Execution

The `VirtualThreadBatchExecutor` (using Java 25 preview features) provides batch command execution with virtual threads for I/O-bound operations.

## Testing

Unit tests use JUnit 5 and AssertJ. Tests can be run standalone without CDI:

```java
// Example test pattern
SimpleCommandHandler handler = new SimpleCommandHandler();
HandlerMetadata<SimpleCommand, String> metadata = createMetadata(
    SimpleCommand.class, String.class, SimpleCommandHandler.class);

HandlerRegistry registry = new DefaultHandlerRegistry(
    List.of(handler), List.of(metadata));

List<CommandHandler<SimpleCommand, String>> handlers =
    registry.getHandlersForCommand(new SimpleCommand("test"));
```

Quarkus tests use `@QuarkusTest` and inject `MessageGateway`:

```java
@QuarkusTest
public class BasicTest {
    @Inject
    MessageGateway gateway;
}
```

## Conventions

### Code Style

- 4-space indentation (defined in `.editorconfig`)
- Max line length: 120 characters
- UTF-8 encoding
- LF line endings

### Build Conventions

The `buildSrc` directory contains convention plugins:

- **`tower.java-conventions.gradle.kts`** - Java 25 source/target compatibility, preview features enabled, JUnit 5 setup
- **`tower.maven-publish.gradle.kts`** - Maven publishing configuration with POM metadata

### Release Process

The project uses JReleaser for publishing to Maven Central. See `RELEASING.md` for details.

Required environment variables for publishing:
- `JRELEASER_MAVENCENTRAL_USERNAME`
- `JRELEASER_MAVENCENTRAL_PASSWORD`
- `JRELEASER_GPG_PUBLIC_KEY`
- `JRELEASER_GPG_SECRET_KEY`
- `JRELEASER_GPG_PASSPHRASE`
