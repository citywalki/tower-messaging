package io.iamcyw.tower.quarkus.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Quarkus BuildItem that carries information about a discovered command handler.
 *
 * <p>This build item is produced during the scanning phase and consumed by the
 * metadata generation phase. It contains all necessary information to generate
 * a {@code HandlerMetadata} implementation for a command handler.</p>
 *
 * <p>Each discovered {@code CommandHandler} implementation produces one
 * {@code CommandHandlerBuildItem}, which is then used to generate the corresponding
 * {@code {HandlerClass}$Metadata} class at build time.</p>
 *
 * @see HandlerMetadataGenerator
 * @see io.iamcyw.tower.messaging.CommandHandler
 * @since 2.0
 */
public final class CommandHandlerBuildItem extends MultiBuildItem {

    private final String handlerClass;
    private final String commandType;
    private final String resultType;
    private final String metadataClassName;

    /**
     * Creates a new CommandHandlerBuildItem with the handler information.
     *
     * @param handlerClass       the fully qualified name of the handler class, must not be null
     * @param commandType        the fully qualified name of the command type, must not be null
     * @param resultType         the fully qualified name of the result type, may be null for void
     * @param metadataClassName  the fully qualified name of the generated metadata class, must not be null
     * @throws NullPointerException if handlerClass, commandType, or metadataClassName is null
     */
    public CommandHandlerBuildItem(String handlerClass, String commandType, String resultType, String metadataClassName) {
        this.handlerClass = handlerClass;
        this.commandType = commandType;
        this.resultType = resultType;
        this.metadataClassName = metadataClassName;
    }

    /**
     * Returns the fully qualified name of the handler class.
     *
     * @return the handler class name, never null
     */
    public String getHandlerClass() {
        return handlerClass;
    }

    /**
     * Returns the fully qualified name of the command type.
     *
     * @return the command type name, never null
     */
    public String getCommandType() {
        return commandType;
    }

    /**
     * Returns the fully qualified name of the result type.
     *
     * @return the result type name, may be null if the handler returns void
     */
    public String getResultType() {
        return resultType;
    }

    /**
     * Returns the fully qualified name of the generated metadata class.
     *
     * @return the metadata class name, never null
     */
    public String getMetadataClassName() {
        return metadataClassName;
    }

    @Override
    public String toString() {
        return "CommandHandlerBuildItem{" +
               "handlerClass='" + handlerClass + '\'' +
               ", commandType='" + commandType + '\'' +
               ", resultType='" + resultType + '\'' +
               ", metadataClassName='" + metadataClassName + '\'' +
               '}';
    }

}
