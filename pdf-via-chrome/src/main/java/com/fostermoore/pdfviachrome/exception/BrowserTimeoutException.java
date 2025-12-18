package com.fostermoore.pdfviachrome.exception;

/**
 * Exception thrown when a browser operation exceeds its configured timeout.
 * <p>
 * This exception is thrown in various timeout scenarios:
 * </p>
 * <ul>
 *   <li>Chrome browser startup takes longer than the configured startup timeout</li>
 *   <li>WebSocket connection to Chrome DevTools Protocol cannot be established within the timeout</li>
 *   <li>Page navigation or loading exceeds the page load timeout</li>
 *   <li>JavaScript execution times out</li>
 *   <li>PDF rendering operation exceeds its timeout</li>
 *   <li>Wait strategies (e.g., network idle, element presence) time out</li>
 * </ul>
 * <p>
 * To resolve this exception:
 * </p>
 * <ul>
 *   <li>Increase timeout values in {@link com.fostermoore.pdfviachrome.chrome.ChromeOptions}</li>
 *   <li>Optimize the page being rendered (reduce resource loading, scripts, etc.)</li>
 *   <li>Ensure adequate system resources (CPU, memory) are available</li>
 *   <li>Check for network connectivity issues if loading remote URLs</li>
 * </ul>
 *
 * @see com.fostermoore.pdfviachrome.chrome.ChromeOptions
 * @see com.fostermoore.pdfviachrome.chrome.ChromeManager
 */
public class BrowserTimeoutException extends PdfGenerationException {

    /**
     * Constructs a new browser timeout exception with the specified detail message.
     *
     * @param message the detail message explaining what operation timed out
     */
    public BrowserTimeoutException(String message) {
        super(message);
    }

    /**
     * Constructs a new browser timeout exception with the specified detail message and cause.
     *
     * @param message the detail message explaining what operation timed out
     * @param cause the underlying cause of this exception
     */
    public BrowserTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new browser timeout exception with the specified cause.
     * The detail message is set to the message of the cause, if available.
     *
     * @param cause the underlying cause of this exception
     */
    public BrowserTimeoutException(Throwable cause) {
        super(cause);
    }
}
