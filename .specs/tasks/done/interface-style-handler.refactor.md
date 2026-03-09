---
title: Refactor Tower Messaging from annotation-driven to interface-based CommandHandler
status: draft
issue_type: feature
complexity: XL
analysis: analysis-command-handler-refactor
skill: command-handler-pattern
---

> **Required Skill**: You MUST use and analyse `command-handler-pattern` skill before doing any modification to task file or starting implementation of it!
>
> Skill location: `.claude/skills/command-handler-pattern/SKILL.md`

# Initial User Prompt

将 Tower Messaging 从注解驱动风格重构为接口强制风格：

1. 定义 CommandHandler<C extends Command, R> 接口，强制 handle(C) 方法
2. 移除 @UseCase, @CommandHandle 等注解，使用 implements CommandHandler 显式声明
3. 通过泛型 <C, R> 实现编译期类型安全
4. 使用 CDI Instance<CommandHandler<?, ?>> 自动收集所有实现
5. 保持轻量设计，不引入复杂 DDD 概念
6. 支持条件判断：接口提供 default condition(C) 方法
7. 批量命令执行：顺序、并行（虚拟线程）、结构化并发
8. Quarkus 集成：@Singleton 管理 Handler 实例

核心目标：用接口约束防止退化为普通 Service，保持代码清晰可维护。

# Description

Refactor Tower Messaging framework from annotation-driven command handling to interface-based command handling. The current @UseCase/@CommandHandle approach allows developers to create handlers without explicit contracts, leading to potential architectural drift where the framework may be misused as a generic service layer.

The new approach introduces a CommandHandler<C extends Command, R> interface that developers must explicitly implement. This enforces compile-time type safety for command and result types, makes handler responsibilities explicit in the code, and prevents misuse of the framework. The interface includes a handle(C command) method for execution and a default canHandle(C command) method for conditional routing.

This refactoring affects the core messaging module, schema builder, and Quarkus extension. The framework maintains its no-reflection principle through build-time processing and code generation. Runtime handler discovery uses CDI for automatic collection. Batch execution support includes sequential, parallel (using Java 21 virtual threads), and structured concurrency modes.

## Scope

**Included:**
- CommandHandler<C, R> interface design and implementation
- HandlerMetadata<C, R> interface and build-time generation
- Build-time interface scanning
- Runtime handler registry with CDI integration
- Batch command execution (sequential, parallel, structured)
- Quarkus extension adaptation for interface-based discovery
- Deprecation of @UseCase, @CommandHandle, @Predicate annotations
- Migration of existing integration tests

**Excluded:**
- Query handler refactoring (out of scope)
- Event sourcing support
- Legacy annotation adapter/migration helper
- Distributed command execution across services
- Changes to MessageGateway public API

## User Scenarios

1. **Primary Flow**: Developer implements CommandHandler interface, framework discovers and registers handler at build time, command dispatch routes to correct handler with type safety.
2. **Alternative Flow - Conditional Routing**: Multiple handlers for same command type use canHandle() method to determine which should process each command instance.
3. **Error Handling**: Missing handler results in clear exception, type mismatches caught at build time, batch execution failures include context for partial results.

## Design Principles

**No Reflection Principle**: The entire framework must operate without runtime reflection. All type information and method invocation are resolved at build time, achieving:
- Faster startup (no runtime reflection analysis)
- Better native image compatibility
- Explicit type safety

## Core Interface Design

```java
public interface CommandHandler<C extends Command, R> {
    /**
     * Process command and return result
     */
    R handle(C command);

    /**
     * Default condition check: whether this handler can process the command
     * Subclasses can override for conditional routing
     */
    default boolean canHandle(C command) {
        return true;
    }
}

/**
 * Handler type metadata - generated at build time, no reflection at runtime
 */
public interface HandlerMetadata<C extends Command, R> {
    Class<C> commandType();
    Class<R> resultType();
    String handlerName();
}
```

## Technical Approach

Build-time processing uses bytecode indexing to scan for interface implementations and bytecode generation to create metadata classes. This maintains the no-reflection principle while providing runtime type information.

Runtime handler discovery uses CDI for automatic collection of handler implementations. The handler registry matches handlers with their generated metadata for type-safe routing.

Quarkus integration uses build steps for interface scanning and bean registration. Generated metadata classes are registered as CDI beans for runtime injection.

## Migration Path

### 1. Existing Code Migration Example

**Before (Annotation-Driven):**
```java
@UseCase
public class TestService {

    @CommandHandle
    @Parameter(value = "test", parameter = "payload")
    public void command(BasicTestCommand basicTestCommand) {
        // processing logic
    }

    @Predicate
    public boolean testPredicate(BasicTestCommand payload, @Parameter("test") String type) {
        return payload.getPayload().equals(type);
    }
}
```

**After (Interface-Driven):**
```java
@Singleton
public class BasicTestCommandHandler
    implements CommandHandler<BasicTestCommand, Void> {

    private final String expectedPayload;

    @Inject
    public BasicTestCommandHandler(@ConfigProperty(name = "test.payload") String expectedPayload) {
        this.expectedPayload = expectedPayload;
    }

    @Override
    public Void handle(BasicTestCommand command) {
        // processing logic
        LOGGER.info("command: {} payload: {}",
            command.getClass().getName(),
            command.getPayload());
        return null;
    }

    @Override
    public boolean canHandle(BasicTestCommand command) {
        // Original @Predicate logic moved to canHandle
        return command.getPayload().equals(expectedPayload);
    }
}
```

### 2. Annotation Deprecation Strategy

```java
@Deprecated(since = "2.0", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UseCase {
    String migrationGuide() default "Migrate to CommandHandler interface";
}

@Deprecated(since = "2.0", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandle {
}
```

## Affected Files

| Module | File | Change Type |
|--------|------|-------------|
| messaging-core | (new) CommandHandler.java | Add |
| messaging-core | (new) HandlerMetadata.java | Add |
| messaging-core | (new) BatchCommandExecutor.java | Add |
| messaging-core | DefaultMessageBus.java | Modify |
| messaging-core | UseCase.java, CommandHandle.java | Deprecate |
| schema-builder | SchemaBuilder.java | Modify |
| schema-builder | Annotations.java | Modify/Simplify |
| schema-builder | OperationCreator.java | Modify |
| schema-builder | (new) HandlerTypeResolver.java | Add |
| quarkus-deployment | MessageQuarkusProcessor.java | Major Modify |
| quarkus-deployment | MethodInvokerFactory.java | Major Modify |
| quarkus-deployment | (new) CommandHandlerBuildItem.java | Add |
| quarkus-deployment | (new) HandlerMetadataGenerator.java | Add |
| quarkus-runtime | (new) QuarkusHandlerRegistry.java | Add |
| integration-tests | TestService.java | Rewrite |

