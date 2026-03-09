package io.iamcyw.tower.utils;

import io.iamcyw.tower.TowerMessageCommonMessages;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

/**
 * Inspects and classifies Java types for messaging framework operations.
 *
 * <p>This class provides type inspection capabilities to determine if a class
 * represents a number, date, collection, map, or other common type categories.
 * It is used primarily for type-safe message serialization and command handling.</p>
 *
 * <p>All methods operate on class names or Class objects to avoid loading
 * classes unnecessarily during type checking operations.</p>
 *
 * @since 2.0
 */
public class TypeInspector {

    private static final List<String> NUMBERS = new ArrayList<>();

    private static final List<String> DATES = new ArrayList<>();

    private static final Map<String, Class> PRIMITIVE_CLASSES = new HashMap<>();

    private static final Map<String, Class> OBJECT_PRIMITIVE_MAPPING = new HashMap<>();

    public static final String ENUM = Enum.class.getName();

    public static final String OPTIONAL = Optional.class.getName();

    public static final String UUID = java.util.UUID.class.getName();

    public static final String URL = java.net.URL.class.getName();

    public static final String URI = java.net.URI.class.getName();

    public static final String LOCALDATE = LocalDate.class.getName();

    public static final String LOCALDATETIME = LocalDateTime.class.getName();

    public static final String LOCALTIME = LocalTime.class.getName();

    public static final String ZONEDDATETIME = ZonedDateTime.class.getName();

    public static final String OFFSETDATETIME = OffsetDateTime.class.getName();

    public static final String OFFSETTIME = OffsetTime.class.getName();

    public static final String INSTANT = Instant.class.getName();

    public static final String UTIL_DATE = Date.class.getName();

    public static final String SQL_DATE = java.sql.Date.class.getName();

    public static final String SQL_TIMESTAMP = java.sql.Timestamp.class.getName();

    public static final String SQL_TIME = java.sql.Time.class.getName();

    public static final String DURATION = Duration.class.getName();

    public static final String PERIOD = Period.class.getName();

    public static final String BYTE = Byte.class.getName();

    public static final String BYTE_PRIMITIVE = byte.class.getName();

    public static final String SHORT = Short.class.getName();

    public static final String SHORT_PRIMITIVE = short.class.getName();

    public static final String INTEGER = Integer.class.getName();

    public static final String INTEGER_PRIMITIVE = int.class.getName();

    public static final String BIG_INTEGER = BigInteger.class.getName();

    public static final String DOUBLE = Double.class.getName();

    public static final String DOUBLE_PRIMITIVE = double.class.getName();

    public static final String BIG_DECIMAL = BigDecimal.class.getName();

    public static final String LONG = Long.class.getName();

    public static final String LONG_PRIMITIVE = long.class.getName();

    public static final String FLOAT = Float.class.getName();

    public static final String FLOAT_PRIMITIVE = float.class.getName();

    public static final String BOOLEAN = Boolean.class.getName();

    public static final String BOOLEAN_PRIMITIVE = boolean.class.getName();

    public static final String CHARACTER = Character.class.getName();

    public static final String CHARACTER_PRIMITIVE = char.class.getName();

    static {
        PRIMITIVE_CLASSES.put("boolean", boolean.class);
        PRIMITIVE_CLASSES.put("byte", byte.class);
        PRIMITIVE_CLASSES.put("char", char.class);
        PRIMITIVE_CLASSES.put("short", short.class);
        PRIMITIVE_CLASSES.put("int", int.class);
        PRIMITIVE_CLASSES.put("long", long.class);
        PRIMITIVE_CLASSES.put("float", float.class);
        PRIMITIVE_CLASSES.put("double", double.class);

        OBJECT_PRIMITIVE_MAPPING.put(Boolean.class.getName(), boolean.class);
        OBJECT_PRIMITIVE_MAPPING.put(Byte.class.getName(), byte.class);
        OBJECT_PRIMITIVE_MAPPING.put(Character.class.getName(), char.class);
        OBJECT_PRIMITIVE_MAPPING.put(Short.class.getName(), short.class);
        OBJECT_PRIMITIVE_MAPPING.put(Integer.class.getName(), int.class);
        OBJECT_PRIMITIVE_MAPPING.put(Long.class.getName(), long.class);
        OBJECT_PRIMITIVE_MAPPING.put(Float.class.getName(), float.class);
        OBJECT_PRIMITIVE_MAPPING.put(Double.class.getName(), double.class);

        NUMBERS.add(BYTE);
        NUMBERS.add(BYTE_PRIMITIVE);
        NUMBERS.add(SHORT);
        NUMBERS.add(SHORT_PRIMITIVE);
        NUMBERS.add(INTEGER);
        NUMBERS.add(INTEGER_PRIMITIVE);
        NUMBERS.add(BIG_INTEGER);
        NUMBERS.add(DOUBLE);
        NUMBERS.add(DOUBLE_PRIMITIVE);
        NUMBERS.add(BIG_DECIMAL);
        NUMBERS.add(LONG);
        NUMBERS.add(LONG_PRIMITIVE);
        NUMBERS.add(FLOAT);
        NUMBERS.add(FLOAT_PRIMITIVE);

        DATES.add(LOCALDATE);
        DATES.add(LOCALTIME);
        DATES.add(LOCALDATETIME);
        DATES.add(INSTANT);
        DATES.add(ZONEDDATETIME);
        DATES.add(OFFSETDATETIME);
        DATES.add(OFFSETTIME);
        DATES.add(UTIL_DATE);
        DATES.add(SQL_DATE);
        DATES.add(SQL_TIMESTAMP);
        DATES.add(SQL_TIME);
    }

