package com.fostermoore.pdfviachrome.exception;

/**
 * Exception thrown when page navigation or loading fails.
 * <p>
 * This exception is thrown in various page loading failure scenarios:
 * </p>
 * <ul>
 *   <li>HTTP errors (404, 500, etc.) when navigating to a URL</li>
 *   <li>Network errors preventing page access (DNS resolution, connection refused, etc.)</li>
 *   <li>Invalid URLs or malformed HTML content</li>
 *   <li>Page crashes or renderer process failures</li>
 *   <li>Certificate errors for HTTPS pages (unless ignored in options)</li>
 *   <li>JavaScript errors that prevent page rendering</li>
 *   <li>Content Security Policy violations blocking resources</li>
 * </ul>
 * <p>
 * Note: This exception is distinct from {@link BrowserTimeoutException}. PageLoadException
 * indicates the page failed to load due to an error, while BrowserTimeoutException indicates
 * the page took too long to load.
 * </p>
 * <p>
 * To resolve this exception:
 * </p>
 * <ul>
 *   <li>Verify the URL is correct and accessible</li>
 *   <li>Check network connectivity and DNS resolution</li>
 *   <li>Ensure the HTML content is valid and well-formed</li>
 *   <li>Review browser console errors for JavaScript issues</li>
 *   <li>Configure Chrome options to ignore certificate errors if needed (not recommended for production)</li>
 * </ul>
 *
 * @see BrowserTimeoutException
 * @see com.fostermoore.pdfviachrome.chrome.ChromeOptions
 */
public class PageLoadException extends PdfGenerationException {

    /**
     * Constructs a new page load exception with the specified detail message.
     *
     * @param message the detail message explaining why the page failed to load
     */
    public PageLoadException(String message) {
        super(message);
    }

    /**
     * Constructs a new page load exception with the specified detail message and cause.
     *
     * @param message the detail message explaining why the page failed to load
     * @param cause the underlying cause of this exception
     */
    public PageLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new page load exception with the specified cause.
     * The detail message is set to the message of the cause, if available.
     *
     * @param cause the underlying cause of this exception
     */
    public PageLoadException(Throwable cause) {
        super(cause);
    }
}
