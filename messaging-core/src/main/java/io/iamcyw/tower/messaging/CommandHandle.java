package io.iamcyw.tower.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a command handler.
 *
 * @deprecated since 2.0, for removal. Use {@link CommandHandler} interface instead.
 *             Migrate by implementing {@code CommandHandler<C extends Command, R>} interface
 *             and overriding the {@code handle(C command)} method.
 *             Example: {@code @Override public MyResult handle(MyCommand command) { ... }}
 */
@Deprecated(since = "2.0", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandle {
}
