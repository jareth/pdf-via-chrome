package com.fostermoore.pdfviachrome.wait;

import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * A wait strategy that executes custom JavaScript and waits for it to return a truthy value.
 * <p>
 * This strategy provides maximum flexibility for complex waiting conditions by allowing
 * arbitrary JavaScript expressions. It's useful when built-in wait strategies don't meet
 * your specific requirements.
 * </p>
 *
 * <h2>How It Works</h2>
 * <p>
 * The strategy uses the Chrome DevTools Protocol Runtime domain to execute the provided
 * JavaScript expression. It polls at regular intervals until the expression returns a
 * truthy value or the timeout is reached.
 * </p>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li><b>javascriptExpression</b> - The JavaScript code to execute (required)</li>
 *   <li><b>pollInterval</b> - The interval between evaluations (default: 100ms)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Wait for a global variable to be set
 * WaitStrategy wait = CustomConditionWait.builder()
 *     .expression("typeof window.myApp !== 'undefined'")
 *     .build();
 * wait.await(cdpService, Duration.ofSeconds(10));
 *
 * // Wait for application to be ready
 * WaitStrategy wait = CustomConditionWait.builder()
 *     .expression("window.myApp && window.myApp.ready === true")
 *     .build();
 * wait.await(cdpService, Duration.ofSeconds(10));
 *
 * // Wait for all images to be loaded
 * WaitStrategy wait = CustomConditionWait.builder()
 *     .expression("Array.from(document.images).every(img => img.complete)")
 *     .pollInterval(Duration.ofMillis(200))
 *     .build();
 * wait.await(cdpService, Duration.ofSeconds(30));
 *
 * // Wait for data attribute to have a specific value
 * WaitStrategy wait = CustomConditionWait.builder()
 *     .expression("document.body.dataset.status === 'ready'")
 *     .build();
 * wait.await(cdpService, Duration.ofSeconds(10));
 * }</pre>
 *
 * <h2>JavaScript Expression Requirements</h2>
 * <p>
 * The JavaScript expression should:
 * <ul>
 *   <li>Return a truthy value when the condition is met</li>
 *   <li>Return a falsy value when the condition is not yet met</li>
 *   <li>Be safe to execute repeatedly (idempotent)</li>
 *   <li>Not modify the page state</li>
 * </ul>
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * If the JavaScript expression throws an error or fails to evaluate, the strategy
 * will log the error and continue polling. This allows for expressions that might
 * reference objects that don't exist yet (e.g., {@code window.myApp.ready} when
 * {@code window.myApp} hasn't been created yet).
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
public class CustomConditionWait implements WaitStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CustomConditionWait.class);

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

    private final String javascriptExpression;
    private final Duration pollInterval;

    /**
     * Creates a new CustomConditionWait with the specified configuration.
     *
     * @param javascriptExpression the JavaScript expression to evaluate
     * @param pollInterval the interval between evaluations
     * @throws IllegalArgumentException if javascriptExpression is null/empty, or pollInterval is null/negative
     */
    private CustomConditionWait(String javascriptExpression, Duration pollInterval) {
        if (javascriptExpression == null || javascriptExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("JavaScript expression cannot be null or empty");
        }
        if (pollInterval == null) {
            throw new IllegalArgumentException("Poll interval cannot be null");
        }
        if (pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("Poll interval must be positive, got: " + pollInterval);
        }

        this.javascriptExpression = javascriptExpression.trim();
        this.pollInterval = pollInterval;
    }

    /**
     * Waits until the JavaScript expression evaluates to a truthy value.
     * <p>
     * This method polls the JavaScript expression at regular intervals using JavaScript execution
     * via the CDP Runtime domain. It blocks until either:
     * <ul>
     *   <li>The expression evaluates to a truthy value</li>
     *   <li>The timeout is reached</li>
     *   <li>The thread is interrupted</li>
     * </ul>
     * </p>
     *
     * @param cdp the Chrome DevTools Service for interacting with the browser
     * @param timeout the maximum time to wait for the condition to become true
     * @throws TimeoutException if the condition does not become true within the timeout
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

        logger.debug("Starting custom condition wait (expression='{}', pollInterval={}, timeout={})",
                truncateForLog(javascriptExpression), pollInterval, timeout);

        Runtime runtime = cdp.getRuntime();
        runtime.enable();

        try {
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeout.toMillis();
            long pollIntervalMillis = pollInterval.toMillis();

            while (true) {
                // Evaluate the JavaScript expression
                if (isConditionMet(runtime)) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    logger.debug("Custom condition met after {} ms", elapsedTime);
                    return;
                }

                // Check if we've exceeded the timeout
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime >= timeoutMillis) {
                    logger.warn("Custom condition not met after {} ms: {}", elapsedTime, truncateForLog(javascriptExpression));
                    throw new TimeoutException("Custom condition not met within " + timeout +
                            ": " + truncateForLog(javascriptExpression));
                }

                // Wait before next poll, but don't exceed timeout
                long remainingTime = timeoutMillis - elapsedTime;
                long sleepTime = Math.min(pollIntervalMillis, remainingTime);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    logger.warn("Custom condition wait interrupted");
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
     * Checks if the custom condition is met by evaluating the JavaScript expression.
     *
     * @param runtime the CDP Runtime domain
     * @return true if the condition is met (expression returns truthy value), false otherwise
     */
    private boolean isConditionMet(Runtime runtime) {
        try {
            logger.trace("Evaluating: {}", truncateForLog(javascriptExpression));
            Evaluate result = runtime.evaluate(javascriptExpression);

            if (result == null || result.getResult() == null) {
                logger.trace("Evaluation returned null result");
                return false;
            }

            // Check if there was an exception during evaluation
            if (result.getExceptionDetails() != null) {
                logger.debug("JavaScript evaluation error (will continue polling): {}",
                        result.getExceptionDetails().getText());
                return false;
            }

            Object value = result.getResult().getValue();
            boolean conditionMet = isTruthy(value);
            logger.trace("Condition check result: {}", conditionMet);
            return conditionMet;

        } catch (Exception e) {
            logger.debug("Error evaluating JavaScript expression (will continue polling): {}", e.getMessage());
            return false;
        }
    }

    /**
     * Determines if a value is "truthy" according to JavaScript semantics.
     * <p>
     * The following values are considered falsy:
     * <ul>
     *   <li>null</li>
     *   <li>false</li>
     *   <li>0 (number zero)</li>
     *   <li>0.0 (double zero)</li>
     *   <li>"" (empty string)</li>
     * </ul>
     * All other values are considered truthy.
     * </p>
     *
     * @param value the value to check
     * @return true if the value is truthy, false if falsy
     */
    private boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            Number num = (Number) value;
            return num.doubleValue() != 0.0;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        // All other object types are truthy
        return true;
    }

    /**
     * Truncates a string for logging purposes.
     *
     * @param str the string to truncate
     * @return the truncated string (max 100 characters)
     */
    private String truncateForLog(String str) {
        if (str.length() <= 100) {
            return str;
        }
        return str.substring(0, 97) + "...";
    }

    /**
     * Gets the configured JavaScript expression.
     *
     * @return the JavaScript expression
     */
    public String getJavascriptExpression() {
        return javascriptExpression;
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
     * Creates a new builder for constructing CustomConditionWait instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "CustomConditionWait{javascriptExpression='" + truncateForLog(javascriptExpression) + "'" +
               ", pollInterval=" + pollInterval + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomConditionWait that = (CustomConditionWait) o;
        return javascriptExpression.equals(that.javascriptExpression) &&
               pollInterval.equals(that.pollInterval);
    }

    @Override
    public int hashCode() {
        int result = javascriptExpression.hashCode();
        result = 31 * result + pollInterval.hashCode();
        return result;
    }

    /**
     * Builder for creating CustomConditionWait instances with custom configuration.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * CustomConditionWait wait = CustomConditionWait.builder()
     *     .expression("window.myApp && window.myApp.ready === true")
     *     .pollInterval(Duration.ofMillis(200))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String javascriptExpression;
        private Duration pollInterval = DEFAULT_POLL_INTERVAL;

        private Builder() {
        }

        /**
         * Sets the JavaScript expression to evaluate.
         * <p>
         * The expression should return a truthy value when the condition is met
         * and a falsy value when it's not yet met. Examples:
         * <ul>
         *   <li>{@code "typeof window.myApp !== 'undefined'"} - Check if variable exists</li>
         *   <li>{@code "window.myApp && window.myApp.ready === true"} - Check nested property</li>
         *   <li>{@code "document.querySelectorAll('.item').length >= 10"} - Count elements</li>
         *   <li>{@code "Array.from(document.images).every(img => img.complete)"} - Check all images loaded</li>
         * </ul>
         * </p>
         *
         * @param expression the JavaScript expression
         * @return this builder
         * @throws IllegalArgumentException if expression is null or empty
         */
        public Builder expression(String expression) {
            if (expression == null || expression.trim().isEmpty()) {
                throw new IllegalArgumentException("JavaScript expression cannot be null or empty");
            }
            this.javascriptExpression = expression.trim();
            return this;
        }

        /**
         * Sets the poll interval - how often to evaluate the JavaScript expression.
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
         * Builds a new CustomConditionWait instance with the configured settings.
         *
         * @return a new CustomConditionWait instance
         * @throws IllegalStateException if expression has not been set
         */
        public CustomConditionWait build() {
            if (javascriptExpression == null) {
                throw new IllegalStateException("JavaScript expression must be set before building");
            }
            return new CustomConditionWait(javascriptExpression, pollInterval);
        }
    }
}