## Acceptance Criteria

### Functional Requirements

- [ ] **AC1: CommandHandler Interface Definition**
  - Given: Framework core module exists
  - When: Developer defines a CommandHandler implementation
  - Then: Interface enforces handle(C command) method implementation
  - And: Interface provides default canHandle(C command) returning true

- [ ] **AC2: HandlerMetadata Interface Definition**
  - Given: Framework core module exists
  - When: Build processes a CommandHandler implementation
  - Then: Metadata interface provides commandType() method
  - And: Metadata interface provides resultType() method
  - And: Metadata interface provides handlerName() method

- [ ] **AC3: Build-Time Interface Scanning**
  - Given: Classes implementing CommandHandler in the project
  - When: Build process executes
  - Then: All CommandHandler implementors are discovered
  - And: Generic type parameters are extracted for each handler

- [ ] **AC4: Handler Metadata Generation**
  - Given: A CommandHandler implementation class
  - When: Build completes
  - Then: A HandlerMetadata implementation class is generated
  - And: Generated class is registered as a CDI bean

- [ ] **AC5: Runtime Handler Registration**
  - Given: Application with handler and metadata beans
  - When: Application starts
  - Then: All handlers are registered with their metadata
  - And: Handlers are available for command dispatch

- [ ] **AC6: Command Dispatch Routing**
  - Given: A registered handler for a command type
  - When: A command of that type is submitted for processing
  - Then: The correct handler processes the command
  - And: The result is returned to the caller

- [ ] **AC7: Conditional Routing via canHandle**
  - Given: Multiple handlers for the same command type
  - And: Handlers override canHandle() with different conditions
  - When: A command is submitted for processing
  - Then: Only handlers where canHandle() returns true process the command

- [ ] **AC8: Sequential Batch Execution**
  - Given: A list of commands to execute
  - When: Sequential batch execution is requested
  - Then: Commands execute in order
  - And: Results are returned in matching order
  - And: Execution stops on first failure

- [ ] **AC9: Parallel Batch Execution**
  - Given: A list of independent commands to execute
  - When: Parallel batch execution is requested
  - Then: Commands execute concurrently
  - And: Results are collected when all complete
  - And: Virtual threads are used for execution

- [ ] **AC10: Structured Concurrency Batch Execution**
  - Given: A list of commands and a timeout duration
  - When: Structured concurrency execution is requested
  - Then: All commands complete within timeout
  - And: If one fails, all others are cancelled
  - And: If timeout occurs, pending commands are cancelled

- [ ] **AC11: Legacy Annotation Deprecation**
  - Given: Existing @UseCase, @CommandHandle, @Predicate annotations
  - When: Framework is built
  - Then: Annotations are marked as deprecated
  - And: Deprecation notice indicates removal version

- [ ] **AC12: No Runtime Reflection**
  - Given: Runtime execution of command processing
  - When: Profiling or analyzing the code
  - Then: No reflection calls are made for handler discovery
  - And: No reflection calls are made for handler invocation

- [ ] **AC13: Integration Test Migration**
  - Given: Existing tests using annotation style
  - When: Tests are migrated to interface style
  - Then: All tests pass with new implementation
  - And: Test coverage is maintained

- [ ] **AC14: Quarkus Extension Compatibility**
  - Given: Quarkus application with interface-based handlers
  - When: Application builds and runs
  - Then: Handlers are discovered and registered correctly
  - And: Application works in JVM mode
  - And: Application works in native mode

### Error Scenarios

- [ ] **AC15: Missing Handler Error**
  - Given: A command with no registered handler
  - When: Command submission is attempted
  - Then: Clear exception indicates no handler found
  - And: Error message includes command type

- [ ] **AC16: Handler Exception Handling**
  - Given: A handler that throws an exception
  - When: Command is processed by that handler
  - Then: Exception propagates with handler context
  - And: Original exception is preserved

- [ ] **AC17: Batch Partial Failure - Parallel**
  - Given: Multiple commands in parallel batch
  - When: Some commands fail during execution
  - Then: Successful results are collected
  - And: Failed commands are recorded with exceptions
  - And: Aggregate exception contains all failures

- [ ] **AC18: Build-Time Type Validation**
  - Given: Handler with invalid generic types
  - When: Build-time scanning occurs
  - Then: Build fails with clear error message
  - And: Problematic handler class is identified

### Non-Functional Requirements

- [ ] **AC19: Command Dispatch Performance**
  - Given: Single command execution
  - When: Command is processed
  - Then: End-to-end latency is under 1 millisecond

- [ ] **AC20: Batch Execution Scalability**
  - Given: Multiple commands in parallel batch
  - When: Commands are executed
  - Then: Execution scales linearly with available virtual threads

- [ ] **AC21: Native Image Compatibility**
  - Given: Application compiled to native image
  - When: Command processing occurs
  - Then: All features work without reflection errors

---

# Architecture Overview

> **References**:
> - Skill: `/Users/walkin/SourceCode/citywalki/tower-messaging/.claude/skills/command-handler-pattern/SKILL.md`
> - Analysis: `/Users/walkin/SourceCode/citywalki/tower-messaging/.specs/analysis/analysis-command-handler-refactor.md`
> - Scratchpad: `/Users/walkin/SourceCode/citywalki/tower-messaging/.specs/scratchpad/9b7519b8.md`

## Solution Strategy

**Architecture Pattern**: Hexagonal (Ports & Adapters) with Clean Architecture layers

**Approach**: Build-time metadata generation with zero-reflection runtime

The refactoring introduces a `CommandHandler<C extends Command, R>` interface as the core domain contract. Build-time processing using Jandex discovers all implementations and Gizmo generates `$Metadata` classes containing type information. Runtime uses CDI to collect handlers and metadata, with a `HandlerRegistry` providing type-safe lookup. This maintains the framework's zero-reflection principle while adding compile-time type safety.

**Key Decisions**:
1. **Interface over Annotation**: `CommandHandler` interface enforces explicit contracts, preventing anemic service classes
2. **Build-Time Generation**: `$Metadata` classes generated at build time avoid runtime reflection
3. **CDI Integration**: Uses `@All` for handler collection, aligning with Quarkus optimization philosophy
4. **Conditional Routing**: `canHandle()` default method replaces `@Predicate` annotation
5. **Batch Execution**: Virtual threads and structured concurrency for parallel execution

**Trade-offs Accepted**:
- **Build complexity vs Runtime performance**: Accept complex build-time logic for zero-reflection runtime
- **Verbosity vs Type safety**: Accept more verbose handler definitions for compile-time checking
- **Breaking change vs Clean architecture**: Accept breaking change from annotations to interfaces for better design

