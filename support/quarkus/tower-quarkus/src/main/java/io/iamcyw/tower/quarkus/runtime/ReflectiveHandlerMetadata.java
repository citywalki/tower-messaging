package io.iamcyw.tower.quarkus.runtime;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerMetadata;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Reflective implementation of {@link HandlerMetadata}.
 *
 * <p>This class extracts type information from a {@link CommandHandler} implementation
 * using reflection. It is used as a fallback when generated metadata classes are not
 * available.</p>
 *
 * @param <C> the command type
 * @param <R> the result type
 * @since 2.0
 */
public class ReflectiveHandlerMetadata<C extends Command, R> implements HandlerMetadata<C, R> {

    private final Class<C> commandType;
    private final Class<R> resultType;
    private final Class<? extends CommandHandler<C, R>> handlerClass;
    private final String handlerName;

    /**
     * Creates reflective metadata from a handler instance.
     *
     * @param handler the handler to extract metadata from
     * @throws IllegalArgumentException if type parameters cannot be extracted
     */
    @SuppressWarnings("unchecked")
    public ReflectiveHandlerMetadata(CommandHandler<C, R> handler) {
        this.handlerClass = (Class<? extends CommandHandler<C, R>>) handler.getClass();
        this.handlerName = handler.getClass().getName();

        Type[] typeParams = extractTypeParameters(handler.getClass());
        if (typeParams == null || typeParams.length < 2) {
            throw new IllegalArgumentException(
                "Cannot extract type parameters from handler: " + handlerName);
        }

        this.commandType = resolveClass(typeParams[0]);
        this.resultType = resolveClass(typeParams[1]);
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

    /**
     * Extracts type parameters from the CommandHandler interface implementation.
     */
    private Type[] extractTypeParameters(Class<?> clazz) {
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericInterface;
                if (pt.getRawType().equals(CommandHandler.class)) {
                    return pt.getActualTypeArguments();
                }
            }
        }

        // Check superclass
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            return extractTypeParameters(superclass);
        }

        return null;
    }

    /**
     * Resolves a Type to its Class representation.
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> resolveClass(Type type) {
        if (type instanceof Class) {
            return (Class<T>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<T>) ((ParameterizedType) type).getRawType();
        }
        return null;
    }

}
