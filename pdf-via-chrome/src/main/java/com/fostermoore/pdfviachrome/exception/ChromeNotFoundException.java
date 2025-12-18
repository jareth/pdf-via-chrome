package com.fostermoore.pdfviachrome.exception;

/**
 * Exception thrown when the Chrome/Chromium executable cannot be found on the system.
 * <p>
 * This exception is typically thrown during Chrome browser startup when:
 * </p>
 * <ul>
 *   <li>No Chrome path is explicitly configured in {@link com.fostermoore.pdfviachrome.chrome.ChromeOptions}</li>
 *   <li>Auto-detection fails to find Chrome in standard installation locations</li>
 *   <li>The specified Chrome executable path does not exist or is not executable</li>
 * </ul>
 * <p>
 * To resolve this exception:
 * </p>
 * <ul>
 *   <li>Install Chrome or Chromium on the system</li>
 *   <li>Explicitly set the Chrome path using {@code ChromeOptions.Builder.chromePath(String)}</li>
 *   <li>Ensure the Chrome executable has proper permissions</li>
 * </ul>
 *
 * @see com.fostermoore.pdfviachrome.chrome.ChromeManager
 * @see com.fostermoore.pdfviachrome.chrome.ChromeOptions
 * @see com.fostermoore.pdfviachrome.util.ChromePathDetector
 */
public class ChromeNotFoundException extends PdfGenerationException {

    /**
     * Constructs a new Chrome not found exception with the specified detail message.
     *
     * @param message the detail message explaining why Chrome could not be found
     */
    public ChromeNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new Chrome not found exception with the specified detail message and cause.
     *
     * @param message the detail message explaining why Chrome could not be found
     * @param cause the underlying cause of this exception
     */
    public ChromeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new Chrome not found exception with the specified cause.
     * The detail message is set to the message of the cause, if available.
     *
     * @param cause the underlying cause of this exception
     */
    public ChromeNotFoundException(Throwable cause) {
        super(cause);
    }
}
