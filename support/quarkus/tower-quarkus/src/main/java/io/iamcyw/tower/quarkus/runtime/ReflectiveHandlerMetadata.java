package io.iamcyw.tower.quarkus.runtime;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerMetadata;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Runtime implementation of {@link HandlerMetadata} that extracts type information
 * from a {@link CommandHandler} using reflection.
 *
 * <p>This class is used by {@link QuarkusHandlerRegistry} to create metadata for
 * handlers when the generated metadata classes are not available (e.g., when
 * {@code GeneratedBeanBuildItem} doesn't properly register the generated classes
 * with the CDI container).</p>
 *
 * <p>This implementation uses reflection to extract the generic type parameters
 * from the handler's implementation of {@code CommandHandler<C, R>}. It assumes
 * that the handler class directly implements {@code CommandHandler} with concrete
 * type parameters.</p>
 *
 * @param <C> the type of command handled
 * @param <R> the type of result produced
 * @see HandlerMetadata
 * @see QuarkusHandlerRegistry
 * @since 2.0
 */
public class ReflectiveHandlerMetadata<C extends Command, R> implements HandlerMetadata<C, R> {

    private final CommandHandler<C, R> handler;
    private final Class<C> commandType;
    private final Class<R> resultType;
    private final String handlerName;
    private final Class<? extends CommandHandler<C, R>> handlerClass;

    /**
     * Constructs a new ReflectiveHandlerMetadata for the given handler.
     *
     * <p>This constructor extracts the generic type parameters from the handler's
     * class using reflection. It assumes the handler directly implements
     * {@code CommandHandler<C, R>} with concrete type parameters.</p>
     *
     * @param handler the handler to extract metadata from; must not be null
     * @throws IllegalArgumentException if the type parameters cannot be extracted
     */
    @SuppressWarnings("unchecked")
    public ReflectiveHandlerMetadata(CommandHandler<C, R> handler) {
        if (handler == null) {
            throw new NullPointerException("handler must not be null");
        }
        this.handler = handler;
        this.handlerClass = (Class<? extends CommandHandler<C, R>>) handler.getClass();
        this.handlerName = handlerClass.getName();

        // Extract generic type parameters from the handler class
        Type[] typeParams = extractTypeParameters();
        this.commandType = (Class<C>) typeParams[0];
        this.resultType = (Class<R>) typeParams[1];
    }

    /**
     * Extracts the generic type parameters from the handler class.
     *
     * <p>This method traverses the class hierarchy to find the {@code CommandHandler}
     * interface and extracts its type arguments.</p>
     *
     * @return an array containing the command type and result type classes
     * @throws IllegalArgumentException if the type parameters cannot be extracted
     */
    @SuppressWarnings("unchecked")
    private Type[] extractTypeParameters() {
        Class<?> clazz = handlerClass;

        // Traverse the class hierarchy to find CommandHandler interface
        while (clazz != null && clazz != Object.class) {
            // Check direct interfaces
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) genericInterface;
                    if (pt.getRawType().equals(CommandHandler.class)) {
                        Type[] actualTypeArgs = pt.getActualTypeArguments();
                        if (actualTypeArgs.length == 2) {
                            return new Type[]{
                                    resolveType(actualTypeArgs[0]),
                                    resolveType(actualTypeArgs[1])
                            };
                        }
                    }
                }
            }

            // Move to superclass
            clazz = clazz.getSuperclass();
        }

        throw new IllegalArgumentException(
                "Could not extract type parameters from handler: " + handlerName);
    }

    /**
     * Resolves a type to its Class representation.
     *
     * @param type the type to resolve
     * @return the Class representing the type
     */
    private Class<?> resolveType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

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
        return handlerName;
    }

    @Override
    public Class<? extends CommandHandler<C, R>> handlerClass() {
        return handlerClass;
    }

}
