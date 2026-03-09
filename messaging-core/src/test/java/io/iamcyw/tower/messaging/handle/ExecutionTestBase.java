package io.iamcyw.tower.messaging.handle;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.DefaultHandlerRegistry;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

/**
 * Base class for execution tests providing common infrastructure.
 *
 * <p>This base class sets up the test infrastructure for interface-based handler
 * testing. It provides methods to create handler registries and metadata for tests.
 *
 * <p>Subclasses can use this base to test handler execution scenarios with
 * different command types and handler configurations.
 *
 * @since 2.0
 */
public class ExecutionTestBase {

    protected HandlerRegistry handlerRegistry;

    /**
     * Sets up the test environment before each test.
     *
     * <p>Subclasses can override this method to provide custom handler configurations.
     */
    @BeforeEach
    public void init() {
        // Default setup - subclasses should override for specific configurations
        handlerRegistry = new DefaultHandlerRegistry(List.of(), List.of());
    }

    /**
     * Creates a handler registry with the given handlers and metadata.
     *
     * @param handlers the list of handlers to register
     * @param metadata the list of metadata for the handlers
     * @return a configured HandlerRegistry
     */
    protected HandlerRegistry createRegistry(
            List<CommandHandler<?, ?>> handlers,
            List<HandlerMetadata<?, ?>> metadata) {
        return new DefaultHandlerRegistry(handlers, metadata);
    }

    /**
     * Creates metadata for a handler class.
     *
     * @param commandType the command type class
     * @param resultType the result type class
     * @param handlerClass the handler class
     * @param <C> the command type
     * @param <R> the result type
     * @return the handler metadata
     */
    @SuppressWarnings("unchecked")
    protected <C extends Command, R> HandlerMetadata<C, R> createMetadata(
            Class<C> commandType,
            Class<R> resultType,
            Class<? extends CommandHandler<C, R>> handlerClass) {
        return new HandlerMetadata<>() {
            @Override
            public Class<C> commandType() {
                return commandType;
            }

            @Override
            public Class<R> resultType() {
                return resultType;
            }

            @Override
            public String handlerName() {
                return handlerClass.getName();
            }

            @Override
            public Class<? extends CommandHandler<C, R>> handlerClass() {
                return handlerClass;
            }
        };
    }

}
