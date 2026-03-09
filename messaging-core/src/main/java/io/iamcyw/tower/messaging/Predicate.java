package io.iamcyw.tower.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a predicate for conditional message handling.
 *
 * @deprecated since 2.0, for removal. Use {@link CommandHandler#canHandle(Command)} method instead.
 *             Migrate by implementing {@code CommandHandler<C extends Command, R>} interface
 *             and overriding the default {@code canHandle(C command)} method.
 *             Example:
 *             <pre>{@code
 *             @Override
 *             public boolean canHandle(MyCommand command) {
 *                 return command.getStatus() == Status.ACTIVE;
 *             }
 *             }</pre>
 */
@Deprecated(since = "2.0", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Predicate {
}