---

## Architecture Decomposition

**Components**:

| Component | Responsibility | Dependencies |
|-----------|---------------|--------------|
| Command | Marker interface for type safety | None |
| CommandHandler<C,R> | Core handler contract with handle() and canHandle() | Command |
| HandlerMetadata<C,R> | Type metadata for runtime lookup | Command |
| HandlerRegistry | Runtime handler lookup by command type | CommandHandler, HandlerMetadata |
| BatchCommandExecutor | Execute multiple commands with different strategies | HandlerRegistry |
| HandlerTypeResolver | Extract generic C,R types from interface | Jandex |
| HandlerMetadataGenerator | Generate $Metadata classes using Gizmo | Gizmo |
| QuarkusHandlerRegistry | CDI-based registry implementation | CDI @All |

**Interactions**:
```
Build-Time:
  Jandex Index --> HandlerTypeResolver --> CommandHandlerBuildItem
                                                  |
                                                  v
                                         HandlerMetadataGenerator
                                                  |
                                                  v
                                          {Handler}$Metadata

Runtime:
  Client --> MessageBus --> HandlerRegistry --> CommandHandler
                               |                    |
                               v                    v
                         HandlerMetadata         Result
```

---

## Expected Changes

```
messaging-core/src/main/java/io/iamcyw/tower/messaging/
├── Command.java                           # NEW: Marker interface
├── CommandHandler.java                    # NEW: Core handler interface
├── HandlerMetadata.java                   # NEW: Metadata interface
├── HandlerRegistry.java                   # NEW: Registry interface
├── DefaultHandlerRegistry.java            # NEW: Registry implementation
├── BatchCommandExecutor.java              # NEW: Batch execution interface
├── VirtualThreadBatchExecutor.java        # NEW: Parallel execution impl
├── UseCase.java                           # UPDATE: @Deprecated(forRemoval)
├── CommandHandle.java                     # UPDATE: @Deprecated(forRemoval)
├── Predicate.java                         # UPDATE: @Deprecated(forRemoval)
├── DefaultMessageBus.java                 # UPDATE: Use HandlerRegistry
└── bootstrap/Bootstrap.java               # UPDATE: Support new model

schema/schema-builder/src/main/java/io/iamcyw/tower/schema/
├── SchemaBuilder.java                     # UPDATE: Scan interfaces
├── Annotations.java                       # UPDATE: Add CommandHandler DotName
├── HandlerTypeResolver.java               # NEW: Generic type extraction
└── creator/OperationCreator.java          # UPDATE: Create from ClassInfo

support/quarkus/tower-quarkus-deployment/
├── MessageQuarkusProcessor.java           # UPDATE: Interface scanning
├── MethodInvokerFactory.java              # UPDATE: Adapt to handle()
├── CommandHandlerBuildItem.java           # NEW: Build item
└── HandlerMetadataGenerator.java          # NEW: Gizmo generation

support/quarkus/tower-quarkus/src/.../runtime/
├── QuarkusHandlerRegistry.java            # NEW: CDI registry
└── MessageRecorder.java                   # UPDATE: Init registry

support/quarkus/integration-tests/
└── TestService.java                       # UPDATE: Interface style
```

---

## Building Block View

```
+------------------------------------------------------------------+
|                    Tower Messaging (New)                          |
+------------------------------------------------------------------+
|  +-------------+  +-------------+  +-------------------------+   |
|  |   Command   |  | CommandHandler| |   HandlerMetadata       |   |
|  |  (marker)   |  |<C extends    |  |<C, R>                   |   |
|  |             |  | Command, R>  |  |                         |   |
|  |             |  |             |  |  - commandType()        |   |
|  |             |  |  - handle(C) |  |  - resultType()         |   |
|  |             |  |  - canHandle()|  |  - handlerName()        |   |
|  +------+------+  +------+------+  +-------------------------+   |
|         |                |                                       |
|         +----------------+---------------------------------------+
|                          |                                       |
|                          v                                       |
|  +-----------------------------------------------------------+   |
|  |                    HandlerRegistry                         |   |
|  |  - getHandlersForCommand(Command)                         |   |
|  |  - lookup by type, filter by canHandle()                  |   |
|  +-------------------------+---------------------------------+   |
|                            |                                     |
|                            v                                     |
|  +-----------------------------------------------------------+   |
|  |                 BatchCommandExecutor                       |   |
|  |  - executeSequential()    - executeParallel()             |   |
|  |  - executeStructured()                                    |   |
|  +-----------------------------------------------------------+   |
+------------------------------------------------------------------+
|                        Adapters                                   |
|  +-----------------+  +-----------------+  +-----------------+   |
|  |  Quarkus Build  |  |  CDI Runtime    |  |  Schema Builder |   |
|  |  - Jandex scan  |  |  - @All inject  |  |  - Type resolve |   |
|  |  - Gizmo gen    |  |  - Registry init|  |  - Metadata gen |   |
|  +-----------------+  +-----------------+  +-----------------+   |
+------------------------------------------------------------------+
```

---

## Runtime Scenarios

**Scenario: Single Command Dispatch**

```
Client
    |
    v
MessageBus.dispatch(CreateOrderCommand)
    |
    v
HandlerRegistry.getHandlersForCommand(command)
    ├── filter: metadata.commandType().isInstance(command)
    │   └── CreateOrderHandler$Metadata.commandType() = CreateOrderCommand.class
    ├── filter: handler.canHandle(command)
    │   └── CreateOrderHandler.canHandle() returns true
    └── returns [CreateOrderHandler]
    |
    v
CreateOrderHandler.handle(CreateOrderCommand)
    |
    v
OrderResult returned
```

**Scenario: Conditional Routing**

```
Command: CreateOrderCommand(customerType=VIP)

HandlerRegistry.getHandlersForCommand(command)
    ├── CreateOrderHandler
    │   ├── metadata.commandType().isInstance(command) = true
    │   └── canHandle(command) = false (customerType != REGULAR)
    ├── PriorityOrderHandler
    │   ├── metadata.commandType().isInstance(command) = true
    │   └── canHandle(command) = true (customerType == VIP)
    └── returns [PriorityOrderHandler]
```

---

## Architecture Decisions

### ADR-1: Interface-Based vs Annotation-Based

**Status**: Accepted

**Context**: Current framework uses @UseCase/@CommandHandle annotations which allow developers to create handlers without explicit contracts, leading to potential architectural drift.

**Decision**: Use pure interface-based approach with CommandHandler<C, R> interface.

**Consequences**:
- (+) Explicit contracts prevent misuse
- (+) Compile-time type safety
- (+) Clearer code intent
- (-) Breaking change for existing users
- (-) More verbose handler definitions

