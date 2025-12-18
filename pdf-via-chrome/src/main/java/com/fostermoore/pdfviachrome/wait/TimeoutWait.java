package com.fostermoore.pdfviachrome.wait;

import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * A simple wait strategy that waits for a fixed duration.
 * <p>
 * This is the simplest wait strategy, useful when you know exactly how long
 * a page takes to load or render. The strategy always succeeds (unless interrupted)
 * after waiting for the specified duration.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Wait for 3 seconds before PDF generation
 * WaitStrategy wait = new TimeoutWait(Duration.ofSeconds(3));
 * wait.await(cdpService, Duration.ofSeconds(10));
 *
 * // Or using the static factory method
 * WaitStrategy wait = WaitStrategy.timeout(Duration.ofSeconds(3));
 * }</pre>
 *
 * <h2>Thread Interruption</h2>
 * <p>
 * This strategy respects thread interruption. If the waiting thread is interrupted,
 * an {@link InterruptedException} will be thrown and the thread's interrupt status
 * will be restored.
 * </p>
 *
 * @see WaitStrategy
 * @since 1.0.0
 */
public class TimeoutWait implements WaitStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TimeoutWait.class);

    private final Duration duration;

    /**
     * Creates a new timeout wait strategy with the specified duration.
     *
     * @param duration the duration to wait
     * @throws IllegalArgumentException if duration is null or negative
     */
    public TimeoutWait(Duration duration) {
        if (duration == null) {
            throw new IllegalArgumentException("Duration cannot be null");
        }
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Duration must be positive, got: " + duration);
        }
        this.duration = duration;
    }

    /**
     * Waits for the specified duration.
     * <p>
     * This method ignores the timeout parameter since it simply waits for the fixed
     * duration specified in the constructor. The wait always succeeds unless the thread
     * is interrupted.
     * </p>
     *
     * @param cdp the Chrome DevTools Service (not used by this strategy)
     * @param timeout the maximum time to wait (not used by this strategy)
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws IllegalArgumentException if cdp is null or timeout is null
     */
    @Override
    public void await(ChromeDevToolsService cdp, Duration timeout) throws TimeoutException, InterruptedException {
        if (cdp == null) {
            throw new IllegalArgumentException("ChromeDevToolsService cannot be null");
        }
        if (timeout == null) {
            throw new IllegalArgumentException("Timeout cannot be null");
        }

        long millis = duration.toMillis();
        logger.debug("Waiting for {} ms before proceeding", millis);

        try {
            Thread.sleep(millis);
            logger.debug("Wait completed successfully after {} ms", millis);
        } catch (InterruptedException e) {
            logger.warn("Wait interrupted after {} ms", millis);
            // Restore interrupt status
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * Gets the configured wait duration.
     *
     * @return the wait duration
     */
    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "TimeoutWait{duration=" + duration + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeoutWait that = (TimeoutWait) o;
        return duration.equals(that.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }
}
