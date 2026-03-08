# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Tower Messaging is a Java framework implementing CQRS (Command Query Responsibility Segregation) and messaging patterns. It provides a message bus for dispatching commands and queries, with support for CDI and Quarkus integration.

## Build System

This is a Gradle multi-module project using Kotlin DSL for build scripts.

**Prerequisites:**
- Java 21+ (configured in `java-conventions.gradle.kts`)
- Gradle 9.0+ (via wrapper)
- `MAVEN_USERNAME` and `MAVEN_PASSWORD` properties must be defined in `~/.gradle/gradle.properties` for publishing (dummy values are fine for local builds)

**Common Commands:**

```bash
# Build all modules (use wrapper for Gradle 9.0)
./gradlew build

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :messaging-core:test

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Clean build outputs
./gradlew clean
```

## Project Structure

The project is organized into the following modules:

### Core Modules

- **`common`** - Shared utilities (TypeReflectionKit, Classes, CommonKit, ListKit)
- **`messaging-core`** - Core messaging framework with MessageBus, message handlers, and dispatch logic
- **`messaging-cdi`** - CDI integration for dependency injection support

### Schema Modules

- **`schema:schema-model`** - Model classes for schema definitions (Operation, Field, Argument, InputType, etc.)
- **`schema:schema-builder`** - Schema builder using Jandex for annotation scanning and model creation

### Quarkus Support

- **`support:quarkus:tower-quarkus`** - Quarkus runtime extension
- **`support:quarkus:tower-quarkus-deployment`** - Quarkus build-time deployment processor
- **`support:quarkus:integration-tests`** - Quarkus integration tests (currently excluded due to Gradle 9.0 compatibility issue)

### Dependencies

- **`messaging-dependencies`** - BOM (Bill of Materials) platform module for dependency version management

## Architecture

### Message Flow

1. Messages are dispatched through `MessageBus.dispatch(Message<R>)`
2. `DefaultMessageBus.route()` resolves the appropriate `MessageHandle` based on the message identifier and operation type (QUERY/COMMAND)
3. `DefaultInterceptorChain` processes any registered interceptors
4. `MessageHandle.handle()` invokes the actual method via `OperationInvoker`

### Key Annotations

- `@UseCase` - Marks a class as containing message handlers
- `@QueryHandle` - Marks a method as a query handler
- `@CommandHandle` - Marks a method as a command handler
- `@Predicate` - Marks a method as a predicate for conditional message handling
- `@Parameter` - Maps method parameters to message payload fields

### Schema Building

The `SchemaBuilder` uses Jandex to scan classes annotated with `@UseCase` at build time:

1. `SchemaBuilder.build(IndexView)` creates a schema from the index
2. `OperationCreator` creates Operation definitions from annotated methods
3. `InputTypeCreator` creates input type definitions from method parameters
4. `ReferenceCreator` manages type references and dependencies

### Testing

Unit tests use JUnit 5 and AssertJ. The test infrastructure uses Jandex indexing:

```java
// Example test pattern from messaging-core tests
IndexView index = Indexer.getTestIndex("io/iamcyw/tower/messaging/test");
Schema schema = SchemaBuilder.build(index);
Bootstrap bootstrap = new Bootstrap(schema);
MessageGateway gateway = bootstrap.getMessageGateway();
```

Quarkus tests use `@QuarkusTest` and inject `MessageGateway`.

## Conventions

### Code Style

- 4-space indentation (defined in `.editorconfig`)
- Max line length: 120 characters
- UTF-8 encoding
- LF line endings

### Build Conventions

The `buildSrc` directory contains convention plugins:

- **`java-conventions.gradle.kts`** - Java 21 source/target compatibility, JUnit 5 setup, JBoss Logging annotation processor
- **`maven-deploy.gradle.kts`** - Maven publishing configuration with sources and Javadoc jars

All Java modules apply these conventions and inherit dependency versions from `messaging-dependencies`.

### Package Structure

```
io.iamcyw.tower
├── messaging          # Core messaging APIs and implementations
├── messaging.cdi      # CDI integration
├── schema             # Schema model and builder
└── quarkus            # Quarkus extension
```
