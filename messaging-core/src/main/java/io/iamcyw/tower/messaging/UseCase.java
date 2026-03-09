package io.iamcyw.tower.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as containing message handlers.
 *
 * @deprecated since 2.0, for removal. Use {@link CommandHandler} interface instead.
 *             Migrate by implementing {@code CommandHandler<C extends Command, R>} interface
 *             which provides compile-time type safety and explicit handler contracts.
 *             Example: {@code public class MyHandler implements CommandHandler<MyCommand, MyResult>}
 */
@Deprecated(since = "2.0", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UseCase {
}
