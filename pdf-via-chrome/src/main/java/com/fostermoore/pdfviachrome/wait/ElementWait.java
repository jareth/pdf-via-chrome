package com.fostermoore.pdfviachrome.wait;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * A wait strategy that waits for a specific element to be present in the DOM using a CSS selector.
 * <p>
 * This strategy is useful when you know a specific element indicates that the page is ready
 * for PDF generation. For example, waiting for a "loading complete" indicator or a specific
 * content element to appear.
 * </p>
 *
 * <h2>How It Works</h2>
 * <p>
 * The strategy uses the Chrome DevTools Protocol Runtime domain to execute JavaScript that
 * checks for element presence using {@code document.querySelector()}. It polls at regular
 * intervals until the element is found or the timeout is reached.
 * </p>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>cssSelector</b> - The CSS selector for the element to wait for (required)</li>
 *   <li><b>pollInterval</b> - The interval between checks (default: 100ms)</li>
 *   <li><b>waitForVisible</b> - Whether to wait for element to be visible, not just present (default: false)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Wait for an element with ID "content-loaded"
 * WaitStrategy elementWait = ElementWait.builder()
 *     .selector("#content-loaded")
 *     .build();
 * elementWait.await(cdpService, Duration.ofSeconds(10));
 *
 * // Wait for a visible element with custom poll interval
 * WaitStrategy elementWait = ElementWait.builder()
 *     .selector(".main-content")
 *     .waitForVisible(true)
 *     .pollInterval(Duration.ofMillis(200))
 *     .build();
 * elementWait.await(cdpService, Duration.ofSeconds(10));
 * }</pre>
 *
 * <h2>Visibility vs Presence</h2>
 * <p>
 * By default, this strategy only checks if the element exists in the DOM (is "present").
 * When {@code waitForVisible} is enabled, it additionally checks that the element is visible,
 * meaning it has non-zero dimensions and is not hidden by CSS.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe. Each invocation of {@link #await(ChromeDevToolsService, Duration)}
 * operates independently.
 * </p>
 *
 * @see WaitStrategy
 * @since 2.0.0
 */
public class ElementWait implements WaitStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ElementWait.class);

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

    private final String cssSelector;
    private final Duration pollInterval;
    private final boolean waitForVisible;

    /**
     * Creates a new ElementWait with the specified configuration.
     *
     * @param cssSelector the CSS selector for the element to wait for
     * @param pollInterval the interval between element presence checks
     * @param waitForVisible whether to wait for element to be visible (not just present)
     * @throws IllegalArgumentException if cssSelector is null/empty, or pollInterval is null/negative
     */
    private ElementWait(String cssSelector, Duration pollInterval, boolean waitForVisible) {
        if (cssSelector == null || cssSelector.trim().isEmpty()) {
            throw new IllegalArgumentException("CSS selector cannot be null or empty");
        }
        if (pollInterval == null) {
            throw new IllegalArgumentException("Poll interval cannot be null");
        }
        if (pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("Poll interval must be positive, got: " + pollInterval);
        }

        this.cssSelector = cssSelector.trim();
        this.pollInterval = pollInterval;
        this.waitForVisible = waitForVisible;
    }

    /**
     * Waits until the specified element is present (or visible) in the DOM.
     * <p>
     * This method polls the DOM at regular intervals using JavaScript execution
     * via the CDP Runtime domain. It blocks until either:
     * <ul>
     *   <li>The element is found (and visible, if configured)</li>
     *   <li>The timeout is reached</li>
     *   <li>The thread is interrupted</li>
     * </ul>
     * </p>
     *
     * @param cdp the Chrome DevTools Service for interacting with the browser
     * @param timeout the maximum time to wait for the element to appear
     * @throws TimeoutException if the element does not appear within the timeout
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

        logger.debug("Starting element wait for selector '{}' (waitForVisible={}, pollInterval={}, timeout={})",
                cssSelector, waitForVisible, pollInterval, timeout);

        Runtime runtime = cdp.getRuntime();
        runtime.enable();

        try {
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeout.toMillis();
            long pollIntervalMillis = pollInterval.toMillis();

            while (true) {
                // Check if element is present (and visible if required)
                if (isElementReady(runtime)) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    logger.debug("Element '{}' found after {} ms", cssSelector, elapsedTime);
                    return;
                }

                // Check if we've exceeded the timeout
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime >= timeoutMillis) {
                    String condition = waitForVisible ? "visible" : "present";
                    logger.warn("Element '{}' not {} after {} ms", cssSelector, condition, elapsedTime);
                    throw new TimeoutException("Element with selector '" + cssSelector +
                            "' was not " + condition + " within " + timeout);
                }

                // Wait before next poll, but don't exceed timeout
                long remainingTime = timeoutMillis - elapsedTime;
                long sleepTime = Math.min(pollIntervalMillis, remainingTime);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    logger.warn("Element wait interrupted for selector '{}'", cssSelector);
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        } finally {
            try {
                runtime.disable();
            } catch (Exception e) {
                logger.warn("Failed to disable Runtime domain", e);
            }
        }
    }

    /**
     * Checks if the element is ready according to the configured criteria.
     *
     * @param runtime the CDP Runtime domain
     * @return true if the element is ready, false otherwise
     */
    private boolean isElementReady(Runtime runtime) {
        try {
            String expression;
            if (waitForVisible) {
                // Check if element exists and is visible
                expression = buildVisibilityCheckScript(cssSelector);
            } else {
                // Check if element exists
                expression = "document.querySelector('" + escapeSelector(cssSelector) + "') !== null";
            }

            logger.trace("Evaluating: {}", expression);
            Evaluate result = runtime.evaluate(expression);

            if (result == null || result.getResult() == null) {
                logger.trace("Evaluation returned null result");
                return false;
            }

            Object value = result.getResult().getValue();
            boolean isReady = Boolean.TRUE.equals(value);
            logger.trace("Element check result: {}", isReady);
            return isReady;

        } catch (Exception e) {
            logger.debug("Error checking element presence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Builds a JavaScript expression that checks if an element is visible.
     * An element is considered visible if it exists, has non-zero dimensions,
     * and is not hidden by CSS.
     *
     * @param selector the CSS selector
     * @return JavaScript expression that evaluates to true if element is visible
     */
    private String buildVisibilityCheckScript(String selector) {
        String escapedSelector = escapeSelector(selector);
        return "(function() {" +
               "  var el = document.querySelector('" + escapedSelector + "');" +
               "  if (!el) return false;" +
               "  var rect = el.getBoundingClientRect();" +
               "  var style = window.getComputedStyle(el);" +
               "  return rect.width > 0 && rect.height > 0 && " +
               "         style.visibility !== 'hidden' && " +
               "         style.display !== 'none';" +
               "})()";
    }

    /**
     * Escapes single quotes in a CSS selector for use in JavaScript string literals.
     *
     * @param selector the selector to escape
     * @return the escaped selector
     */
    private String escapeSelector(String selector) {
        return selector.replace("'", "\\'");
    }

    /**
     * Gets the configured CSS selector.
     *
     * @return the CSS selector
     */
    public String getCssSelector() {
        return cssSelector;
    }

    /**
     * Gets the configured poll interval.
     *
     * @return the poll interval duration
     */
    public Duration getPollInterval() {
        return pollInterval;
    }

    /**
     * Gets whether this strategy waits for visibility.
     *
     * @return true if waiting for visibility, false if only checking presence
     */
    public boolean isWaitForVisible() {
        return waitForVisible;
    }

    /**
     * Creates a new builder for constructing ElementWait instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ElementWait{cssSelector='" + cssSelector + "'" +
               ", pollInterval=" + pollInterval +
               ", waitForVisible=" + waitForVisible + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementWait that = (ElementWait) o;
        return waitForVisible == that.waitForVisible &&
               cssSelector.equals(that.cssSelector) &&
               pollInterval.equals(that.pollInterval);
    }

    @Override
    public int hashCode() {
        int result = cssSelector.hashCode();
        result = 31 * result + pollInterval.hashCode();
        result = 31 * result + (waitForVisible ? 1 : 0);
        return result;
    }

    /**
     * Builder for creating ElementWait instances with custom configuration.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * ElementWait wait = ElementWait.builder()
     *     .selector("#my-element")
     *     .waitForVisible(true)
     *     .pollInterval(Duration.ofMillis(200))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String cssSelector;
        private Duration pollInterval = DEFAULT_POLL_INTERVAL;
        private boolean waitForVisible = false;

        private Builder() {
        }

        /**
         * Sets the CSS selector for the element to wait for.
         * <p>
         * This can be any valid CSS selector, such as:
         * <ul>
         *   <li>ID selector: {@code "#my-id"}</li>
         *   <li>Class selector: {@code ".my-class"}</li>
         *   <li>Tag selector: {@code "div"}</li>
         *   <li>Attribute selector: {@code "[data-loaded='true']"}</li>
         *   <li>Complex selector: {@code "div.content > p:first-child"}</li>
         * </ul>
         * </p>
         *
         * @param cssSelector the CSS selector
         * @return this builder
         * @throws IllegalArgumentException if cssSelector is null or empty
         */
        public Builder selector(String cssSelector) {
            if (cssSelector == null || cssSelector.trim().isEmpty()) {
                throw new IllegalArgumentException("CSS selector cannot be null or empty");
            }
            this.cssSelector = cssSelector.trim();
            return this;
        }

        /**
         * Sets the poll interval - how often to check for the element.
         * <p>
         * Shorter intervals provide faster response but consume more CPU.
         * Longer intervals are more efficient but may add latency.
         * </p>
         * <p>
         * Default: 100ms
         * </p>
         *
         * @param pollInterval the poll interval duration
         * @return this builder
         * @throws IllegalArgumentException if pollInterval is null or not positive
         */
        public Builder pollInterval(Duration pollInterval) {
            if (pollInterval == null) {
                throw new IllegalArgumentException("Poll interval cannot be null");
            }
            if (pollInterval.isNegative() || pollInterval.isZero()) {
                throw new IllegalArgumentException("Poll interval must be positive, got: " + pollInterval);
            }
            this.pollInterval = pollInterval;
            return this;
        }

        /**
         * Sets whether to wait for the element to be visible, not just present in the DOM.
         * <p>
         * When {@code true}, the strategy will wait until the element:
         * <ul>
         *   <li>Exists in the DOM</li>
         *   <li>Has non-zero width and height</li>
         *   <li>Is not hidden by CSS (display:none or visibility:hidden)</li>
         * </ul>
         * </p>
         * <p>
         * When {@code false} (default), the strategy only checks if the element exists in the DOM,
         * regardless of its visibility.
         * </p>
         *
         * @param waitForVisible whether to wait for visibility
         * @return this builder
         */
        public Builder waitForVisible(boolean waitForVisible) {
            this.waitForVisible = waitForVisible;
            return this;
        }

        /**
         * Builds a new ElementWait instance with the configured settings.
         *
         * @return a new ElementWait instance
         * @throws IllegalStateException if selector has not been set
         */
        public ElementWait build() {
            if (cssSelector == null) {
                throw new IllegalStateException("CSS selector must be set before building");
            }
            return new ElementWait(cssSelector, pollInterval, waitForVisible);
        }
    }
}
