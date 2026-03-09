package io.iamcyw.tower.messaging;

/**
 * Metadata interface for {@link CommandHandler} implementations.
 *
 * <p>This interface provides runtime access to the generic type information of a
 * command handler. Since Java generics are subject to type erasure, this metadata
 * is typically generated at build time using bytecode generation tools, enabling
 * zero-reflection access to type information.</p>
 *
 * <p>The metadata enables:</p>
 * <ul>
 *   <li>Runtime type checking without reflection</li>
 *   <li>Handler discovery and routing based on command types</li>
 *   <li>Type-safe handler registry implementations</li>
 * </ul>
 *
 * <p>Example generated implementation:</p>
 * <pre>
 * {@literal @}Singleton
 * public class CreateOrderHandler$Metadata implements HandlerMetadata&lt;CreateOrderCommand, OrderResult&gt; {
 *
 *     {@literal @}Override
 *     public Class&lt;CreateOrderCommand&gt; commandType() {
 *         return CreateOrderCommand.class;
 *     }
 *
 *     {@literal @}Override
 *     public Class&lt;OrderResult&gt; resultType() {
 *         return OrderResult.class;
 *     }
 *
 *     {@literal @}Override
 *     public String handlerName() {
 *         return "com.example.CreateOrderHandler";
 *     }
 *
 *     {@literal @}Override
 *     public Class&lt;CreateOrderHandler&gt; handlerClass() {
 *         return CreateOrderHandler.class;
 *     }
 * }
 * </pre>
 *
 * <p>Usage in handler registry:</p>
 * <pre>
 * public List&lt;CommandHandler&lt;C, R&gt;&gt; getHandlersForCommand(C command) {
 *     return handlers.stream()
 *         .filter(h -&gt; metadataByName.get(h.getClass().getName())
 *             .commandType().isInstance(command))
 *         .map(h -&gt; (CommandHandler&lt;C, R&gt;) h)
 *         .filter(h -&gt; h.canHandle(command))
 *         .collect(Collectors.toList());
 * }
 * </pre>
 *
 * @param <C> the type of command this metadata describes; must extend {@link Command}
 * @param <R> the type of result this metadata describes
 *
 * @see CommandHandler
 * @see Command
 * @since 2.0
 */
public interface HandlerMetadata<C extends Command, R> {

    /**
     * Returns the class object representing the command type.
     *
     * <p>This method provides runtime access to the generic type parameter {@code C}
     * of the associated {@link CommandHandler}. The returned class can be used for
     * type checking, instantiation, or reflection operations.</p>
     *
     * @return the class object for command type C; never null
     */
    Class<C> commandType();

    /**
     * Returns the class object representing the result type.
     *
     * <p>This method provides runtime access to the generic type parameter {@code R}
     * of the associated {@link CommandHandler}. The returned class can be used for
     * type checking or reflection operations.</p>
     *
     * @return the class object for result type R; never null
     */
    Class<R> resultType();

    /**
     * Returns the fully qualified name of the handler class.
     *
     * <p>This name serves as a unique identifier for the handler implementation
     * and is used for debugging and logging purposes. The format follows
     * {@link Class#getName()} conventions.</p>
     *
     * <p>Note: This should not be used for correlating metadata with handler instances
     * in registries, as it may not match for CDI proxy classes. Use
     * {@link #handlerClass()} instead for correlation.</p>
     *
     * @return the fully qualified class name of the handler; never null
     */
    String handlerName();

    /**
     * Returns the actual handler class.
     *
     * <p>This method returns the class object of the handler implementation, not
     * a CDI proxy class. This is essential for correlating metadata with handler
     * instances in registries, as it works correctly even when CDI proxies are used.</p>
     *
     * <p>The returned class is used as the key in handler registries to look up
     * the corresponding metadata for a handler instance.</p>
     *
     * @return the class object of the handler; never null
     */
    Class<? extends CommandHandler<C, R>> handlerClass();

}
