---
title: Codebase Impact Analysis - Refactor Tower Messaging from annotation-driven to interface-based CommandHandler
task_file: .specs/tasks/draft/interface-style-handler.refactor.md
scratchpad: .specs/scratchpad/ed6e930d.md
created: 2026-03-08
status: complete
---

# Codebase Impact Analysis: Refactor Tower Messaging from annotation-driven to interface-based CommandHandler

## Summary

- **Files to Modify**: 11 files
- **Files to Create**: 10 files
- **Files to Deprecate**: 3 files
- **Test Files Affected**: 2 files
- **Risk Level**: High

This refactoring changes the fundamental programming model of Tower Messaging from annotation-driven (@UseCase, @CommandHandle) to interface-based (CommandHandler<C, R>). The change affects build-time processing (Jandex scanning), runtime dispatch, and Quarkus integration.

---

## Files to be Modified/Created

### Primary Changes

```
messaging-core/src/main/java/io/iamcyw/tower/messaging/
├── Command.java                           # NEW: Marker interface for commands
├── CommandHandler.java                    # NEW: Core interface with handle() and canHandle()
├── HandlerMetadata.java                   # NEW: Build-time metadata interface
├── HandlerRegistry.java                   # NEW: Runtime handler lookup interface
├── BatchCommandExecutor.java              # NEW: Batch execution support
├── UseCase.java                           # UPDATE: Mark @Deprecated(forRemoval = true)
├── CommandHandle.java                     # UPDATE: Mark @Deprecated(forRemoval = true)
├── Predicate.java                         # UPDATE: Mark @Deprecated(forRemoval = true)
├── DefaultMessageBus.java                 # UPDATE: Adapt to interface-based routing
└── bootstrap/
    └── Bootstrap.java                     # UPDATE: Support interface-based handlers

schema/schema-builder/src/main/java/io/iamcyw/tower/schema/
├── SchemaBuilder.java                     # UPDATE: Scan CommandHandler implementors
├── Annotations.java                       # UPDATE: Add CommandHandler DotName
├── HandlerTypeResolver.java               # NEW: Extract generic types from interface
└── creator/
    └── OperationCreator.java              # UPDATE: Create from handler class

support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/
├── MessageQuarkusProcessor.java           # UPDATE: Major refactoring for interface scanning
├── MethodInvokerFactory.java              # UPDATE: Adapt to handle() method
├── CommandHandlerBuildItem.java           # NEW: Build item for handler info
└── HandlerMetadataGenerator.java          # NEW: Generate $Metadata classes

support/quarkus/tower-quarkus/src/main/java/io/iamcyw/tower/quarkus/runtime/
├── QuarkusHandlerRegistry.java            # NEW: CDI-based handler registry
└── MessageRecorder.java                   # UPDATE: May need adjustments
```

### Test Files

```
support/quarkus/integration-tests/src/main/java/io/iamcyw/tower/quarkus/it/
└── TestService.java                       # UPDATE: Convert to interface style

messaging-core/src/test/java/io/iamcyw/tower/messaging/
└── handle/
    └── CommandTest.java                   # UPDATE: Add comprehensive tests
```

---

## Useful Resources for Implementation

### Pattern References

The current codebase already uses several patterns that should be followed:

1. **MethodInvoker Generation** (`MethodInvokerFactory.java:27-62`):
   - Uses Gizmo to generate bytecode invokers
   - Pattern: Create class name with hash, generate `invoke(Object, Object[])` method

2. **Schema Building** (`SchemaBuilder.java:34-59`):
   - Uses Jandex IndexView for class scanning
   - Pattern: Register index, create creators, generate schema

3. **CDI Integration** (`MessageProducer.java:19-64`):
   - Uses @Produces for gateway/schema/bootstrap
   - Pattern: Initialize at runtime with recorder

4. **Build-Time Processing** (`MessageQuarkusProcessor.java:82-123`):
   - Uses @BuildStep for Quarkus deployment
   - Pattern: Scan index, create schema, produce build items

---

## Key Interfaces & Contracts

### New Core Interface

**Location**: `messaging-core/src/main/java/io/iamcyw/tower/messaging/CommandHandler.java` (NEW)

```java
public interface CommandHandler<C extends Command, R> {
    /**
     * Process command and return result
     */
    R handle(C command);

    /**
     * Default condition check: whether this handler can process the command
     * Subclasses can override for conditional routing (replaces @Predicate)
     */
    default boolean canHandle(C command) {
        return true;
    }
}
```

### New Metadata Interface

**Location**: `messaging-core/src/main/java/io/iamcyw/tower/messaging/HandlerMetadata.java` (NEW)

