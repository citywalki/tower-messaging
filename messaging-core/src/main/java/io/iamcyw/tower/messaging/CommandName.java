package io.iamcyw.tower.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to explicitly specify the name of a command.
 *
 * <p>This annotation can be used on command classes to define a custom name
 * for lookup purposes. When not present, the command name is derived automatically
 * from the class name by removing the "Command" suffix.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;CommandName("createOrder")
 * public class CreateOrderCommand implements Command {
 *     // ...
 * }
 * </pre>
 *
 * @see HandlerRegistry#getCommandClass(String)
 * @see CommandNameResolver
 * @since 2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandName {

    /**
     * The name of the command.
     *
     * <p>This name is used to identify the command when looking it up by name
     * via {@link HandlerRegistry#getCommandClass(String)}. The name should be
     * unique across all registered commands.</p>
     *
     * @return the command name; never null
     */
    String value();

}
