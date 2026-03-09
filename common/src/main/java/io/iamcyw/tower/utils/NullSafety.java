package io.iamcyw.tower.utils;

import java.util.function.Supplier;

/**
 * Provides null-safe value retrieval with fallback defaults.
 *
 * <p>This class offers utility methods for handling potentially null values
 * by providing default values or computing defaults via suppliers. It helps
 * eliminate null checks throughout the codebase.</p>
 *
 * @since 2.0
 */
public class NullSafety {

    private NullSafety() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns the given instance, if not {@code null}, or otherwise the value provided by {@code defaultProvider}.
     *
     * @param instance        the value to return, if not {@code null}
     * @param defaultProvider to provide the value, when {@code instance} is {@code null}
     * @param <T>             the type of value to return
     * @return {@code instance} if not {@code null}, otherwise the value provided by {@code defaultProvider}
     */
    public static <T> T getOrDefault(T instance, Supplier<T> defaultProvider) {
        if (instance == null) {
            return defaultProvider.get();
        }
        return instance;
    }

    /**
     * Returns the given instance, if not {@code null}, or otherwise the given {@code defaultValue}.
     *
     * @param instance     the value to return, if not {@code null}
     * @param defaultValue the value, when {@code instance} is {@code null}
     * @param <T>          the type of value to return
     * @return {@code instance} if not {@code null}, otherwise {@code defaultValue}
     */
    public static <T> T getOrDefault(T instance, T defaultValue) {
        if (instance == null) {
            return defaultValue;
        }
        return instance;
    }

    /**
     * Returns the given instance, if not {@code null} or empty, or otherwise the given {@code defaultValue}.
     *
     * @param instance     the value to return, if not {@code null} or empty
     * @param defaultValue the value, when {@code instance} is {@code null} or empty
     * @param <T>          the type of value to return
     * @return {@code instance} if not {@code null} or empty, otherwise {@code defaultValue}
     */
    public static <T extends CharSequence> T getNonEmptyOrDefault(T instance, T defaultValue) {
        if (instance == null || instance.isEmpty()) {
            return defaultValue;
        }
        return instance;
    }

}