### ADR-2: Build-Time Metadata Generation

**Status**: Accepted

**Context**: Runtime needs type information (C and R) for handler lookup, but generic types are erased at runtime.

**Decision**: Use build-time Gizmo generation of $Metadata classes.

**Consequences**:
- (+) Zero runtime reflection
- (+) Native image compatible
- (+) Fast runtime performance
- (-) Complex build-time logic
- (-) Generated code maintenance

### ADR-3: CDI @All vs Instance<T> for Handler Collection

**Status**: Accepted

**Context**: Need to collect all CommandHandler implementations at runtime.

**Decision**: Use `@All` for eager collection with `Instance` as fallback option.

**Consequences**:
- (+) Quarkus-optimized
- (+) Clean injection
- (+) Startup-time validation
- (-) Quarkus-specific (not portable)

### ADR-4: Batch Execution Strategy

**Status**: Accepted

**Context**: Need to support batch command execution with different concurrency models.

**Decision**: Use virtual threads with structured concurrency for parallel execution.

**Consequences**:
- (+) High throughput for I/O bound commands
- (+) Structured concurrency for error handling
- (+) Java 21 modern approach
- (-) Requires Java 21+
- (-) Potential thread pinning if synchronized used

---

## High-Level Structure

```
Feature: Interface-Based Command Handling
|
├── Entry Points
│   ├── CommandHandler.handle(C command)     # Handler implementation
│   └── MessageBus.dispatch(Message<R>)      # Client API (existing)
│
├── Core Logic
│   ├── HandlerRegistry.getHandlersForCommand()  # Handler lookup
│   ├── canHandle() filtering                    # Conditional routing
│   └── BatchCommandExecutor                     # Batch strategies
│
├── Data Layer
│   ├── HandlerMetadata<C,R>                     # Type information
│   ├── {Handler}$Metadata (generated)           # Build-time metadata
│   └── CommandHandlerBuildItem                  # Build-time data
│
└── Output
    ├── R result (command result)                # Single execution
    └── List<R> results (batch execution)        # Batch execution
```

---

## Workflow Steps

**Build-Time Workflow**:
```
1. Jandex Index Scan
   └── Find all CommandHandler implementors
   |
   v
2. Type Extraction
   └── Extract <C, R> generic parameters
   |
   v
3. Metadata Generation
   └── Generate {Handler}$Metadata class with Gizmo
   |
   v
4. CDI Registration
   └── Register metadata as @Singleton bean
```

**Runtime Workflow**:
```
1. Command Submission
   └── MessageBus.dispatch(command)
   |
   v
2. Handler Resolution
   └── HandlerRegistry filters by type and canHandle()
   |
   v
3. Handler Execution
   └── CommandHandler.handle(command)
   |
   v
4. Result Return
   └── Return result to caller
```

---

## Contracts

**Interface Contract**:
```java
// Core handler interface
public interface CommandHandler<C extends Command, R> {
    R handle(C command);
    default boolean canHandle(C command) { return true; }
}

// Metadata interface (implemented by generated classes)
public interface HandlerMetadata<C extends Command, R> {
    Class<C> commandType();
    Class<R> resultType();
    String handlerName();
}

// Registry interface
public interface HandlerRegistry {
    <C extends Command, R> List<CommandHandler<C, R>> getHandlersForCommand(C command);
}

// Batch executor interface
public interface BatchCommandExecutor {
    <R> List<R> executeSequential(List<? extends Command> commands);
    <R> List<R> executeParallel(List<? extends Command> commands);
    <R> List<R> executeStructured(List<? extends Command> commands, Duration timeout);
}
```

---

---

## Implementation Process

You MUST launch for each step a separate agent, instead of performing all steps yourself. And for each step marked as parallel, you MUST launch separate agents in parallel.

**CRITICAL:** For each agent you MUST:
1. Use the **Agent** type specified in the step (e.g., `sdd:developer`, `sdd:tech-writer`)
2. Provide path to task file and prompt which step to implement
3. Require agent to implement exactly that step, not more, not less, not other steps

---

### Parallelization Overview

```
                    Parallelization Overview

Phase 1: Foundation (Parallel)
--------------------------------
Step 1 [sdd:developer/opus]          Step 2 [sdd:developer/haiku]
Create Core Interfaces                 Deprecate Legacy Annotations
(No dependencies)                      (No dependencies)
    │                                      │
    └──────────────────┬───────────────────┘
                       ▼
Phase 2: Core Components (Parallel)
------------------------------------
Step 3 [sdd:developer/opus]          Step 4 [sdd:developer/opus]
HandlerTypeResolver                    HandlerRegistry
(Depends: Step 1)                      (Depends: Step 1)
    │                                      │
    │                                      ▼
    │                                  Step 5 [sdd:developer/opus]
    │                                  BatchCommandExecutor
    │                                  (Depends: Step 4)
    │                                      │
    ▼                                      │
Step 6 [sdd:developer/opus]              │
HandlerMetadataGenerator                 │
(Depends: Step 3)                        │
    │                                      │
    └──────────────────┬───────────────────┘
                       ▼
Phase 3: Integration (Sequential)
----------------------------------
Step 7 [sdd:developer/opus]
Update Quarkus Extension
(Depends: Steps 3, 4, 6)
    │
    ▼
Phase 4: Validation (Sequential)
---------------------------------
Step 8 [sdd:developer/opus]
Migrate Tests and Validate
(Depends: Steps 1-7)
```

---

### Implementation Strategy

**Approach**: Mixed (Bottom-Up for Core, Top-Down for Integration)

**Rationale**:
- Core interfaces and build-time components use Bottom-Up: foundation first
- Quarkus integration uses Top-Down: workflow first, then fill in details
- This matches the dependency chain and allows parallel workstreams

### Least-to-Most Decomposition Chain

| Level | Subproblem | Depends On | Why This Order |
|-------|------------|------------|----------------|
| 0 | Define core interfaces (Command, CommandHandler, HandlerMetadata) | - | Foundation - no dependencies, pure contracts |
| 0 | Deprecate legacy annotations | - | Can be done in parallel with interface creation |
| 1 | Build-time type resolution (HandlerTypeResolver) | Level 0 | Needs CommandHandler interface to exist |
| 1 | Runtime handler registry (HandlerRegistry) | Level 0 | Needs CommandHandler interface |
| 2 | Batch command execution | Level 1 | Needs HandlerRegistry to dispatch |
| 2 | Metadata generation infrastructure | Level 1 | Needs type resolution |
| 3 | Quarkus extension integration | Level 2 | Needs all core components |
| 3 | Test migration and validation | Level 2 | Needs working implementation |

---

### Step 1: Create Core Interfaces

