package com.supporthawk.utils;

/**
 * Simple retry helper for transient automation failures (timeouts, flaky navigations).
 * Retries only when the operation throws; does not interpret assertion outcomes.
 */
public final class RetryUtils {

    @FunctionalInterface
    public interface Retryable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface RetryableSupplier<T> {
        T get() throws Exception;
    }

    private RetryUtils() {
    }

    /**
     * Runs {@code action} up to {@code maxAttempts} times.
     * On failure, waits {@code delayMs} via {@link Thread#sleep(long)} before the next attempt.
     * If every attempt fails, rethrows the last throwable (preserving type when possible).
     */
    public static void execute(int maxAttempts, long delayMs, Retryable action) {
        execute(maxAttempts, delayMs, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Same as {@link #execute(int, long, Retryable)} but returns a value from a successful attempt.
     */
    public static <T> T execute(int maxAttempts, long delayMs, RetryableSupplier<T> action) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must be >= 0");
        }

        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Throwable failure) {
                lastFailure = failure;
                if (attempt == maxAttempts) {
                    break;
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(
                            "Retry interrupted after attempt " + attempt + " of " + maxAttempts,
                            interrupted
                    );
                }
            }
        }

        if (lastFailure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (lastFailure instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(lastFailure);
    }
}
