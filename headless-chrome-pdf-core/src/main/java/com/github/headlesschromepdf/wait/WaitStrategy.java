package com.github.headlesschromepdf.wait;

import com.github.kklisura.cdt.services.ChromeDevToolsService;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Defines a strategy for waiting for page readiness before PDF generation.
 * <p>
 * Different implementations provide various waiting conditions to ensure content is fully loaded
 * before capturing the page as a PDF. This interface allows for flexible wait strategies
 * that can be composed and customized based on application needs.
 * </p>
 *
 * <h2>Common Implementations</h2>
 * <ul>
 *   <li>{@link TimeoutWait} - Wait for a fixed duration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Wait with a simple timeout
 * WaitStrategy timeout = WaitStrategy.timeout(Duration.ofSeconds(5));
 * timeout.await(cdpService, Duration.ofSeconds(10));
 * }</pre>
 *
 * <h2>Custom Implementation Example</h2>
 * <pre>{@code
 * public class CustomWait implements WaitStrategy {
 *     @Override
 *     public void await(ChromeDevToolsService cdp, Duration timeout) throws TimeoutException {
 *         // Custom wait logic here
 *     }
 * }
 * }</pre>
 *
 * @see TimeoutWait
 * @since 1.0.0
 */
public interface WaitStrategy {

    /**
     * Waits for the page to be ready according to this strategy's criteria.
     * <p>
     * This method blocks until either the wait condition is satisfied or the timeout is reached.
     * Implementations should respect the timeout parameter and throw {@link TimeoutException}
     * if the wait condition is not satisfied within the specified duration.
     * </p>
     *
     * @param cdp the Chrome DevTools Service for interacting with the browser
     * @param timeout the maximum time to wait for the condition to be satisfied
     * @throws TimeoutException if the wait condition is not satisfied within the timeout
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws IllegalArgumentException if cdp is null or timeout is null/negative
     */
    void await(ChromeDevToolsService cdp, Duration timeout) throws TimeoutException, InterruptedException;

    /**
     * Creates a timeout-based wait strategy that waits for a fixed duration.
     * <p>
     * This is the simplest wait strategy, useful when you know exactly how long
     * a page takes to load or render.
     * </p>
     *
     * @param duration the duration to wait
     * @return a new TimeoutWait strategy
     * @throws IllegalArgumentException if duration is null or negative
     * @see TimeoutWait
     */
    static WaitStrategy timeout(Duration duration) {
        return new TimeoutWait(duration);
    }

    /**
     * Creates a timeout-based wait strategy with a default duration of 2 seconds.
     * <p>
     * This is a convenience method equivalent to {@code timeout(Duration.ofSeconds(2))}.
     * </p>
     *
     * @return a new TimeoutWait strategy with 2 second duration
     * @see TimeoutWait
     */
    static WaitStrategy timeout() {
        return timeout(Duration.ofSeconds(2));
    }
}
