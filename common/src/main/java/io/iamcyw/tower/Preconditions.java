package io.iamcyw.tower;

import java.util.function.Supplier;

/**
 * Validates preconditions for method arguments and state.
 *
 * <p>This class provides validation methods that throw appropriate exceptions
 * when preconditions are not met. It is designed for validating method arguments
 * and object state in the messaging framework.</p>
 *
 * @since 2.0
 */
public interface Preconditions {

    /**
     * Validates that the given object is not null.
     *
     * @param object the object to check
     * @param <T>    the object type
     * @return the object if not null
     * @throws IllegalArgumentException if the object is null
     */
    static <T> T requireNonNull(T object) {
        if (object != null) {
            return object;
        }
        throw new IllegalArgumentException(TowerMessageCommonMessages.log.objectRequiredNotNull());
    }

    /**
     * Validates that the given object is not null, with a descriptive name.
     *
     * @param object the object to check
     * @param name   the name of the parameter for the error message
     * @param <T>    the object type
     * @return the object if not null
     * @throws IllegalArgumentException if the object is null
     */
    static <T> T requireNonNull(T object, String name) {
        if (object != null) {
            return object;
        }
        throw TowerMessageCommonMessages.log.nullParam(name);
    }

    /**
     * Validates that the given object is not null, with a custom message supplier.
     *
     * @param object  the object to check
     * @param message supplier for the error message
     * @param <T>     the object type
     * @return the object if not null
     * @throws IllegalArgumentException if the object is null
     */
    static <T> T requireNonNull(T object, Supplier<String> message) {
        if (object != null) {
            return object;
        }
        throw new IllegalArgumentException(message.get());
    }

}