**Model:** opus
**Agent:** sdd:developer
**Depends on:** None
**Parallel with:** Step 2

**Goal**: Define Command, CommandHandler, and HandlerMetadata interfaces as the foundation for the new architecture.

**Complexity**: Small
**Uncertainty**: Low
**Level**: 0

#### Expected Output

- `messaging-core/src/main/java/io/iamcyw/tower/messaging/Command.java` - Marker interface
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/CommandHandler.java` - Core handler interface
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/HandlerMetadata.java` - Metadata interface

#### Success Criteria

- [ ] Command interface exists as a marker interface
- [ ] CommandHandler<C extends Command, R> interface exists with handle(C) method
- [ ] CommandHandler provides default canHandle(C) method returning true
- [ ] HandlerMetadata<C extends Command, R> interface exists with commandType(), resultType(), handlerName() methods
- [ ] All interfaces compile without errors

#### Verification

**Level:** ✅ CRITICAL - Panel of 2 Judges with Aggregated Voting
**Artifacts:**
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/Command.java`
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/CommandHandler.java`
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/HandlerMetadata.java`
**Threshold:** 4.0/5.0

**Rubric:**

| Criterion | Weight | Description |
|-----------|--------|-------------|
| API Design Quality | 0.25 | Interfaces follow clean API design principles, clear method names, appropriate generics constraints |
| Type Safety | 0.25 | Generic parameters C extends Command, R result type are correctly bounded and used |
| Contract Completeness | 0.20 | All required methods present (handle, canHandle, commandType, resultType, handlerName) |
| Documentation Quality | 0.15 | Javadoc explains purpose, parameters, return values, and usage examples |
| Backward Compatibility | 0.15 | Design allows for future evolution without breaking changes |

#### Subtasks

- [ ] Create Command.java marker interface
- [ ] Create CommandHandler.java with handle() and canHandle() methods
- [ ] Create HandlerMetadata.java with type accessor methods
- [ ] Write unit tests for interface contracts

#### Blockers

None - this is the foundation step.

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Interface design doesn't support future requirements | High | Low | Review design against all acceptance criteria before implementation |

#### Definition of Done

- [ ] All three interfaces created
- [ ] Unit tests verify interface contracts
- [ ] Code compiles successfully

---

### Step 2: Deprecate Legacy Annotations

**Model:** haiku
**Agent:** sdd:developer
**Depends on:** None
**Parallel with:** Step 1

**Goal**: Mark @UseCase, @CommandHandle, and @Predicate annotations as deprecated to signal the migration path.

**Complexity**: Small
**Uncertainty**: Low
**Level**: 0

#### Expected Output

- Updated `messaging-core/src/main/java/io/iamcyw/tower/messaging/UseCase.java`
- Updated `messaging-core/src/main/java/io/iamcyw/tower/messaging/CommandHandle.java`
- Updated `messaging-core/src/main/java/io/iamcyw/tower/messaging/Predicate.java`

#### Success Criteria

- [ ] All three annotations marked with @Deprecated(since = "2.0", forRemoval = true)
- [ ] Javadoc includes migration guide reference
- [ ] Deprecation warnings are generated when annotations are used

#### Verification

**Level:** ❌ NOT NEEDED
**Rationale:** Simple mechanical deprecation changes. Success is binary - annotations either have @Deprecated or they don't. Build will fail if syntax is incorrect.

#### Subtasks

- [ ] Add @Deprecated annotation to UseCase.java
- [ ] Add @Deprecated annotation to CommandHandle.java
- [ ] Add @Deprecated annotation to Predicate.java
- [ ] Update javadoc with migration guidance

#### Blockers

None - can run in parallel with Step 1.

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Existing tests may generate deprecation warnings | Low | High | Acceptable - tests will be migrated in Step 8 |

#### Definition of Done

- [ ] All three annotations deprecated
- [ ] Javadoc updated with migration guidance
- [ ] Build succeeds with deprecation warnings

---

### Step 3: Create HandlerTypeResolver

**Model:** opus
**Agent:** sdd:developer
**Depends on:** Step 1
**Parallel with:** Step 4

**Goal**: Implement build-time type resolution to extract generic C,R types from CommandHandler implementations using Jandex.

**Complexity**: Medium
**Uncertainty**: Medium
**Level**: 1
**Depends On**: Step 1

#### Expected Output

- `schema/schema-builder/src/main/java/io/iamcyw/tower/schema/HandlerTypeResolver.java`
- `schema/schema-builder/src/main/java/io/iamcyw/tower/schema/model/HandlerTypeInfo.java` (data holder)

#### Success Criteria

- [X] HandlerTypeResolver can extract command type from parameterized interface
- [X] HandlerTypeResolver can extract result type from parameterized interface
- [X] HandlerTypeResolver handles inheritance (recursive superclass checking)
- [X] HandlerTypeResolver validates that types implement Command interface
- [X] All edge cases covered by unit tests

#### Verification

**Level:** ✅ CRITICAL - Panel of 2 Judges with Aggregated Voting
**Artifacts:**
- `schema/schema-builder/src/main/java/io/iamcyw/tower/schema/HandlerTypeResolver.java`
- `schema/schema-builder/src/main/java/io/iamcyw/tower/schema/model/HandlerTypeInfo.java`
**Threshold:** 4.0/5.0

**Rubric:**

| Criterion | Weight | Description |
|-----------|--------|-------------|
| Correctness | 0.30 | Correctly extracts generic types C and R from any inheritance hierarchy |
| Edge Case Handling | 0.25 | Handles recursive generics, multiple inheritance, type erasure edge cases |
| Validation | 0.20 | Validates that extracted types implement Command interface |
| Performance | 0.15 | Efficient Jandex usage, no unnecessary index scans |
| Test Coverage | 0.10 | Comprehensive unit tests for complex inheritance scenarios |

#### Subtasks

- [X] Create HandlerTypeInfo record/class to hold extracted types
- [X] Implement resolve() method using Jandex IndexView
- [X] Implement recursive superclass checking for inherited generics
- [X] Add validation for Command interface implementation
- [X] Write comprehensive unit tests for type extraction

#### Blockers

- Step 1 must complete (CommandHandler interface must exist)

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Generic type erasure edge cases | High | Medium | Extensive unit testing with complex inheritance hierarchies |
| Recursive superclass checking infinite loop | Medium | Low | Track visited classes during recursion |

#### Definition of Done

- [X] HandlerTypeResolver implemented
- [X] All success criteria met
- [X] Unit tests pass with >90% coverage
- [X] Handles complex generic inheritance scenarios

---

### Step 4: Create HandlerRegistry

**Model:** opus
**Agent:** sdd:developer
**Depends on:** Step 1
**Parallel with:** Step 3

**Goal**: Implement runtime handler registry for type-safe handler lookup with canHandle filtering.

**Complexity**: Medium
**Uncertainty**: Low
**Level**: 1
**Depends On**: Step 1

#### Expected Output

- `messaging-core/src/main/java/io/iamcyw/tower/messaging/HandlerRegistry.java` (interface)
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/DefaultHandlerRegistry.java` (implementation)

