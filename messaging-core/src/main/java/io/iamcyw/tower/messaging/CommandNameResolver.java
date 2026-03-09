package io.iamcyw.tower.messaging;

import java.util.Objects;

/**
 * Strategy interface for resolving command names from command classes.
 *
 * <p>This interface defines how command names are determined from command classes.
 * The default implementation follows these rules:</p>
 * <ol>
 *   <li>If the class has a {@link CommandName} annotation, use its value</li>
 *   <li>If the class name ends with "Command", remove that suffix</li>
 *   <li>Otherwise, use the simple class name as-is</li>
 * </ol>
 *
 * <p>Custom resolvers can be provided to implement different naming conventions.</p>
 *
 * @see CommandName
 * @see HandlerRegistry#getCommandClass(String)
 * @since 2.0
 */
@FunctionalInterface
public interface CommandNameResolver {

    /**
     * Resolves the command name for the given command class.
     *
     * @param commandClass the command class; never null
     * @return the resolved command name; never null
     */
    String resolve(Class<? extends Command> commandClass);

    /**
     * Returns the default command name resolver.
     *
     * <p>The default resolver implements the following strategy:</p>
     * <ol>
     *   <li>If the class has a {@link CommandName} annotation, return its value</li>
     *   <li>If the class name ends with "Command", return the name without that suffix</li>
     *   <li>Otherwise, return the simple class name</li>
     * </ol>
     *
     * @return the default resolver instance
     */
    static CommandNameResolver defaultResolver() {
        return commandClass -> {
            Objects.requireNonNull(commandClass, "commandClass must not be null");

            CommandName annotation = commandClass.getAnnotation(CommandName.class);
            if (annotation != null) {
                return annotation.value();
            }

            String simpleName = commandClass.getSimpleName();
            if (simpleName.endsWith("Command")) {
                return simpleName.substring(0, simpleName.length() - "Command".length());
            }

            return simpleName;
        };
    }

}
