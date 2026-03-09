package io.iamcyw.tower.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CommandNameResolver}.
 */
class CommandNameResolverTest {

    /**
     * Command class for testing auto-derived name (ends with Command).
     */
    static class CreateOrderCommand implements Command {
    }

    /**
     * Command class for testing auto-derived name (does not end with Command).
     */
    static class SimpleEvent implements Command {
    }

    /**
     * Command class with @CommandName annotation.
     */
    @CommandName("customName")
    static class AnnotatedCommand implements Command {
    }

    /**
     * Command class with empty annotation value.
     */
    @CommandName("")
    static class EmptyAnnotationCommand implements Command {
    }

    @Test
    @DisplayName("Default resolver should remove Command suffix from class name")
    void defaultResolverShouldRemoveCommandSuffix() {
        // Given
        CommandNameResolver resolver = CommandNameResolver.defaultResolver();

        // When
        String name = resolver.resolve(CreateOrderCommand.class);

        // Then
        assertThat(name).isEqualTo("CreateOrder");
    }

    @Test
    @DisplayName("Default resolver should use simple class name when no Command suffix")
    void defaultResolverShouldUseSimpleClassNameWhenNoSuffix() {
        // Given
        CommandNameResolver resolver = CommandNameResolver.defaultResolver();

        // When
        String name = resolver.resolve(SimpleEvent.class);

        // Then
        assertThat(name).isEqualTo("SimpleEvent");
    }

    @Test
    @DisplayName("Default resolver should prefer annotation value over class name")
    void defaultResolverShouldPreferAnnotationValue() {
        // Given
        CommandNameResolver resolver = CommandNameResolver.defaultResolver();

        // When
        String name = resolver.resolve(AnnotatedCommand.class);

        // Then
        assertThat(name).isEqualTo("customName");
    }

    @Test
    @DisplayName("Default resolver should return empty string for empty annotation value")
    void defaultResolverShouldReturnEmptyStringForEmptyAnnotation() {
        // Given
        CommandNameResolver resolver = CommandNameResolver.defaultResolver();

        // When
        String name = resolver.resolve(EmptyAnnotationCommand.class);

        // Then
        assertThat(name).isEmpty();
    }

    @Test
    @DisplayName("Default resolver should throw NullPointerException for null class")
    void defaultResolverShouldThrowExceptionForNullClass() {
        // Given
        CommandNameResolver resolver = CommandNameResolver.defaultResolver();

        // Then
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("commandClass");
    }

    @Test
    @DisplayName("Custom resolver should work correctly")
    void customResolverShouldWork() {
        // Given
        CommandNameResolver customResolver = clazz -> "CMD_" + clazz.getSimpleName().toUpperCase();

        // When
        String name = customResolver.resolve(CreateOrderCommand.class);

        // Then
        assertThat(name).isEqualTo("CMD_CREATEORDERCOMMAND");
    }

}
