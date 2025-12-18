package com.fostermoore.pdfviachrome.exception;

/**
 * Base exception for all PDF generation failures in the headless-chrome-pdf library.
 * <p>
 * This is an unchecked exception (extends {@link RuntimeException}) that serves as the parent
 * for all library-specific exceptions. This allows library users to catch all PDF generation
 * errors with a single catch block, or handle specific error types individually.
 * </p>
 * <p>
 * Common scenarios where this exception or its subclasses may be thrown:
 * </p>
 * <ul>
 *   <li>Chrome browser cannot be found or started</li>
 *   <li>Chrome DevTools Protocol connection fails</li>
 *   <li>Browser operations timeout</li>
 *   <li>Page navigation or loading fails</li>
 *   <li>PDF rendering encounters errors</li>
 * </ul>
 *
 * @see ChromeNotFoundException
 * @see BrowserTimeoutException
 * @see PageLoadException
 * @see CdpConnectionException
 */
public class PdfGenerationException extends RuntimeException {

    /**
     * Constructs a new PDF generation exception with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public PdfGenerationException(String message) {
        super(message);
    }

    /**
     * Constructs a new PDF generation exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new PDF generation exception with the specified cause.
     * The detail message is set to the message of the cause, if available.
     *
     * @param cause the underlying cause of this exception
     */
    public PdfGenerationException(Throwable cause) {
        super(cause);
    }
}