```java
public interface HandlerMetadata<C extends Command, R> {
    Class<C> commandType();
    Class<R> resultType();
    String handlerName();
}
```

### Modified Classes

| Location | Name | Current | Change Required |
|----------|------|---------|-----------------|
| `messaging-core/.../DefaultMessageBus.java:37-50` | `route()` | Uses `Bootstrap.getCommandHandles()` Multimap | Use `HandlerRegistry` to look up handlers by command type |
| `schema-builder/.../SchemaBuilder.java:43-59` | `generateSchema()` | Scans `@UseCase` annotations | Scan `CommandHandler` implementors via `index.getAllKnownImplementors()` |
| `schema-builder/.../OperationCreator.java:86-144` | `createOperation()` | Creates from `MethodInfo` | Create from `ClassInfo` with extracted handle method |
| `quarkus-deployment/.../MessageQuarkusProcessor.java:47-51` | `additionalBeanDefiningAnnotation()` | Registers `@UseCase` | Register `CommandHandler` interface |

---

## Integration Points

### Files That Interact with Affected Code

| File | Relationship | Impact | Action Needed |
|------|--------------|--------|---------------|
| `messaging-core/.../handle/MessageHandle.java:11-51` | Uses Operation for invocation | High | May need adapter for interface-based handlers |
| `messaging-core/.../handle/helper/OperationInvoker.java:9-36` | Invokes methods via Operation | High | Create new invoker for CommandHandler interface |
| `messaging-cdi/.../producer/MessageProducer.java:19-64` | Produces Bootstrap from Schema | Medium | Ensure compatibility with new handler model |
| `messaging-cdi/.../CDIMessageBus.java` | Extends DefaultMessageBus | Medium | Verify async handling works with new model |
| `quarkus-runtime/.../MessageRecorder.java:12-30` | Initializes messaging service | Medium | May need to initialize HandlerRegistry |

---

## Similar Implementations

### Current Annotation-Based Handler Pattern

**Location**: `support/quarkus/integration-tests/src/main/java/io/iamcyw/tower/quarkus/it/TestService.java:9-28`

```java
@UseCase
public class TestService {
    @CommandHandle
    @Parameter(value = "test", parameter = "payload")
    public void command(BasicTestCommand basicTestCommand) { ... }

    @Predicate
    public boolean testPredicate(BasicTestCommand payload, @Parameter("test") String type) { ... }
}
```

**Key Learning**: One class can have multiple handlers and predicates. The @Parameter annotation maps metadata to method parameters.

### Current Schema Building Pattern

**Location**: `schema/schema-builder/src/main/java/io/iamcyw/tower/schema/SchemaBuilder.java:43-59`

```java
public Schema generateSchema() {
    Collection<AnnotationInstance> useCaseAnnotations = ScanningContext.getIndex()
                                                                       .getAnnotations(Annotations.USECASE);
    final Schema schema = new Schema();
    for (AnnotationInstance graphQLApiAnnotation : useCaseAnnotations) {
        ClassInfo apiClass = graphQLApiAnnotation.target().asClass();
        List<MethodInfo> methods = apiClass.methods();
        addOperations(schema, methods);
    }
    // ...
}
```

**Key Learning**: Uses Jandex to scan annotations, then processes methods to create Operations.

### Current Gizmo Generation Pattern

**Location**: `support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/MethodInvokerFactory.java:27-62`

```java
public String create(ClassInfo currentClassInfo, MethodInfo info) {
    String baseName = currentClassInfo.name() + "$" + methodName + "$" + commandName + "_" + HashUtil.sha1(...);
    try (ClassCreator classCreator = new ClassCreator(
            new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
            baseName, null, Object.class.getName(), MethodInvoker.class.getName())) {
        MethodCreator mc = classCreator.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
        // ... generate invoke method
    }
    return baseName;
}
```

**Key Learning**: Generates classes at build time that implement MethodInvoker interface.

---

## Test Coverage

### Existing Tests to Update

| Test File | Tests Affected | Update Required |
|-----------|----------------|-----------------|
| `support/quarkus/integration-tests/src/test/java/io/iamcyw/tower/quarkus/it/BasicTest.java` | All tests | Update to use new handler style |
| `messaging-core/src/test/java/io/iamcyw/tower/messaging/handle/CommandTest.java` | Currently empty | Add comprehensive tests |

### New Tests Needed

