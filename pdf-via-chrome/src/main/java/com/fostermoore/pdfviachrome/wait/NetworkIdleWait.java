package com.fostermoore.pdfviachrome.wait;

import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A wait strategy that waits until network activity stops for a specified duration.
 * <p>
 * This strategy is useful for pages with AJAX requests, dynamic content loading, or
 * any scenario where you need to wait for network requests to complete before capturing
 * the page as a PDF.
 * </p>
 *
 * <h2>How It Works</h2>
 * <p>
 * The strategy monitors network activity using the Chrome DevTools Protocol Network domain.
 * It tracks the number of inflight requests (requests that have started but not yet finished).
 * When the number of inflight requests remains at or below the configured threshold for
 * the specified quiet period, the network is considered idle and the wait completes.
 * </p>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>quietPeriod</b> - The duration the network must be quiet before considering it idle (default: 500ms)</li>
 *   <li><b>maxInflightRequests</b> - Maximum number of allowed inflight requests during quiet period (default: 0)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Wait until network is idle for 500ms with no inflight requests
 * WaitStrategy networkIdle = NetworkIdleWait.builder().build();
 * networkIdle.await(cdpService, Duration.ofSeconds(30));
 *
 * // Wait until network is idle for 1 second with at most 2 inflight requests
 * WaitStrategy networkIdle = NetworkIdleWait.builder()
 *     .quietPeriod(Duration.ofSeconds(1))
 *     .maxInflightRequests(2)
 *     .build();
 * networkIdle.await(cdpService, Duration.ofSeconds(30));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe. Each invocation of {@link #await(ChromeDevToolsService, Duration)}
 * creates its own isolated network monitoring context.
 * </p>
 *
 * @see WaitStrategy
 * @since 2.0.0
 */
public class NetworkIdleWait implements WaitStrategy {

    private static final Logger logger = LoggerFactory.getLogger(NetworkIdleWait.class);

    private static final Duration DEFAULT_QUIET_PERIOD = Duration.ofMillis(500);
    private static final int DEFAULT_MAX_INFLIGHT_REQUESTS = 0;

    private final Duration quietPeriod;
    private final int maxInflightRequests;

    /**
     * Creates a new NetworkIdleWait with the specified configuration.
     *
     * @param quietPeriod the duration the network must be quiet before considering it idle
     * @param maxInflightRequests the maximum number of allowed inflight requests during quiet period
     * @throws IllegalArgumentException if quietPeriod is null or negative, or maxInflightRequests is negative
     */
    private NetworkIdleWait(Duration quietPeriod, int maxInflightRequests) {
        if (quietPeriod == null) {
            throw new IllegalArgumentException("Quiet period cannot be null");
        }
        if (quietPeriod.isNegative() || quietPeriod.isZero()) {
            throw new IllegalArgumentException("Quiet period must be positive, got: " + quietPeriod);
        }
        if (maxInflightRequests < 0) {
            throw new IllegalArgumentException("Max inflight requests must be non-negative, got: " + maxInflightRequests);
        }

        this.quietPeriod = quietPeriod;
        this.maxInflightRequests = maxInflightRequests;
    }

    /**
     * Waits until the network is idle according to this strategy's criteria.
     * <p>
     * This method monitors network activity and blocks until either:
     * <ul>
     *   <li>The network has been idle (inflight requests &lt;= maxInflightRequests) for the quiet period</li>
     *   <li>The timeout is reached</li>
     *   <li>The thread is interrupted</li>
     * </ul>
     * </p>
     *
     * @param cdp the Chrome DevTools Service for interacting with the browser
     * @param timeout the maximum time to wait for the network to become idle
     * @throws TimeoutException if the network does not become idle within the timeout
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws IllegalArgumentException if cdp is null or timeout is null/negative
     */
    @Override
    public void await(ChromeDevToolsService cdp, Duration timeout) throws TimeoutException, InterruptedException {
        if (cdp == null) {
            throw new IllegalArgumentException("ChromeDevToolsService cannot be null");
        }
        if (timeout == null) {
            throw new IllegalArgumentException("Timeout cannot be null");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive, got: " + timeout);
        }

        logger.debug("Starting network idle wait (quietPeriod={}, maxInflightRequests={}, timeout={})",
                quietPeriod, maxInflightRequests, timeout);

        Network network = cdp.getNetwork();
        network.enable();

        // Track inflight requests
        AtomicInteger inflightCount = new AtomicInteger(0);
        AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());
        CompletableFuture<Void> idleFuture = new CompletableFuture<>();

        // Create a timer thread to check for idle state
        Thread idleChecker = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && !idleFuture.isDone()) {
                    long currentTime = System.currentTimeMillis();
                    long timeSinceLastActivity = currentTime - lastActivityTime.get();
                    int currentInflight = inflightCount.get();

                    if (currentInflight <= maxInflightRequests && timeSinceLastActivity >= quietPeriod.toMillis()) {
                        logger.debug("Network idle detected: inflightRequests={}, quietDuration={}ms",
                                currentInflight, timeSinceLastActivity);
                        idleFuture.complete(null);
                        break;
                    }

                    // Check every 50ms
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                idleFuture.completeExceptionally(e);
            }
        }, "NetworkIdleWait-Checker");
        idleChecker.setDaemon(true);

        // Listen to network events
        network.onRequestWillBeSent(event -> {
            int count = inflightCount.incrementAndGet();
            lastActivityTime.set(System.currentTimeMillis());
            logger.trace("Network request started: {} (inflight={})", event.getRequest().getUrl(), count);
        });

        network.onLoadingFinished(event -> {
            int count = inflightCount.decrementAndGet();
            lastActivityTime.set(System.currentTimeMillis());
            logger.trace("Network request finished: requestId={} (inflight={})", event.getRequestId(), count);
        });

        network.onLoadingFailed(event -> {
            int count = inflightCount.decrementAndGet();
            lastActivityTime.set(System.currentTimeMillis());
            logger.trace("Network request failed: requestId={} (inflight={})", event.getRequestId(), count);
        });

        // Start the idle checker thread
        idleChecker.start();

        try {
            // Wait for idle state or timeout
            idleFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            logger.debug("Network idle wait completed successfully");
        } catch (java.util.concurrent.TimeoutException e) {
            logger.warn("Network idle wait timed out after {} (inflight requests: {})", timeout, inflightCount.get());
            throw new TimeoutException("Network did not become idle within " + timeout +
                    " (inflight requests: " + inflightCount.get() + ")");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                logger.warn("Network idle wait interrupted");
                Thread.currentThread().interrupt();
                throw (InterruptedException) cause;
            }
            throw new RuntimeException("Unexpected error during network idle wait", cause);
        } finally {
            // Clean up
            idleChecker.interrupt();
            try {
                network.disable();
            } catch (Exception e) {
                logger.warn("Failed to disable network monitoring", e);
            }
        }
    }

    /**
     * Gets the configured quiet period.
     *
     * @return the quiet period duration
     */
    public Duration getQuietPeriod() {
        return quietPeriod;
    }

    /**
     * Gets the configured maximum inflight requests threshold.
     *
     * @return the maximum inflight requests
     */
    public int getMaxInflightRequests() {
        return maxInflightRequests;
    }

    /**
     * Creates a new builder for constructing NetworkIdleWait instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "NetworkIdleWait{quietPeriod=" + quietPeriod +
               ", maxInflightRequests=" + maxInflightRequests + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkIdleWait that = (NetworkIdleWait) o;
        return maxInflightRequests == that.maxInflightRequests &&
               quietPeriod.equals(that.quietPeriod);
    }

    @Override
    public int hashCode() {
        int result = quietPeriod.hashCode();
        result = 31 * result + maxInflightRequests;
        return result;
    }

    /**
     * Builder for creating NetworkIdleWait instances with custom configuration.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * NetworkIdleWait wait = NetworkIdleWait.builder()
     *     .quietPeriod(Duration.ofSeconds(1))
     *     .maxInflightRequests(2)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private Duration quietPeriod = DEFAULT_QUIET_PERIOD;
        private int maxInflightRequests = DEFAULT_MAX_INFLIGHT_REQUESTS;

        private Builder() {
        }

        /**
         * Sets the quiet period duration.
         * <p>
         * The network must remain idle (with inflight requests &lt;= maxInflightRequests)
         * for this duration before the wait completes.
         * </p>
         *
         * @param quietPeriod the quiet period duration
         * @return this builder
         * @throws IllegalArgumentException if quietPeriod is null or not positive
         */
        public Builder quietPeriod(Duration quietPeriod) {
            if (quietPeriod == null) {
                throw new IllegalArgumentException("Quiet period cannot be null");
            }
            if (quietPeriod.isNegative() || quietPeriod.isZero()) {
                throw new IllegalArgumentException("Quiet period must be positive, got: " + quietPeriod);
            }
            this.quietPeriod = quietPeriod;
            return this;
        }

        /**
         * Sets the maximum number of allowed inflight requests during the quiet period.
         * <p>
         * When the number of inflight requests is at or below this threshold for the
         * quiet period duration, the network is considered idle.
         * </p>
         * <p>
         * Common values:
         * <ul>
         *   <li>0 (default) - Wait for all requests to complete</li>
         *   <li>1-2 - Allow a few long-polling or background requests</li>
         * </ul>
         * </p>
         *
         * @param maxInflightRequests the maximum inflight requests threshold
         * @return this builder
         * @throws IllegalArgumentException if maxInflightRequests is negative
         */
        public Builder maxInflightRequests(int maxInflightRequests) {
            if (maxInflightRequests < 0) {
                throw new IllegalArgumentException("Max inflight requests must be non-negative, got: " + maxInflightRequests);
            }
            this.maxInflightRequests = maxInflightRequests;
            return this;
        }

        /**
         * Builds a new NetworkIdleWait instance with the configured settings.
         *
         * @return a new NetworkIdleWait instance
         */
        public NetworkIdleWait build() {
            return new NetworkIdleWait(quietPeriod, maxInflightRequests);
        }
    }
}
