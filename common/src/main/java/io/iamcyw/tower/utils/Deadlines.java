package io.iamcyw.tower.utils;

/**
 * Utilities for working with deadline timestamps.
 *
 * <p>This class provides helper methods for calculating remaining time
 * until deadlines, useful for timeout and deadline propagation in
 * asynchronous messaging operations.</p>
 *
 * @since 2.0
 */
public class Deadlines {

    private Deadlines() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Gets the number of milliseconds remaining until the deadline.
     *
     * <p>If the deadline has already passed, returns 0. The deadline is
     * measured in milliseconds since the epoch ({@link System#currentTimeMillis()}).</p>
     *
     * @param deadline the deadline timestamp in milliseconds
     * @return the number of milliseconds remaining, or 0 if the deadline has passed
     */
    public static long getRemainingMillis(long deadline) {
        long remaining = deadline - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

}