| Test Type | Location | Coverage Target |
|-----------|----------|-----------------|
| Unit | `messaging-core/src/test/.../CommandHandlerTest.java` | Interface contract compliance |
| Unit | `schema-builder/src/test/.../HandlerTypeResolverTest.java` | Generic type extraction |
| Integration | `quarkus-deployment/src/test/.../HandlerMetadataGeneratorTest.java` | Gizmo generation |
| Integration | `integration-tests/src/test/.../InterfaceStyleTest.java` | End-to-end interface style |

---

## Risk Assessment

### High Risk Areas

| Area | Risk | Mitigation |
|------|------|------------|
| Generic Type Resolution | Type erasure may lose C and R types at runtime | Use Jandex at build time, store in HandlerMetadata |
| CDI Circular Dependencies | HandlerRegistry depends on Instance<CommandHandler> which depends on registry | Use @Inject constructor with Instance, lazy resolution |
| Backward Compatibility | Existing @UseCase code will break | Deprecate but don't remove; provide migration guide |
| Gizmo Generation | Complex bytecode generation for $Metadata classes | Thorough unit testing of generated code |
| Build-Time Performance | Interface scanning may be slower than annotation scanning | Cache scan results, optimize Jandex queries |

### Medium Risk Areas

| Area | Risk | Mitigation |
|------|------|------------|
| Predicate Migration | @Predicate logic needs to move to canHandle() | Document migration pattern clearly |
| Batch Execution | Virtual threads and structured concurrency are new | Extensive testing with various load patterns |
| Quarkus Dev Mode | Hot reload may not detect handler changes | Test dev mode thoroughly |

---

## Recommended Exploration

Before implementation, developer should read:

1. **`messaging-core/src/main/java/io/iamcyw/tower/messaging/DefaultMessageBus.java`** (lines 37-50)
   - Understand current routing logic
   - Critical for adapting to HandlerRegistry

2. **`schema/schema-builder/src/main/java/io/iamcyw/tower/schema/SchemaBuilder.java`** (lines 43-59)
   - Understand current annotation scanning
   - Critical for implementing interface scanning

3. **`support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/MessageQuarkusProcessor.java`** (lines 47-51, 82-123)
   - Understand build-time processing
   - Critical for implementing new BuildSteps

4. **`support/quarkus/tower-quarkus-deployment/src/main/java/io/iamcyw/tower/quarkus/deployment/MethodInvokerFactory.java`** (lines 27-62)
   - Understand Gizmo code generation
   - Critical for generating HandlerMetadata classes

5. **`messaging-core/src/main/java/io/iamcyw/tower/messaging/bootstrap/Bootstrap.java`** (lines 38-54)
   - Understand how Schema becomes MessageHandle instances
   - Critical for adapting to interface-based handlers

---

## Architecture Comparison

### Current Architecture (Annotation-Driven)

```
@UseCase class
    └── @CommandHandle methods
            └── Operation (Schema)
                    └── MessageHandle (Bootstrap)
                            └── OperationInvoker
                                    └── MethodInvoker (Gizmo/reflection)
```

### New Architecture (Interface-Based)

```
CommandHandler<C, R> implementation
    └── handle(C) method
            └── HandlerMetadata (build-time generated)
                    └── HandlerRegistry (runtime)
                            └── Direct invocation (no reflection)
```

### Key Differences

| Aspect | Current | New |
|--------|---------|-----|
| Discovery | @UseCase annotation scanning | CommandHandler interface implementors |
| Handler Definition | Methods with @CommandHandle | Classes implementing CommandHandler |
| Type Safety | Runtime via reflection | Compile-time via generics |
| Predicate | Separate @Predicate method | canHandle() default method |
| Metadata | Runtime reflection | Build-time generated HandlerMetadata |
| Batch Support | None | Built-in BatchCommandExecutor |

---

## Verification Summary

| Check | Status | Notes |
|-------|--------|-------|
| All affected files identified | ✅ | 11 modify, 10 create, 3 deprecate |
| Integration points mapped | ✅ | 5 integration points identified |
| Similar patterns found | ✅ | 3 reference patterns documented |
| Test coverage analyzed | ✅ | 2 existing tests to update, 4 new tests needed |
| Risks assessed | ✅ | 4 high risk, 3 medium risk areas |

### Limitations/Caveats

1. This analysis assumes the new `Command` marker interface will be created. Existing command classes may need to implement it.

2. The analysis does not cover potential changes to `MessageInterceptor` interface, which may need updates for the new model.

3. The `Operation` model class may need simplification as it currently supports multiple operation types (COMMAND, PREDICATE) but the new model separates these concerns.

4. Native image compatibility has been considered but not fully verified - the Gizmo-generated code should work but needs testing.
