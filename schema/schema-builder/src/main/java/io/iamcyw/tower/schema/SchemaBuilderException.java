package io.iamcyw.tower.schema;

/**
 * Exception thrown when schema building fails.
 *
 * @since 2.0
 */
public class SchemaBuilderException extends RuntimeException {

    public SchemaBuilderException(String message) {
        super(message);
    }

    public SchemaBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

}
