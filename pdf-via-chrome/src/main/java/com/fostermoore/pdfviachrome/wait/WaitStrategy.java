package com.fostermoore.pdfviachrome.wait;

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
 *   <li>{@link NetworkIdleWait} - Wait until network activity stops (Version 2.0+)</li>
 *   <li>{@link ElementWait} - Wait for a specific element to appear (Version 2.0+)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Wait with a simple timeout
 * WaitStrategy timeout = WaitStrategy.timeout(Duration.ofSeconds(5));
 * timeout.await(cdpService, Duration.ofSeconds(10));
 *
 * // Wait for network to be idle
 * WaitStrategy networkIdle = WaitStrategy.networkIdle();
 * networkIdle.await(cdpService, Duration.ofSeconds(30));
 *
 * // Wait for a specific element to appear
 * WaitStrategy elementWait = WaitStrategy.elementPresent("#content-loaded");
 * elementWait.await(cdpService, Duration.ofSeconds(10));
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

    /**
     * Creates a network idle wait strategy with custom configuration.
     * <p>
     * This strategy waits until network activity stops for a specified duration.
     * It's useful for pages with AJAX requests and dynamic content loading.
     * </p>
     *
     * @param quietPeriod the duration the network must be quiet before considering it idle
     * @param maxInflightRequests the maximum number of allowed inflight requests during quiet period
     * @return a new NetworkIdleWait strategy
     * @throws IllegalArgumentException if quietPeriod is null or not positive, or maxInflightRequests is negative
     * @see NetworkIdleWait
     * @since 2.0.0
     */
    static WaitStrategy networkIdle(Duration quietPeriod, int maxInflightRequests) {
        return NetworkIdleWait.builder()
                .quietPeriod(quietPeriod)
                .maxInflightRequests(maxInflightRequests)
                .build();
    }

    /**
     * Creates a network idle wait strategy with default settings.
     * <p>
     * This is a convenience method equivalent to {@code networkIdle(Duration.ofMillis(500), 0)}.
     * The default configuration waits for 500ms of network quiet time with no inflight requests.
     * </p>
     *
     * @return a new NetworkIdleWait strategy with default settings (500ms quiet period, 0 max inflight requests)
     * @see NetworkIdleWait
     * @since 2.0.0
     */
    static WaitStrategy networkIdle() {
        return NetworkIdleWait.builder().build();
    }

    /**
     * Creates an element wait strategy that waits for an element to be present in the DOM.
     * <p>
     * This strategy polls the DOM using the specified CSS selector until the element appears.
     * It uses the default poll interval of 100ms.
     * </p>
     *
     * @param cssSelector the CSS selector for the element to wait for
     * @return a new ElementWait strategy that checks for element presence
     * @throws IllegalArgumentException if cssSelector is null or empty
     * @see ElementWait
     * @since 2.0.0
     */
    static WaitStrategy elementPresent(String cssSelector) {
        return ElementWait.builder()
                .selector(cssSelector)
                .waitForVisible(false)
                .build();
    }

    /**
     * Creates an element wait strategy that waits for an element to be visible.
     * <p>
     * This strategy polls the DOM using the specified CSS selector until the element
     * is both present and visible (has non-zero dimensions and is not hidden by CSS).
     * It uses the default poll interval of 100ms.
     * </p>
     *
     * @param cssSelector the CSS selector for the element to wait for
     * @return a new ElementWait strategy that checks for element visibility
     * @throws IllegalArgumentException if cssSelector is null or empty
     * @see ElementWait
     * @since 2.0.0
     */
    static WaitStrategy elementVisible(String cssSelector) {
        return ElementWait.builder()
                .selector(cssSelector)
                .waitForVisible(true)
                .build();
    }

    /**
     * Creates an element wait strategy with custom configuration.
     * <p>
     * This allows full control over the element waiting behavior including
     * poll interval and visibility checking.
     * </p>
     *
     * @param cssSelector the CSS selector for the element to wait for
     * @param pollInterval the interval between element checks
     * @param waitForVisible whether to wait for visibility (true) or just presence (false)
     * @return a new ElementWait strategy with custom configuration
     * @throws IllegalArgumentException if cssSelector is null/empty, or pollInterval is null/negative
     * @see ElementWait
     * @since 2.0.0
     */
    static WaitStrategy element(String cssSelector, Duration pollInterval, boolean waitForVisible) {
        return ElementWait.builder()
                .selector(cssSelector)
                .pollInterval(pollInterval)
                .waitForVisible(waitForVisible)
                .build();
    }
}
