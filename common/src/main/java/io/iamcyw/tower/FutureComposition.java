package io.iamcyw.tower;

import io.iamcyw.tower.utils.collect.ListOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Composes multiple asynchronous operations into unified results.
 *
 * <p>This class provides utilities for working with {@link CompletableFuture}
 * including sequential and parallel execution, exception handling, and
 * transformations over lists of futures.</p>
 *
 * @since 2.0
 */
@SuppressWarnings("FutureReturnValueIgnored")
public class FutureComposition {

    /**
     * Waits for all futures to complete and returns their results as a list.
     *
     * <p>If any future completes exceptionally, the returned future will also
     * complete exceptionally with the same cause.</p>
     *
     * @param futures the list of futures to wait for
     * @param <U>     the result type
     * @return a future that completes with a list of all results
     */
    public static <U> CompletableFuture<List<U>> allOf(List<CompletableFuture<U>> futures) {
        CompletableFuture<List<U>> result = new CompletableFuture<>();

        @SuppressWarnings("unchecked")
        CompletableFuture<U>[] array = futures.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(array).whenComplete((ignored, exception) -> {
            if (exception != null) {
                result.completeExceptionally(exception);
                return;
            }
            List<U> results = new ArrayList<>(array.length);
            for (CompletableFuture<U> future : array) {
                results.add(future.join());
            }
            result.complete(results);
        });
        return result;
    }

    /**
     * Maps each element to a future and waits for all to complete.
     *
     * @param collection the source collection
     * @param futureFactory function to create a future from each element
     * @param <T>        the source element type
     * @param <U>        the future result type
     * @return a future that completes with a list of all results
     */
    public static <T, U> CompletableFuture<List<U>> mapEach(Collection<T> collection,
                                                         BiFunction<T, Integer, CompletableFuture<U>> futureFactory) {
        List<CompletableFuture<U>> futures = new ArrayList<>(collection.size());
        int index = 0;
        for (T item : collection) {
            CompletableFuture<U> future;
            try {
                future = futureFactory.apply(item, index++);
                Preconditions.requireNonNull(future, TowerMessageCommonMessages.log::cfFactoryNonNullValue);
            } catch (Exception e) {
                future = new CompletableFuture<>();
                future.completeExceptionally(new CompletionException(e));
            }
            futures.add(future);
        }
        return allOf(futures);
    }

    /**
     * Executes futures sequentially, preserving order.
     *
     * <p>Each future is created only after the previous one completes,
     * allowing later futures to depend on earlier results.</p>
     *
     * @param iterable the source iterable
     * @param futureFactory function to create a future from each element and previous results
     * @param <T>        the source element type
     * @param <U>        the future result type
     * @return a future that completes with a list of all results in order
     */
    public static <T, U> CompletableFuture<List<U>> sequentially(Iterable<T> iterable,
                                                               SequentialFutureFactory<T, U> futureFactory) {
        CompletableFuture<List<U>> result = new CompletableFuture<>();
        sequentiallyImpl(iterable.iterator(), futureFactory, 0, new ArrayList<>(), result);
        return result;
    }

    private static <T, U> void sequentiallyImpl(Iterator<T> iterator,
                                                  SequentialFutureFactory<T, U> futureFactory,
                                                  int index,
                                                  List<U> accumulatedResults,
                                                  CompletableFuture<List<U>> result) {
        if (!iterator.hasNext()) {
            result.complete(accumulatedResults);
            return;
        }
        CompletableFuture<U> future;
        try {
            future = futureFactory.apply(iterator.next(), index, accumulatedResults);
            Preconditions.requireNonNull(future, TowerMessageCommonMessages.log::cfFactoryNonNullValue);
        } catch (Exception e) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new CompletionException(e));
        }
        future.whenComplete((value, exception) -> {
            if (exception != null) {
                result.completeExceptionally(exception);
                return;
            }
            accumulatedResults.add(value);
            sequentiallyImpl(iterator, futureFactory, index + 1, accumulatedResults, result);
        });
    }

    /**
     * Wraps an object in a CompletableFuture if not already one.
     *
     * @param value the value to wrap
     * @param <T>   the value type
     * @return a completed future with the value, or the future itself if already a CompletionStage
     */
    public static <T> CompletableFuture<T> wrap(T value) {
        if (value instanceof CompletionStage) {
            @SuppressWarnings("unchecked")
            CompletableFuture<T> future = ((CompletionStage<T>) value).toCompletableFuture();
            return future;
        }
        return CompletableFuture.completedFuture(value);
    }

    /**
     * Catches exceptions from a supplier and returns them as a failed future.
     *
     * @param supplier the supplier that may throw
     * @param <T>      the result type
     * @return the future from the supplier, or a failed future if the supplier threw
     */
    public static <T> CompletableFuture<T> tryCatch(Supplier<CompletableFuture<T>> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            CompletableFuture<T> result = new CompletableFuture<>();
            result.completeExceptionally(e);
            return result;
        }
    }

    /**
     * Creates a CompletableFuture that is already completed exceptionally.
     *
     * @param exception the exception to complete with
     * @param <T>      the result type
     * @return a failed future
     */
    public static <T> CompletableFuture<T> failedFuture(Throwable exception) {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(exception);
        return result;
    }

    /**
     * Maps each element to a future and flattens the results.
     *
     * @param inputs the source list
     * @param mapper function to map each element to a future
     * @param <U>    the result type
     * @param <T>    the source type
     * @return a future that completes with a list of all results
     */
    public static <U, T> CompletableFuture<List<U>> flatMap(List<T> inputs, Function<T, CompletableFuture<U>> mapper) {
        List<CompletableFuture<U>> futures = ListOperations.map(inputs, mapper);
        return allOf(futures);
    }

    /**
     * Maps over the results of a future list.
     *
     * @param future the future containing a list
     * @param mapper function to transform each element
     * @param <U>    the result type
     * @param <T>    the source type
     * @return a future containing the transformed list
     */
    public static <U, T> CompletableFuture<List<U>> map(CompletableFuture<List<T>> future, Function<T, U> mapper) {
        return future.thenApply(list -> ListOperations.map(list, mapper));
    }

    /**
     * Maps over a list of futures, transforming each result.
     *
     * @param futures the list of futures
     * @param mapper  the transformation function
     * @param <U>     the result type
     * @param <T>     the source type
     * @return a list of futures with transformed results
     */
    public static <U, T> List<CompletableFuture<U>> map(List<CompletableFuture<T>> futures, Function<T, U> mapper) {
        return ListOperations.map(futures, future -> future.thenApply(mapper));
    }

    /**
     * Composes a list of futures with a mapping function.
     *
     * @param futures the list of futures
     * @param mapper  function that returns a new future for each result
     * @param <U>     the final result type
     * @param <T>     the source type
     * @return a list of composed futures
     */
    public static <U, T> List<CompletableFuture<U>> compose(List<CompletableFuture<T>> futures,
                                                            Function<T, CompletableFuture<U>> mapper) {
        return ListOperations.map(futures, future -> future.thenCompose(mapper));
    }

    /**
     * Factory for creating futures in sequential execution.
     *
     * @param <T> the input type
     * @param <U> the future result type
     */
    @FunctionalInterface
    public interface SequentialFutureFactory<T, U> {
        /**
         * Creates a future for the given input.
         *
         * @param input           the current element
         * @param index           the index of the element
         * @param previousResults all previously completed results
         * @return a CompletableFuture for this element
         */
        CompletableFuture<U> apply(T input, int index, List<U> previousResults);
    }

}
