package io.iamcyw.tower.messaging.gateway;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerRegistry;
import io.iamcyw.tower.messaging.interceptor.CommandInterceptor;
import io.iamcyw.tower.messaging.interceptor.DefaultInterceptorChain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link MessageGateway}.
 *
 * <p>This implementation uses a {@link HandlerRegistry} to look up appropriate
 * handlers for commands and delegates execution to them. It supports both
 * synchronous and asynchronous execution modes, as well as interceptor chains
 * for cross-cutting concerns.</p>
 *
 * <p>For each command, the gateway:
 * <ol>
 *   <li>Queries the {@link HandlerRegistry} for handlers that can process the command</li>
 *   <li>Selects the first matching handler (or throws if none found)</li>
 *   <li>Executes the interceptor chain (if any interceptors are configured)</li>
 *   <li>Invokes the handler's {@code handle()} method</li>
 *   <li>Returns or completes the future with the result</li>
 * </ol>
 *
 * @since 2.0
 */
public class DefaultMessageGateway implements MessageGateway {

    private final HandlerRegistry handlerRegistry;
    private final List<CommandInterceptor> interceptors;

    /**
     * Creates a new DefaultMessageGateway with the given handler registry.
     *
     * @param handlerRegistry the registry to use for handler lookup; must not be null
     * @throws NullPointerException if handlerRegistry is null
     */
    public DefaultMessageGateway(HandlerRegistry handlerRegistry) {
        this(handlerRegistry, List.of());
    }

    /**
     * Creates a new DefaultMessageGateway with the given handler registry and interceptors.
     *
     * @param handlerRegistry the registry to use for handler lookup; must not be null
     * @param interceptors the list of interceptors to apply; may be empty but not null
     * @throws NullPointerException if handlerRegistry or interceptors is null
     */
    public DefaultMessageGateway(HandlerRegistry handlerRegistry, List<CommandInterceptor> interceptors) {
        this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry must not be null");
        this.interceptors = sortInterceptors(Objects.requireNonNull(interceptors, "interceptors must not be null"));
    }

    /**
     * Sorts interceptors by order (ascending).
     *
     * @param interceptors the interceptors to sort
     * @return a sorted list of interceptors
     */
    private List<CommandInterceptor> sortInterceptors(List<CommandInterceptor> interceptors) {
        List<CommandInterceptor> sorted = new ArrayList<>(interceptors);
        sorted.sort(Comparator.comparingInt(CommandInterceptor::order));
        return List.copyOf(sorted);
    }

    @Override
    public void send(Command command) {
        sendWithHandler(command, handler -> {
            @SuppressWarnings("unchecked")
            CommandHandler<Command, ?> typedHandler = (CommandHandler<Command, ?>) handler;
            if (interceptors.isEmpty()) {
                typedHandler.handle(command);
            } else {
                DefaultInterceptorChain chain = new DefaultInterceptorChain(interceptors, typedHandler);
                try {
                    chain.proceed(command);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CommandExecutionException(command, e);
                }
            }
            return null;
        });
    }

    @Override
    public <R> CompletableFuture<R> sendAsync(Command command, Class<R> resultType) {
        return CompletableFuture.supplyAsync(() -> sendWithHandler(command, handler -> {
            @SuppressWarnings("unchecked")
            CommandHandler<Command, R> typedHandler = (CommandHandler<Command, R>) handler;
            if (interceptors.isEmpty()) {
                return typedHandler.handle(command);
            } else {
                DefaultInterceptorChain chain = new DefaultInterceptorChain(interceptors, typedHandler);
                try {
                    return chain.proceed(command);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CommandExecutionException(command, e);
                }
            }
        }));
    }

    /**
     * Helper method to find handler and execute action with it.
     *
     * @param command the command to process
     * @param action the action to perform with the handler
     * @param <R> the result type
     * @return the result of the action
     * @throws NoHandlerFoundException if no handler is found
     */
    @SuppressWarnings("unchecked")
    private <R> R sendWithHandler(Command command, CommandHandlerAction<R> action) {
        Objects.requireNonNull(command, "command must not be null");

        List<CommandHandler<Command, ?>> handlers =
            (List<CommandHandler<Command, ?>>) (List<?>) handlerRegistry.getHandlersForCommand(command);

        if (handlers.isEmpty()) {
            throw new NoHandlerFoundException(command);
        }

        CommandHandler<Command, ?> handler = handlers.get(0);
        return action.execute(handler);
    }

    /**
     * Functional interface for handler actions.
     *
     * @param <R> the result type
     */
    @FunctionalInterface
    private interface CommandHandlerAction<R> {
        R execute(CommandHandler<Command, ?> handler);
    }

}
