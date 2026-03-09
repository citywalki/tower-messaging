package io.iamcyw.tower.messaging.interceptor;

import io.iamcyw.tower.messaging.Command;

/**
 * Example interceptor that logs command execution.
 *
 * <p>This interceptor demonstrates the interceptor pattern by logging
 * before and after command execution.
 *
 * @since 2.0
 */
public class LoggingInterceptor implements CommandInterceptor {

    @Override
    public int order() {
        return 100; // Execute early
    }

    @Override
    public <C extends Command, R> R intercept(C command, CommandInterceptorChain chain) throws Exception {
        System.out.println("[LOG] Before executing: " + command.getClass().getSimpleName());
        long start = System.currentTimeMillis();

        try {
            R result = chain.proceed(command);
            long duration = System.currentTimeMillis() - start;
            System.out.println("[LOG] After executing: " + command.getClass().getSimpleName() +
                              " (took " + duration + "ms)");
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            System.out.println("[LOG] Exception in: " + command.getClass().getSimpleName() +
                              " after " + duration + "ms - " + e.getMessage());
            throw e;
        }
    }

}