#### Success Criteria

- [X] HandlerRegistry interface defines getHandlersForCommand(C command) method
- [X] DefaultHandlerRegistry filters handlers by metadata.commandType().isInstance(command)
- [X] DefaultHandlerRegistry filters handlers by handler.canHandle(command)
- [X] Returns empty list when no handlers match
- [X] Returns multiple handlers when multiple match (for conditional routing)
- [X] HandlerRegistryProducer created for CDI integration (FIX)
- [X] HandlerRegistryProducer uses @Singleton and constructor injection (FIX)
- [X] HandlerRegistryProducer injects Instance<CommandHandler<?, ?>> for handlers (FIX)
- [X] HandlerRegistryProducer injects Instance<HandlerMetadata<?, ?>> for metadata (FIX)
- [X] HandlerRegistryProducer has @Produces method creating DefaultHandlerRegistry (FIX)
- [X] CDI integration tests verify automatic handler collection (FIX)

#### Verification

**Level:** ✅ CRITICAL - Panel of 2 Judges with Aggregated Voting
**Artifacts:**
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/HandlerRegistry.java`
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/DefaultHandlerRegistry.java`
**Threshold:** 4.0/5.0

**Rubric:**

| Criterion | Weight | Description |
|-----------|--------|-------------|
| Correctness | 0.30 | Correctly filters handlers by type and canHandle() predicate |
| Performance | 0.20 | Efficient lookup, minimal overhead on critical path |
| Error Handling | 0.20 | Graceful handling of missing handlers, empty results |
| CDI Integration | 0.15 | Proper use of @All or Instance injection, no circular dependencies |
| Type Safety | 0.15 | Safe generic casting with proper suppressions and documentation |

#### Subtasks

- [X] Create HandlerRegistry interface
- [X] Create DefaultHandlerRegistry implementation
- [X] Implement type-based filtering logic
- [X] Implement canHandle() filtering logic
- [X] Write unit tests for registry lookup
- [X] Write unit tests for conditional routing
- [X] Create HandlerRegistryProducer for CDI integration (FIX)
- [X] Inject Instance<CommandHandler<?, ?>> for automatic handler collection (FIX)
- [X] Inject Instance<HandlerMetadata<?, ?>> for metadata collection (FIX)
- [X] Write CDI integration tests for HandlerRegistry (FIX)

#### Blockers

- Step 1 must complete (CommandHandler and HandlerMetadata interfaces must exist)

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| CDI circular dependencies | Medium | Medium | Use constructor injection with lazy resolution |
| Type safety warnings | Low | High | Use @SuppressWarnings("unchecked") with documentation |

#### Definition of Done

- [X] HandlerRegistry interface and implementation created
- [X] HandlerRegistryProducer created for CDI integration (FIX)
- [X] All success criteria met
- [X] Unit tests pass with >90% coverage
- [X] CDI integration tests pass (FIX)
- [X] Handles missing handler scenario gracefully

---

### Step 5: Create BatchCommandExecutor

**Model:** opus
**Agent:** sdd:developer
**Depends on:** Step 4
**Parallel with:** Step 6 (after Step 4 completes)

**Goal**: Implement batch command execution with sequential, parallel (virtual threads), and structured concurrency modes.

**Complexity**: Medium
**Uncertainty**: Medium
**Level**: 2
**Depends On**: Step 4

#### Expected Output

- `messaging-core/src/main/java/io/iamcyw/tower/messaging/BatchCommandExecutor.java` (interface)
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/VirtualThreadBatchExecutor.java` (implementation)
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/BatchExecutionException.java`

#### Success Criteria

- [X] BatchCommandExecutor interface defines executeSequential(), executeParallel(), executeStructured() methods
- [X] executeSequential() processes commands in order, stops on first failure
- [X] executeParallel() uses VirtualThreadPerTaskExecutor for concurrent execution
- [X] executeStructured() uses StructuredTaskScope.ShutdownOnFailure with timeout
- [X] Parallel execution collects all results or throws aggregate exception
- [X] Structured execution cancels all tasks on single failure

#### Verification

**Level:** ✅ CRITICAL - Panel of 2 Judges with Aggregated Voting
**Artifacts:**
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/BatchCommandExecutor.java`
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/VirtualThreadBatchExecutor.java`
- `messaging-core/src/main/java/io/iamcyw/tower/messaging/BatchExecutionException.java`
**Threshold:** 4.0/5.0

**Rubric:**

| Criterion | Weight | Description |
|-----------|--------|-------------|
| Concurrency Safety | 0.30 | No race conditions, proper synchronization where needed |
| Error Handling | 0.25 | Proper exception propagation, aggregate exceptions for parallel execution |
| Resource Management | 0.20 | Proper executor lifecycle, no thread leaks, virtual thread efficiency |
| Structured Concurrency | 0.15 | Correct use of StructuredTaskScope, proper cancellation |
| Test Coverage | 0.10 | Tests for timeout, cancellation, partial failure scenarios |

#### Subtasks

- [X] Create BatchCommandExecutor interface
- [X] Create BatchExecutionException for error handling
- [X] Implement executeSequential() method
- [X] Implement executeParallel() using Executors.newVirtualThreadPerTaskExecutor()
- [X] Implement executeStructured() using StructuredTaskScope.ShutdownOnFailure
- [X] Write unit tests for sequential execution
- [X] Write unit tests for parallel execution
- [X] Write unit tests for structured execution with timeout

#### Blockers

- Step 4 must complete (HandlerRegistry needed for dispatch)

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Virtual thread pinning | Medium | Medium | Document to avoid synchronized blocks in handlers |
| Structured concurrency preview status | Low | Low | Use standard API (final in Java 21) |
| Resource exhaustion with many commands | High | Low | Add configurable batch size limits |

#### Definition of Done

- [X] BatchCommandExecutor interface and implementation created
- [X] All three execution modes working
- [X] Unit tests pass with >90% coverage
- [ ] Performance tests verify virtual thread efficiency (deferred to later phase)

---

### Step 6: Create HandlerMetadataGenerator

**Model:** opus
**Agent:** sdd:developer
**Depends on:** Step 3
**Parallel with:** Step 5 (after Step 3 completes)

**Goal**: Implement Gizmo-based bytecode generation for $Metadata classes.

**Complexity**: Medium
**Uncertainty**: Medium
**Level**: 2
**Depends On**: Step 3

