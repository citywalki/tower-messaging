package io.iamcyw.tower.utils;

/**
 * Utilities for working with class names and simple names.
 *
 * <p>This class provides helper methods for extracting simple class names
 * from fully qualified names and converting objects to strings.</p>
 *
 * @since 2.0
 */
public class ClassNames {

    private ClassNames() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extracts the simple class name without package prefix.
     *
     * <p>For example, {@code java.lang.String} becomes {@code String}.
     * If the class is in the default package, the original name is returned.</p>
     *
     * @param className the fully qualified class name
     * @return the simple class name without package prefix
     */
    public static String simpleName(String className) {
        int index = className.lastIndexOf('.');
        if (index == -1) {
            return className;
        }
        return index < className.length() - 1 ? className.substring(index + 1) : "";
    }

    /**
     * Converts an object to its string representation.
     *
     * <p>Returns {@code null} if the input is null, returns the string directly
     * if already a String, otherwise calls {@link Object#toString()}.</p>
     *
     * @param object the object to convert
     * @return the string representation, or null if input was null
     */
    public static String toString(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        }
        return object.toString();
    }

}
