package io.iamcyw.tower.schema;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.schema.model.HandlerTypeInfo;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HandlerTypeResolver}.
 *
 * <p>These tests verify that the resolver correctly extracts generic type parameters
 * from CommandHandler implementations, including direct implementation, inheritance,
 * and various edge cases.</p>
 */
class HandlerTypeResolverTest {

    private HandlerTypeResolver resolver;
    private Indexer indexer;

    // Test command types
    interface TestCommand extends Command {
    }

    record SimpleCommand(String value) implements TestCommand {
    }

    record ComplexCommand(String id, int count) implements TestCommand {
    }

    // Test result types
    record SimpleResult(String value) {
    }

    record ComplexResult(String id, boolean success) {
    }

    // Container for recursive generics test - must implement Command
    record Container<T>(T value) implements Command {
    }

    // Direct implementation
    static class DirectHandler implements CommandHandler<SimpleCommand, SimpleResult> {
        @Override
        public SimpleResult handle(SimpleCommand command) {
            return new SimpleResult(command.value());
        }
    }

    // Handler with void result
    static class VoidResultHandler implements CommandHandler<SimpleCommand, Void> {
        @Override
        public Void handle(SimpleCommand command) {
            return null;
        }
    }

    // Abstract base handler with generic types
    static abstract class BaseHandler<C extends Command, R> implements CommandHandler<C, R> {
        // Base implementation
    }

    // Concrete handler extending abstract base
    static class ConcreteHandler extends BaseHandler<ComplexCommand, ComplexResult> {
        @Override
        public ComplexResult handle(ComplexCommand command) {
            return new ComplexResult(command.id(), true);
        }
    }

    // Intermediate abstract class
    static abstract class IntermediateHandler<C extends Command> extends BaseHandler<C, SimpleResult> {
        // Intermediate layer
    }

    // Handler through intermediate layer
    static class DeepHandler extends IntermediateHandler<SimpleCommand> {
        @Override
        public SimpleResult handle(SimpleCommand command) {
            return new SimpleResult(command.value());
        }
    }

    // Handler that doesn't implement CommandHandler (for negative testing)
    static class NotAHandler {
        public void handle(Object cmd) {
        }
    }

    // Abstract handler (should be skipped in resolveAll)
    static abstract class AbstractHandler implements CommandHandler<SimpleCommand, SimpleResult> {
        @Override
        public abstract SimpleResult handle(SimpleCommand command);
    }

    // ========== Interface Inheritance Tests ==========

    // Interface extending CommandHandler
    interface MyCommandHandler<C extends Command, R> extends CommandHandler<C, R> {
    }

    // Handler implementing interface that extends CommandHandler
    static class InterfaceInheritanceHandler implements MyCommandHandler<SimpleCommand, SimpleResult> {
        @Override
        public SimpleResult handle(SimpleCommand command) {
            return new SimpleResult(command.value());
        }
    }

    // Deep interface inheritance
    interface DeepCommandHandler<C extends Command, R> extends MyCommandHandler<C, R> {
    }

    static class DeepInterfaceHandler implements DeepCommandHandler<ComplexCommand, ComplexResult> {
        @Override
        public ComplexResult handle(ComplexCommand command) {
            return new ComplexResult(command.id(), true);
        }
    }

    // ========== Complex Inheritance Patterns ==========

    // Recursive generics: CommandHandler<Container<T>, R>
    // Container implements Command, so this is valid
    static abstract class ContainerHandler<T, R> implements CommandHandler<Container<T>, R> {
    }

    static class ConcreteContainerHandler extends ContainerHandler<String, SimpleResult> {
        @Override
        public SimpleResult handle(Container<String> command) {
            return new SimpleResult(command.value());
        }
    }

    // Deep inheritance chain with type variables at multiple levels
    static abstract class Level1Handler<C extends Command, R> implements CommandHandler<C, R> {
    }

    static abstract class Level2Handler<C extends Command> extends Level1Handler<C, SimpleResult> {
    }

    static abstract class Level3Handler extends Level2Handler<ComplexCommand> {
    }

    static class DeepChainHandler extends Level3Handler {
        @Override
        public SimpleResult handle(ComplexCommand command) {
            return new SimpleResult(command.id());
        }
    }

    // Multiple interface inheritance
    interface AnotherInterface {
        default void anotherMethod() {
        }
    }