#### Expected Output

- `support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/HandlerMetadataGenerator.java`
- `support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/CommandHandlerBuildItem.java`

#### Success Criteria

- [ ] HandlerMetadataGenerator generates {HandlerClass}$Metadata class for each handler
- [ ] Generated class implements HandlerMetadata<C, R> interface
- [ ] Generated commandType() method returns Class<C>
- [ ] Generated resultType() method returns Class<R>
- [ ] Generated handlerName() method returns handler class name
- [ ] Generated class is registered as @Singleton CDI bean

#### Verification

**Level:** ✅ CRITICAL - Panel of 2 Judges with Aggregated Voting
**Artifacts:**
- `support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/HandlerMetadataGenerator.java`
- `support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/CommandHandlerBuildItem.java`
**Threshold:** 4.0/5.0

**Rubric:**

| Criterion | Weight | Description |
|-----------|--------|-------------|
| Bytecode Correctness | 0.30 | Generated bytecode passes verification, no illegal instructions |
| Interface Compliance | 0.25 | Generated classes correctly implement HandlerMetadata interface |
| CDI Registration | 0.20 | Generated classes properly registered as @Singleton beans |
| Naming Convention | 0.15 | Consistent {HandlerClass}$Metadata naming pattern |
| Build Integration | 0.10 | Proper integration with Quarkus build items and Gizmo |

#### Subtasks

- [ ] Create CommandHandlerBuildItem for build-time data transfer
- [ ] Create HandlerMetadataGenerator class
- [ ] Implement generate() method using Gizmo ClassCreator
- [ ] Implement commandType() method generation
- [ ] Implement resultType() method generation
- [ ] Implement handlerName() method generation
- [ ] Add @Singleton annotation to generated class
- [ ] Write unit tests for metadata generation

#### Blockers

- Step 3 must complete (HandlerTypeResolver needed for type information)

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Gizmo bytecode generation errors | High | Medium | Extensive testing, validate generated bytecode |
| Class name collisions | Medium | Low | Use predictable naming pattern with handler class name |

#### Definition of Done

- [ ] HandlerMetadataGenerator implemented
- [ ] Generated classes pass bytecode verification
- [ ] Unit tests verify generated class behavior
- [ ] Integration with Quarkus build step works

---

### Step 7: Update Quarkus Extension

**Model:** opus
**Agent:** sdd:developer
**Depends on:** Steps 3, 4, 6
**Parallel with:** None

**Goal**: Update Quarkus extension with build steps for interface scanning, metadata generation, and CDI registration.

**Complexity**: Large (broken into sub-steps)
**Uncertainty**: High
**Level**: 3
**Depends On**: Steps 3, 4, 6

#### Expected Output

- Updated `support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/MessageQuarkusProcessor.java`
- `support/quarkus/tower-quarkus/src/main/java/io/iamcyw/tower/quarkus/runtime/QuarkusHandlerRegistry.java`
- Updated `support/quarkus/tower-quarkus/src/main/java/io/iamcyw/tower/quarkus/runtime/MessageRecorder.java`

#### Success Criteria

- [X] MessageQuarkusProcessor has @BuildStep for scanning CommandHandler implementors
- [X] MessageQuarkusProcessor has @BuildStep for generating metadata classes
- [X] MessageQuarkusProcessor produces AdditionalBeanBuildItem for generated metadata
- [X] QuarkusHandlerRegistry uses CDI Instance to collect handlers and metadata
- [X] QuarkusHandlerRegistry implements HandlerRegistry interface
- [ ] MethodInvokerFactory adapted for handle() method invocation (deferred to Step 8)

#### Verification

**Level:** ✅ CRITICAL - Panel of 2 Judges with Aggregated Voting
**Artifacts:**
- `support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/MessageQuarkusProcessor.java`
- `support/quarkus/tower-quarkus/src/main/java/io/iamcyw/tower/quarkus/runtime/QuarkusHandlerRegistry.java`
- `support/quarkus/tower-quarkus/src/main/java/io/iamcyw/tower/quarkus/runtime/MessageRecorder.java`
**Threshold:** 4.0/5.0

**Rubric:**

| Criterion | Weight | Description |
|-----------|--------|-------------|
| Build Step Correctness | 0.25 | All @BuildStep methods execute in correct order with proper dependencies |
| CDI Integration | 0.25 | Proper bean registration, @All injection works, no circular dependencies |
| Dev Mode Support | 0.15 | Hot reload works correctly for handler changes |
| Native Image | 0.15 | No reflection, compatible with GraalVM native image compilation |
| Error Handling | 0.10 | Clear error messages for build-time failures |
| Test Coverage | 0.10 | Integration tests for JVM and native modes |

#### Subtasks

- [X] Create QuarkusHandlerRegistry with CDI Instance injection
- [X] Update MessageQuarkusProcessor with scanCommandHandlers() @BuildStep
- [X] Update MessageQuarkusProcessor with generateHandlerMetadata() @BuildStep
- [X] Update MessageQuarkusProcessor with registerHandlerBeans() @BuildStep
- [ ] Update MethodInvokerFactory for handle() method (deferred to Step 8)
- [ ] Update MessageRecorder for registry initialization (deferred to Step 8)
- [ ] Write integration tests for build steps (deferred to Step 8)

#### Blockers

- Step 3 must complete (HandlerTypeResolver)
- Step 4 should complete (HandlerRegistry interface)
- Step 6 must complete (HandlerMetadataGenerator)

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Build step ordering issues | High | Medium | Use proper BuildItem dependencies |
| CDI bean discovery timing | High | Medium | Use AdditionalBeanBuildItem for generated classes |
| Quarkus dev mode hot reload | Medium | Medium | Test thoroughly in dev mode |

#### Definition of Done

- [X] All build steps working correctly
- [X] QuarkusHandlerRegistry functional
- [X] Integration tests pass in JVM mode
- [ ] Integration tests pass in native mode (deferred to Step 8)

---

### Step 8: Migrate Tests and Validate

**Model:** opus
**Agent:** sdd:developer
**Depends on:** Steps 1, 2, 3, 4, 5, 6, 7
**Parallel with:** None

**Goal**: Migrate existing tests to interface-based style and add comprehensive test coverage.

**Complexity**: Medium
**Uncertainty**: Low
**Level**: 3
**Depends On**: Steps 1-7

#### Expected Output

- Updated `support/quarkus/integration-tests/src/main/java/io/iamcyw/tower/quarkus/it/TestService.java`
- Updated `messaging-core/src/test/java/io/iamcyw/tower/messaging/handle/CommandTest.java`
- New comprehensive tests for all features

#### Success Criteria

