package io.iamcyw.tower.utils.collect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.iamcyw.tower.Preconditions.requireNonNull;

/**
 * Operations for transforming and filtering lists without Stream API overhead.
 *
 * <p>This class provides list operations like map, flatMap, and filter that
 * avoid the intermediate object creation of the Stream API. Benchmarking has
 * shown these operations can outperform equivalent Stream operations for simple
 * transformations on small to medium sized lists.</p>
 *
 * @since 2.0
 */
public final class ListOperations {

    private ListOperations() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Maps each element of the iterable to a list and flattens the result.
     *
     * @param iterable the source iterable
     * @param mapper   function to map each element to a list
     * @param <T>      the source element type
     * @param <R>      the result element type
     * @return a list containing all elements from the mapped lists
     */
    public static <T, R> List<R> flatMap(Iterable<? extends T> iterable, Function<T, List<R>> mapper) {
        requireNonNull(iterable);
        requireNonNull(mapper);
        List<R> result = new ArrayList<>();
        for (T item : iterable) {
            List<R> mapped = mapper.apply(item);
            result.addAll(mapped);
        }
        return result;
    }

    /**
     * Transforms each element using the given mapper function.
     *
     * <p>This is more efficient than {@code stream().map().collect()} because it does
     * not create the intermediate objects needed for the flexible stream style.
     * Benchmarking has shown this to outperform stream-based mapping.</p>
     *
     * @param iterable the source iterable
     * @param mapper   the transformation function
     * @param <T>      the source element type
     * @param <R>      the result element type
     * @return a list containing the transformed elements
     */
    public static <T, R> List<R> map(Iterable<? extends T> iterable, Function<? super T, ? extends R> mapper) {
        requireNonNull(iterable);
        requireNonNull(mapper);
        List<R> result = new ArrayList<>();
        for (T item : iterable) {
            R mapped = mapper.apply(item);
            result.add(mapped);
        }
        return result;
    }

    /**
     * Filters elements based on the given predicate.
     *
     * @param iterable the source iterable
     * @param predicate the filter predicate (returns true to include)
     * @param <R>      the element type
     * @return a list containing only elements matching the predicate
     */
    public static <R> List<R> filter(Iterable<R> iterable, Function<R, Boolean> predicate) {
        requireNonNull(iterable);
        requireNonNull(predicate);
        List<R> result = new ArrayList<>();
        for (R item : iterable) {
            if (Boolean.TRUE.equals(predicate.apply(item))) {
                result.add(item);
            }
        }
        return result;
    }

}
