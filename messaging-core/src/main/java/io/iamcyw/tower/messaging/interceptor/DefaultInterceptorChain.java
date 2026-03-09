package io.iamcyw.tower.messaging.interceptor;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;

import java.util.List;

/**
 * Default implementation of {@link CommandInterceptorChain}.
 *
 * <p>This class manages the execution of interceptors in sequence, ultimately
 * invoking the command handler. Each interceptor in the chain receives the
 * next interceptor (or the terminal handler) as its chain parameter.</p>
 *
 * @since 2.0
 */
public class DefaultInterceptorChain implements CommandInterceptorChain {

    private final int index;
    private final List<CommandInterceptor> interceptors;
    private final CommandHandler<Command, Object> handler;

    /**
     * Creates a new interceptor chain.
     *
     * @param interceptors the list of interceptors; may be empty but not null
     * @param handler the command handler to invoke at the end of the chain; never null
     */
    @SuppressWarnings("unchecked")
    public DefaultInterceptorChain(List<CommandInterceptor> interceptors, CommandHandler<?, ?> handler) {
        this.interceptors = List.copyOf(interceptors);
        this.handler = (CommandHandler<Command, Object>) handler;
        this.index = 0;
    }

    private DefaultInterceptorChain(int index, List<CommandInterceptor> interceptors, CommandHandler<Command, Object> handler) {
        this.index = index;
        this.interceptors = interceptors;
        this.handler = handler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Command, R> R proceed(C command) throws Exception {
        if (command == null) {
            throw new NullPointerException("command must not be null");
        }

        if (index < interceptors.size()) {
            CommandInterceptor interceptor = interceptors.get(index);
            CommandInterceptorChain nextChain = new DefaultInterceptorChain(index + 1, interceptors, handler);
            return interceptor.intercept(command, nextChain);
        } else {
            return (R) handler.handle(command);
        }
    }

}