- [X] TestService converted to implement CommandHandler interface
- [X] CommandTest has comprehensive tests for interface-based handlers
- [X] All existing integration tests pass (core modules)
- [X] New tests cover: single dispatch, conditional routing, batch execution
- [ ] Performance tests verify <1ms dispatch latency (deferred)
- [ ] Native image tests pass (blocked by Quarkus test infrastructure)

#### Verification

**Level:** ✅ Per-Test-Category Judges (3 separate evaluations in parallel)
**Artifacts:**
- `support/quarkus/integration-tests/src/main/java/io/iamcyw/tower/quarkus/it/TestService.java`
- `messaging-core/src/test/java/io/iamcyw/tower/messaging/handle/CommandTest.java`
- Additional test files for batch execution, conditional routing, performance
**Threshold:** 4.0/5.0

**Rubric (per test category):**

| Criterion | Weight | Description |
|-----------|--------|-------------|
| Coverage | 0.25 | Tests cover single dispatch, conditional routing, batch execution |
| Migration Completeness | 0.25 | All existing tests migrated from annotation to interface style |
| Edge Cases | 0.20 | Error scenarios, timeout, cancellation tested |
| Performance | 0.15 | Benchmarks verify <1ms dispatch latency |
| Native Image | 0.15 | Tests pass in both JVM and native modes |

#### Subtasks

- [X] Convert TestService from @UseCase to CommandHandler implementation
- [X] Update CommandTest with interface-based handler tests
- [X] Add tests for conditional routing with canHandle()
- [X] Add tests for sequential batch execution
- [X] Add tests for parallel batch execution
- [X] Add tests for structured concurrency execution
- [ ] Add performance benchmarks (deferred)
- [ ] Verify native image compatibility (blocked by Quarkus test infrastructure)

#### Blockers

- Steps 1-7 must complete

#### Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Test migration reveals design issues | Medium | Medium | Iterate on design if needed |
| Performance regression | High | Low | Benchmark before and after |

#### Definition of Done

- [X] All tests migrated and passing (core modules: 50/50 tests pass)
- [X] Test coverage >80% for new code
- [ ] Performance benchmarks meet criteria (deferred)
- [ ] Native image tests pass (blocked by Quarkus test infrastructure)

---

## Verification Summary

| Step | Verification Level | Judges | Threshold | Artifacts |
|------|-------------------|--------|-----------|-----------|
| 1 | ✅ Panel (2) | 2 | 4.0/5.0 | Core interfaces (Command, CommandHandler, HandlerMetadata) |
| 2 | ❌ None | 0 | - | Legacy annotation deprecation |
| 3 | ✅ Panel (2) | 2 | 4.0/5.0 | HandlerTypeResolver, HandlerTypeInfo |
| 4 | ✅ Panel (2) | 2 | 4.0/5.0 | HandlerRegistry, DefaultHandlerRegistry |
| 5 | ✅ Panel (2) | 2 | 4.0/5.0 | BatchCommandExecutor, VirtualThreadBatchExecutor |
| 6 | ✅ Panel (2) | 2 | 4.0/5.0 | HandlerMetadataGenerator, CommandHandlerBuildItem |
| 7 | ✅ Panel (2) | 2 | 4.0/5.0 | Quarkus extension integration |
| 8 | ✅ Per-Item (3) | 3 | 4.0/5.0 | Test migration categories |

**Total Evaluations:** 15 (12 Panel + 3 Per-Item)

**Implementation Command:** `/implement /Users/walkin/SourceCode/citywalki/tower-messaging/.specs/tasks/draft/interface-style-handler.refactor.md`

---

## Implementation Summary

| Step | Goal | Output | Level | Effort | Uncertainty |
|------|------|--------|-------|--------|-------------|
| 1 | Create core interfaces | Command, CommandHandler, HandlerMetadata interfaces | 0 | Small | Low |
| 2 | Deprecate legacy annotations | @Deprecated on @UseCase, @CommandHandle, @Predicate | 0 | Small | Low |
| 3 | Create HandlerTypeResolver | Type extraction from generics | 1 | Medium | Medium |
| 4 | Create HandlerRegistry | Runtime handler lookup | 1 | Medium | Low |
| 5 | Create BatchCommandExecutor | Sequential, parallel, structured execution | 2 | Medium | Medium |
| 6 | Create HandlerMetadataGenerator | Gizmo metadata generation | 2 | Medium | Medium |
| 7 | Update Quarkus Extension | Build steps and CDI integration | 3 | Large | High |
| 8 | Migrate Tests and Validate | Test migration and validation | 3 | Medium | Low |

**Total Steps**: 8
**Critical Path**: Steps 1 → 3 → 6 → 7 → 8
**Parallel Opportunities**: Steps 1 & 2; Steps 3 & 4; Steps 5 & 6

---

## Risks & Blockers Summary

### High Priority

| Risk/Blocker | Impact | Likelihood | Mitigation |
|--------------|--------|------------|------------|
| Generic type extraction complexity | High | Medium | Extensive unit testing, recursive superclass checking |
| Gizmo bytecode generation errors | High | Medium | Validate generated bytecode, incremental testing |
| Quarkus build step integration | High | Medium | Follow Quarkus extension patterns, test in dev mode |
| CDI circular dependencies | Medium | Medium | Constructor injection, lazy resolution |

### Medium Priority

| Risk/Blocker | Impact | Likelihood | Mitigation |
|--------------|--------|------------|------------|
| Virtual thread pinning | Medium | Medium | Document best practices for handler implementations |
| Test migration complexity | Medium | Medium | Incremental migration, maintain test coverage |
| Performance regression | High | Low | Benchmark before and after, optimize critical path |

---

## Definition of Done (Task Level)

- [ ] All implementation steps completed
- [ ] All acceptance criteria verified
- [ ] All unit tests pass across all modules
- [ ] Quarkus integration tests pass in JVM mode
- [ ] Quarkus integration tests pass in native mode
- [ ] No runtime reflection calls in critical path (verified by static analysis)
- [ ] Documentation updated with migration guide
- [ ] Code reviewed and approved

## Technical Risks

1. **Build-Time Complexity**: Interface scanning is more complex than annotation scanning, requiring handling of generic inheritance and type erasure edge cases
2. **Metadata Generation Synchronization**: HandlerMetadata implementations must correspond one-to-one with CommandHandler implementations; generation logic must ensure no omissions
3. **CDI Circular Dependencies**: HandlerRegistry injecting Instance<CommandHandler> and Instance<HandlerMetadata> must avoid circular dependencies
4. **Performance Impact**: Batch command virtual thread execution requires monitoring thread creation overhead
5. **Bytecode Generation Complexity**: Using bytecode generation to create $Metadata classes and MethodInvoker classes increases build-time code generation complexity
