package io.iamcyw.tower.quarkus.deployment.resolver;

import org.jboss.jandex.ClassInfo;

/**
 * Data holder for extracted handler type information.
 *
 * <p>This class holds the command type (C) and result type (R) extracted from a
 * {@code CommandHandler<C, R>} implementation, along with the handler class name.
 * It is used during build-time schema generation to resolve generic type parameters
 * from parameterized interfaces.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Given: class CreateOrderHandler implements CommandHandler&lt;CreateOrderCommand, OrderResult&gt;
 * HandlerTypeInfo info = resolver.resolve(handlerClass, index);
 *
 * // info.getCommandType() returns ClassInfo for CreateOrderCommand
 * // info.getResultType() returns ClassInfo for OrderResult
 * // info.getHandlerClassName() returns "com.example.CreateOrderHandler"
 * </pre>
 *
 * @see io.iamcyw.tower.messaging.CommandHandler
 * @see HandlerTypeResolver
 * @since 2.0
 */
public final class HandlerTypeInfo {

    private final ClassInfo commandType;
    private final ClassInfo resultType;
    private final String handlerClassName;

    /**
     * Creates a new HandlerTypeInfo with the extracted type information.
     *
     * @param commandType     the ClassInfo for the command type (C), must not be null
     * @param resultType      the ClassInfo for the result type (R), may be null for void results
     * @param handlerClassName the fully qualified name of the handler class, must not be null
     * @throws NullPointerException if commandType or handlerClassName is null
     */
    public HandlerTypeInfo(ClassInfo commandType, ClassInfo resultType, String handlerClassName) {
        this.commandType = commandType;
        this.resultType = resultType;
        this.handlerClassName = handlerClassName;
    }

    /**
     * Returns the ClassInfo for the command type (C) extracted from the
     * {@code CommandHandler<C, R>} interface.
     *
     * @return the command type ClassInfo, never null
     */
    public ClassInfo getCommandType() {
        return commandType;
    }

    /**
     * Returns the ClassInfo for the result type (R) extracted from the
     * {@code CommandHandler<C, R>} interface.
     *
     * @return the result type ClassInfo, may be null if the handler returns void
     */
    public ClassInfo getResultType() {
        return resultType;
    }

    /**
     * Returns the fully qualified name of the handler class.
     *
     * @return the handler class name, never null
     */
    public String getHandlerClassName() {
        return handlerClassName;
    }

    /**
     * Checks if this handler has a non-void result type.
     *
     * @return true if the result type is not null
     */
    public boolean hasResultType() {
        return resultType != null;
    }

    @Override
    public String toString() {
        return "HandlerTypeInfo{" +
               "handlerClassName='" + handlerClassName + '\'' +
               ", commandType=" + (commandType != null ? commandType.name() : "null") +
               ", resultType=" + (resultType != null ? resultType.name() : "null") +
               '}';
    }

}