    private TypeInspector() {
    }

    /**
     * Checks if the given class name represents a UUID type.
     *
     * @param className the fully qualified class name
     * @return true if the class is java.util.UUID
     */
    public static boolean isUUID(String className) {
        return className.equals(UUID);
    }

    /**
     * Checks if the given class name represents a URL type.
     *
     * @param className the fully qualified class name
     * @return true if the class is java.net.URL
     */
    public static boolean isURL(String className) {
        return className.equals(URL);
    }

    /**
     * Checks if the given class name represents a URI type.
     *
     * @param className the fully qualified class name
     * @return true if the class is java.net.URI
     */
    public static boolean isURI(String className) {
        return className.equals(URI);
    }

    /**
     * Checks if the given class name represents a Period type.
     *
     * @param className the fully qualified class name
     * @return true if the class is java.time.Period
     */
    public static boolean isPeriod(String className) {
        return className.equals(PERIOD);
    }

    /**
     * Checks if the given class name represents a Duration type.
     *
     * @param className the fully qualified class name
     * @return true if the class is java.time.Duration
     */
    public static boolean isDuration(String className) {
        return className.equals(DURATION);
    }

    /**
     * Checks if the given name represents a primitive type.
     *
     * @param primitiveName the primitive type name (e.g., "int", "boolean")
     * @return true if the name is a primitive type
     */
    public static boolean isPrimitive(String primitiveName) {
        return PRIMITIVE_CLASSES.containsKey(primitiveName);
    }

    /**
     * Checks if the given object is a Collection.
     *
     * @param object the object to check
     * @return true if the object is a non-null Collection
     */
    public static boolean isCollection(Object object) {
        if (object == null)
            return false;
        return Collection.class.isAssignableFrom(object.getClass());
    }

    /**
     * Checks if the given object is a Map.
     *
     * @param object the object to check
     * @return true if the object is a non-null Map
     */
    public static boolean isMap(Object object) {
        if (object == null)
            return false;
        return Map.class.isAssignableFrom(object.getClass());
    }

    /**
     * Gets the primitive Class for a primitive type name.
     *
     * @param primitiveName the primitive type name
     * @return the Class object for the primitive type
     * @throws ClassNotFoundException if the name is not a primitive type
     */
    public static Class getPrimitiveClassType(String primitiveName) throws ClassNotFoundException {
        if (isPrimitive(primitiveName)) {
            return PRIMITIVE_CLASSES.get(primitiveName);
        }
        throw TowerMessageCommonMessages.log.unknownPrimitiveType(primitiveName);
    }

    /**
     * Checks if the given class name represents a numeric type.
     *
     * @param className the fully qualified class name
     * @return true if the class represents a number (Integer, Double, BigDecimal, etc.)
     */
    public static boolean isNumberType(String className) {
        return NUMBERS.contains(className);
    }

    /**
     * Checks if the given class name represents a date or time type.
     *
     * @param className the fully qualified class name
     * @return true if the class represents a date or time
     */
    public static boolean isDateType(String className) {
        return DATES.contains(className);
    }

    /**
     * Tests if {@code boxedType} is the wrapper type of {@code primitiveType}.
     * For example, {@code java.lang.Integer} is the wrapper for {@code int}.
     *
     * @param primitiveType the classname of the primitive type
     * @param boxedType     the classname of the boxed type
     * @return true, if {@code boxedType} is the wrapper type of {@code primitiveType}
     */
    public static boolean isPrimitiveOf(String primitiveType, String boxedType) {
        if (OBJECT_PRIMITIVE_MAPPING.containsKey(boxedType)) {
            return OBJECT_PRIMITIVE_MAPPING.get(boxedType).getName().equals(primitiveType);
        }
        return false;
    }

    /**
     * Checks if the given class name represents a boolean type.
     *
     * @param className the fully qualified class name
     * @return true if the class is Boolean or boolean
     */
    public static boolean isBoolean(String className) {
        return className.equals(BOOLEAN) || className.equals(BOOLEAN_PRIMITIVE);
    }

    /**
     * Checks if the given class name represents a character type.
     *
     * @param className the fully qualified class name
     * @return true if the class is Character or char
     */
    public static boolean isCharacter(String className) {
        return className.equals(CHARACTER) || className.equals(CHARACTER_PRIMITIVE);
    }

}
