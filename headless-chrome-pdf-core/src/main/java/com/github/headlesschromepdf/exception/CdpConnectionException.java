package com.github.headlesschromepdf.exception;

/**
 * Exception thrown when connection to Chrome DevTools Protocol (CDP) fails.
 * <p>
 * This exception is thrown when the library cannot establish or maintain a WebSocket
 * connection to the Chrome browser's DevTools Protocol interface. Common scenarios include:
 * </p>
 * <ul>
 *   <li>Chrome browser did not start properly or crashed before connection</li>
 *   <li>WebSocket URL could not be extracted from Chrome's debugging output</li>
 *   <li>WebSocket connection handshake failed</li>
 *   <li>Connection was refused or reset by the browser</li>
 *   <li>CDP protocol version mismatch</li>
 *   <li>Network or firewall issues blocking WebSocket connection</li>
 *   <li>Browser debugging port is already in use or inaccessible</li>
 * </ul>
 * <p>
 * The Chrome DevTools Protocol is essential for all browser control operations including
 * navigation, JavaScript execution, and PDF generation. If this connection cannot be
 * established, no PDF generation operations can proceed.
 * </p>
 * <p>
 * To resolve this exception:
 * </p>
 * <ul>
 *   <li>Ensure Chrome started successfully without errors</li>
 *   <li>Verify the debugging port is not blocked by firewall</li>
 *   <li>Check that no other process is using the configured debugging port</li>
 *   <li>Increase connection timeout if network latency is high</li>
 *   <li>Review Chrome startup logs for errors</li>
 * </ul>
 *
 * @see com.github.headlesschromepdf.cdp.CdpSession
 * @see com.github.headlesschromepdf.cdp.CdpClient
 * @see com.github.headlesschromepdf.chrome.ChromeOptions
 */
public class CdpConnectionException extends PdfGenerationException {

    /**
     * Constructs a new CDP connection exception with the specified detail message.
     *
     * @param message the detail message explaining why the CDP connection failed
     */
    public CdpConnectionException(String message) {
        super(message);
    }

    /**
     * Constructs a new CDP connection exception with the specified detail message and cause.
     *
     * @param message the detail message explaining why the CDP connection failed
     * @param cause the underlying cause of this exception
     */
    public CdpConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new CDP connection exception with the specified cause.
     * The detail message is set to the message of the cause, if available.
     *
     * @param cause the underlying cause of this exception
     */
    public CdpConnectionException(Throwable cause) {
        super(cause);
    }
}