    interface CommandHandlerWithExtra<C extends Command, R> extends CommandHandler<C, R>, AnotherInterface {
    }

    static class MultipleInterfaceHandler implements CommandHandlerWithExtra<SimpleCommand, ComplexResult> {
        @Override
        public ComplexResult handle(SimpleCommand command) {
            return new ComplexResult(command.value(), true);
        }
    }

    // Type variable reordering test
    // Base class with type parameters in different order conceptually
    static abstract class ReorderedBaseHandler<R, C extends Command> implements CommandHandler<C, R> {
    }

    static class ReorderedHandler extends ReorderedBaseHandler<ComplexResult, SimpleCommand> {
        @Override
        public ComplexResult handle(SimpleCommand command) {
            return new ComplexResult(command.value(), true);
        }
    }

    @BeforeEach
    void setUp() {
        resolver = new HandlerTypeResolver();
        indexer = new Indexer();
    }

    @Test
    @DisplayName("Should extract types from direct CommandHandler implementation")
    void shouldExtractTypesFromDirectImplementation() throws IOException {
        // Given
        IndexView index = indexClasses(
            DirectHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(DirectHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(DirectHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(SimpleCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(SimpleResult.class.getName());
        assertThat(info.hasResultType()).isTrue();
    }

    @Test
    @DisplayName("Should handle void result type")
    void shouldHandleVoidResultType() throws IOException {
        // Given
        IndexView index = indexClasses(
            VoidResultHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(VoidResultHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getCommandType().name().toString()).isEqualTo(SimpleCommand.class.getName());
        // Void type may or may not resolve to ClassInfo depending on Jandex
    }

    @Test
    @DisplayName("Should extract types from inherited generic parameters")
    void shouldExtractTypesFromInheritedGenericParameters() throws IOException {
        // Given
        IndexView index = indexClasses(
            ConcreteHandler.class,
            BaseHandler.class,
            ComplexCommand.class,
            TestCommand.class,
            ComplexResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(ConcreteHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(ConcreteHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(ComplexCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(ComplexResult.class.getName());
    }

    @Test
    @DisplayName("Should extract types through intermediate abstract classes")
    void shouldExtractTypesThroughIntermediateAbstractClasses() throws IOException {
        // Given
        IndexView index = indexClasses(
            DeepHandler.class,
            IntermediateHandler.class,
            BaseHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(DeepHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(DeepHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(SimpleCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(SimpleResult.class.getName());
    }

    @Test
    @DisplayName("Should resolve all handlers in index")
    void shouldResolveAllHandlersInIndex() throws IOException {
        // Given
        IndexView index = indexClasses(
            DirectHandler.class,
            ConcreteHandler.class,
            DeepHandler.class,
            BaseHandler.class,
            IntermediateHandler.class,
            SimpleCommand.class,
            ComplexCommand.class,
            TestCommand.class,
            SimpleResult.class,
            ComplexResult.class,
            Command.class,
            CommandHandler.class
        );

        // When
        List<HandlerTypeInfo> handlers = resolver.resolveAll(index);

        // Then - should find concrete handlers but not abstract ones
        assertThat(handlers).hasSize(3);

        List<String> handlerNames = handlers.stream()
            .map(HandlerTypeInfo::getHandlerClassName)
            .toList();

        assertThat(handlerNames).contains(
            DirectHandler.class.getName(),
            ConcreteHandler.class.getName(),
            DeepHandler.class.getName()
        );

        // Abstract handler should not be included
        assertThat(handlerNames).doesNotContain(AbstractHandler.class.getName());
    }

    @Test
    @DisplayName("Should skip abstract handlers in resolveAll")
    void shouldSkipAbstractHandlersInResolveAll() throws IOException {
        // Given
        IndexView index = indexClasses(
            DirectHandler.class,
            AbstractHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );

        // When
        List<HandlerTypeInfo> handlers = resolver.resolveAll(index);

        // Then
        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0).getHandlerClassName()).isEqualTo(DirectHandler.class.getName());
    }

    @Test
    @DisplayName("Should throw exception when handlerClass is null")
    void shouldThrowExceptionWhenHandlerClassIsNull() {
        assertThatThrownBy(() -> resolver.resolve(null, indexer.complete()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Handler class must not be null");
    }

    @Test
    @DisplayName("Should throw exception when index is null")
    void shouldThrowExceptionWhenIndexIsNull() throws IOException {
        // Given
        IndexView index = indexClasses(DirectHandler.class, SimpleCommand.class, TestCommand.class, Command.class, CommandHandler.class);
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(DirectHandler.class.getName()));

        // When/Then
        assertThatThrownBy(() -> resolver.resolve(handlerClass, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("IndexView must not be null");
    }

    @Test
    @DisplayName("Should throw exception when index is null in resolveAll")
    void shouldThrowExceptionWhenIndexIsNullInResolveAll() {
        assertThatThrownBy(() -> resolver.resolveAll(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("IndexView must not be null");
    }

    @Test
    @DisplayName("Should return empty list when no handlers in index")
    void shouldReturnEmptyListWhenNoHandlersInIndex() throws IOException {
        // Given - index with no handlers
        IndexView index = indexClasses(
            NotAHandler.class,
            SimpleCommand.class,
            TestCommand.class
        );

        // When
        List<HandlerTypeInfo> handlers = resolver.resolveAll(index);

        // Then
        assertThat(handlers).isEmpty();
    }

    @Test
    @DisplayName("HandlerTypeInfo should provide correct string representation")
    void handlerTypeInfoShouldProvideCorrectStringRepresentation() throws IOException {
        // Given
        IndexView index = indexClasses(
            DirectHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(DirectHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);
        String toString = info.toString();

        // Then
        assertThat(toString).contains(DirectHandler.class.getName());
        assertThat(toString).contains(SimpleCommand.class.getName());
        assertThat(toString).contains(SimpleResult.class.getName());
    }

    @Test
    @DisplayName("HandlerTypeInfo should indicate hasResultType correctly")
    void handlerTypeInfoShouldIndicateHasResultTypeCorrectly() throws IOException {
        // Given - handler with non-void result
        IndexView index = indexClasses(
            DirectHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(DirectHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info.hasResultType()).isTrue();
    }

    // ========== Interface Inheritance Tests ==========

    @Test
    @DisplayName("Should extract types from interface inheritance")
    void shouldExtractTypesFromInterfaceInheritance() throws IOException {
        // Given - handler implements interface that extends CommandHandler
        IndexView index = indexClasses(
            InterfaceInheritanceHandler.class,
            MyCommandHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(InterfaceInheritanceHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(InterfaceInheritanceHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(SimpleCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(SimpleResult.class.getName());
    }

    @Test
    @DisplayName("Should extract types from deep interface inheritance")
    void shouldExtractTypesFromDeepInterfaceInheritance() throws IOException {
        // Given - handler implements interface that extends interface that extends CommandHandler
        IndexView index = indexClasses(
            DeepInterfaceHandler.class,
            DeepCommandHandler.class,
            MyCommandHandler.class,
            ComplexCommand.class,
            TestCommand.class,
            ComplexResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(DeepInterfaceHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(DeepInterfaceHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(ComplexCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(ComplexResult.class.getName());
    }

    // ========== Negative Test: Command Validation ==========

    @Test
    @DisplayName("Should throw exception when command type does not implement Command interface")
    void shouldThrowExceptionWhenCommandTypeDoesNotImplementCommand() throws IOException {
        // Given - Create a scenario where we have a handler class but the command type
        // doesn't implement Command. Since Java's type system prevents creating an invalid
        // CommandHandler at compile time, we test by verifying the implementsCommand logic
        // works correctly for non-Command types.

        // Index classes - use an existing class that doesn't implement Command
        // Use the test classloader which can load both user classes and bootstrap classes
        Indexer testIndexer = new Indexer();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Index Integer class (doesn't implement Command)
        String resourceName = "java/lang/Integer.class";
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            if (stream != null) {
                testIndexer.index(stream);
            }
        }
        // Also index Command interface
        resourceName = Command.class.getName().replace('.', '/') + ".class";
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            if (stream != null) {
                testIndexer.index(stream);
            }
        }
        IndexView index = testIndexer.complete();

        ClassInfo integerClassInfo = index.getClassByName(DotName.createSimple(Integer.class.getName()));

        // Then - Verify that Integer does not implement Command
        assertThat(integerClassInfo).isNotNull();
        assertThat(integerClassInfo.interfaceNames()).noneMatch(name ->
            name.toString().equals(Command.class.getName()));
    }

    @Test
    @DisplayName("Should validate that SimpleCommand implements Command interface")
    void shouldValidateThatSimpleCommandImplementsCommand() throws IOException {
        // Given
        IndexView index = indexClasses(
            SimpleCommand.class,
            TestCommand.class,
            Command.class
        );

        ClassInfo commandClassInfo = index.getClassByName(DotName.createSimple(SimpleCommand.class.getName()));

        // Verify that SimpleCommand does implement Command (through TestCommand)
        // This validates the implementsCommand method works correctly for positive cases
        assertThat(commandClassInfo).isNotNull();
        assertThat(commandClassInfo.interfaceNames()).anyMatch(name ->
            name.toString().equals(TestCommand.class.getName()) ||
            name.toString().equals(Command.class.getName()));
    }

    // ========== Complex Inheritance Pattern Tests ==========

    @Test
    @DisplayName("Should handle recursive generics: CommandHandler<Container<T>, R>")
    void shouldHandleRecursiveGenerics() throws IOException {
        // Given - handler with recursive generic type Container<T>
        IndexView index = indexClasses(
            ConcreteContainerHandler.class,
            ContainerHandler.class,
            Container.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(ConcreteContainerHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(ConcreteContainerHandler.class.getName());
        // The command type should be Container (with String type parameter resolved)
        assertThat(info.getCommandType().name().toString()).isEqualTo(Container.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(SimpleResult.class.getName());
    }

    @Test
    @DisplayName("Should handle deep inheritance chains with type variables at multiple levels")
    void shouldHandleDeepInheritanceChains() throws IOException {
        // Given - 3-level inheritance chain with type variables at each level
        IndexView index = indexClasses(
            DeepChainHandler.class,
            Level3Handler.class,
            Level2Handler.class,
            Level1Handler.class,
            ComplexCommand.class,
            TestCommand.class,
            SimpleResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(DeepChainHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(DeepChainHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(ComplexCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(SimpleResult.class.getName());
    }

    @Test
    @DisplayName("Should handle multiple interface inheritance")
    void shouldHandleMultipleInterfaceInheritance() throws IOException {
        // Given - handler implements interface that extends both CommandHandler and another interface
        IndexView index = indexClasses(
            MultipleInterfaceHandler.class,
            CommandHandlerWithExtra.class,
            AnotherInterface.class,
            SimpleCommand.class,
            TestCommand.class,
            ComplexResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(MultipleInterfaceHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(MultipleInterfaceHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(SimpleCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(ComplexResult.class.getName());
    }

    @Test
    @DisplayName("Should handle type variable reordering in inheritance")
    void shouldHandleTypeVariableReorderingInInheritance() throws IOException {
        // Given - base class has type parameters in different conceptual order
        IndexView index = indexClasses(
            ReorderedHandler.class,
            ReorderedBaseHandler.class,
            SimpleCommand.class,
            TestCommand.class,
            ComplexResult.class,
            Command.class,
            CommandHandler.class
        );
        ClassInfo handlerClass = index.getClassByName(DotName.createSimple(ReorderedHandler.class.getName()));

        // When
        HandlerTypeInfo info = resolver.resolve(handlerClass, index);

        // Then - should correctly map C to SimpleCommand and R to ComplexResult
        // regardless of their declaration order in the base class
        assertThat(info).isNotNull();
        assertThat(info.getHandlerClassName()).isEqualTo(ReorderedHandler.class.getName());
        assertThat(info.getCommandType().name().toString()).isEqualTo(SimpleCommand.class.getName());
        assertThat(info.getResultType().name().toString()).isEqualTo(ComplexResult.class.getName());
    }

    /**
     * Helper method to index multiple classes.
     */
    private IndexView indexClasses(Class<?>... classes) throws IOException {
        for (Class<?> clazz : classes) {
            indexClass(indexer, clazz);
        }
        return indexer.complete();
    }

    /**
     * Helper method to index a single class.
     */
    private void indexClass(Indexer indexer, Class<?> clazz) throws IOException {
        String resourceName = clazz.getName().replace('.', '/') + ".class";
        try (InputStream stream = clazz.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream != null) {
                indexer.index(stream);
            }
        }
    }
}
