package io.iamcyw.tower.messaging.interceptor;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.DefaultHandlerRegistry;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.gateway.DefaultMessageGateway;
import io.iamcyw.tower.messaging.gateway.NoHandlerFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CommandInterceptor} and {@link DefaultInterceptorChain}.
 */
class CommandInterceptorTest {

    record TestCommand(String value) implements Command {
    }

    record TestResult(String value) {
    }

    static class TestHandler implements CommandHandler<TestCommand, TestResult> {
        @Override
        public TestResult handle(TestCommand command) {
            return new TestResult("handled:" + command.value());
        }
    }

    static class CountingInterceptor implements CommandInterceptor {
        private final AtomicInteger beforeCount = new AtomicInteger(0);
        private final AtomicInteger afterCount = new AtomicInteger(0);
        private final int order;

        CountingInterceptor(int order) {
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public <C extends Command, R> R intercept(C command, CommandInterceptorChain chain) throws Exception {
            beforeCount.incrementAndGet();
            R result = chain.proceed(command);
            afterCount.incrementAndGet();
            return result;
        }

        int getBeforeCount() {
            return beforeCount.get();
        }

        int getAfterCount() {
            return afterCount.get();
        }
    }

    static class ShortCircuitInterceptor implements CommandInterceptor {
        @Override
        public <C extends Command, R> R intercept(C command, CommandInterceptorChain chain) {
            @SuppressWarnings("unchecked")
            R result = (R) new TestResult("short-circuited");
            return result;
        }
    }

    static class ExceptionInterceptor implements CommandInterceptor {
        @Override
        public <C extends Command, R> R intercept(C command, CommandInterceptorChain chain) throws Exception {
            throw new RuntimeException("Interceptor exception");
        }
    }

    private static HandlerMetadata<TestCommand, TestResult> createMetadata(Class<?> handlerClass) {
        return new HandlerMetadata<>() {
            @Override
            public Class<TestCommand> commandType() {
                return TestCommand.class;
            }

            @Override
            public Class<TestResult> resultType() {
                return TestResult.class;
            }

            @Override
            public String handlerName() {
                return handlerClass.getName();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends CommandHandler<TestCommand, TestResult>> handlerClass() {
                return (Class<? extends CommandHandler<TestCommand, TestResult>>) handlerClass;
            }
        };
    }

    @Test
    @DisplayName("Should execute interceptors in order")
    void shouldExecuteInterceptorsInOrder() {
        // Given
        TestHandler handler = new TestHandler();
        CountingInterceptor interceptor1 = new CountingInterceptor(1);
        CountingInterceptor interceptor2 = new CountingInterceptor(2);

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(createMetadata(handler.getClass()))
        );

        DefaultMessageGateway gateway = new DefaultMessageGateway(registry,
                List.of(interceptor1, interceptor2));

        // When
        TestCommand command = new TestCommand("test");
        gateway.send(command);

        // Then
        assertThat(interceptor1.getBeforeCount()).isEqualTo(1);
        assertThat(interceptor1.getAfterCount()).isEqualTo(1);
        assertThat(interceptor2.getBeforeCount()).isEqualTo(1);
        assertThat(interceptor2.getAfterCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should execute interceptors in reverse order when order is reversed")
    void shouldExecuteInterceptorsInReverseOrder() {
        // Given
        TestHandler handler = new TestHandler();
        List<String> executionOrder = new ArrayList<>();

        CommandInterceptor interceptor1 = new CommandInterceptor() {
            @Override
            public int order() {
                return 10;
            }

            @Override
            public <C extends Command, R> R intercept(C command, CommandInterceptorChain chain) throws Exception {
                executionOrder.add("interceptor1-before");
                R result = chain.proceed(command);
                executionOrder.add("interceptor1-after");
                return result;
            }
        };

        CommandInterceptor interceptor2 = new CommandInterceptor() {
            @Override
            public int order() {
                return 5; // Lower order, should execute first
            }

            @Override
            public <C extends Command, R> R intercept(C command, CommandInterceptorChain chain) throws Exception {
                executionOrder.add("interceptor2-before");
                R result = chain.proceed(command);
                executionOrder.add("interceptor2-after");
                return result;
            }
        };

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(createMetadata(handler.getClass()))
        );

        DefaultMessageGateway gateway = new DefaultMessageGateway(registry,
                List.of(interceptor1, interceptor2));

        // When
        gateway.send(new TestCommand("test"));

        // Then - interceptor2 has lower order, so it executes first
        assertThat(executionOrder).containsExactly(
                "interceptor2-before",
                "interceptor1-before",
                "interceptor1-after",
                "interceptor2-after"
        );
    }

    @Test
    @DisplayName("Should allow interceptor to short-circuit chain")
    void shouldAllowInterceptorToShortCircuit() {
        // Given
        TestHandler handler = new TestHandler();
        CountingInterceptor normalInterceptor = new CountingInterceptor(1);

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(createMetadata(handler.getClass()))
        );

        DefaultMessageGateway gateway = new DefaultMessageGateway(registry,
                List.of(normalInterceptor, new ShortCircuitInterceptor()));

        // When
        TestCommand command = new TestCommand("test");
        CompletableFuture<TestResult> future = gateway.sendAsync(command, TestResult.class);
        TestResult result = future.join();

        // Then - handler should not be called due to short-circuit
        assertThat(result.value()).isEqualTo("short-circuited");
    }

    @Test
    @DisplayName("Should propagate exception from interceptor")
    void shouldPropagateExceptionFromInterceptor() {
        // Given
        TestHandler handler = new TestHandler();

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(createMetadata(handler.getClass()))
        );

        DefaultMessageGateway gateway = new DefaultMessageGateway(registry,
                List.of(new ExceptionInterceptor()));

        // When/Then
        TestCommand command = new TestCommand("test");
        assertThatThrownBy(() -> gateway.send(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Interceptor exception");
    }

    @Test
    @DisplayName("Should work without interceptors")
    void shouldWorkWithoutInterceptors() {
        // Given
        TestHandler handler = new TestHandler();

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(createMetadata(handler.getClass()))
        );

        DefaultMessageGateway gateway = new DefaultMessageGateway(registry, List.of());

        // When
        TestCommand command = new TestCommand("test");
        CompletableFuture<TestResult> future = gateway.sendAsync(command, TestResult.class);
        TestResult result = future.join();

        // Then
        assertThat(result.value()).isEqualTo("handled:test");
    }

    @Test
    @DisplayName("Should throw NoHandlerFoundException when no handler matches")
    void shouldThrowNoHandlerFoundException() {
        // Given
        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(),
                List.of()
        );

        DefaultMessageGateway gateway = new DefaultMessageGateway(registry,
                List.of(new CountingInterceptor(1)));

        // When/Then
        TestCommand command = new TestCommand("test");
        assertThatThrownBy(() -> gateway.send(command))
                .isInstanceOf(NoHandlerFoundException.class);
    }
}
