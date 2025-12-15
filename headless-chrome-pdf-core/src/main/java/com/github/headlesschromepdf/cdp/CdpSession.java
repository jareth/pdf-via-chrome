package com.github.headlesschromepdf.cdp;

import com.github.headlesschromepdf.exception.CdpConnectionException;
import com.github.kklisura.cdt.protocol.commands.*;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;
import com.github.kklisura.cdt.services.types.ChromeTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages a Chrome DevTools Protocol (CDP) session.
 *
 * This class implements AutoCloseable to ensure proper cleanup of WebSocket connections
 * using try-with-resources pattern. It wraps the ChromeDevToolsService and provides
 * thread-safe access to CDP domains for controlling Chrome.
 */
public class CdpSession implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(CdpSession.class);
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 30000;

    private final String webSocketUrl;
    private final int connectionTimeoutMs;
    private ChromeDevToolsService devToolsService;
    private volatile boolean closed = false;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new CDP session with the specified WebSocket URL.
     *
     * @param webSocketUrl the WebSocket debugger URL obtained from Chrome
     */
    public CdpSession(String webSocketUrl) {
        this(webSocketUrl, DEFAULT_CONNECTION_TIMEOUT_MS);
    }

    /**
     * Creates a new CDP session with the specified WebSocket URL and timeout.
     *
     * @param webSocketUrl the WebSocket debugger URL obtained from Chrome
     * @param connectionTimeoutMs the connection timeout in milliseconds
     */
    public CdpSession(String webSocketUrl, int connectionTimeoutMs) {
        if (webSocketUrl == null || webSocketUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("WebSocket URL cannot be null or empty");
        }
        if (connectionTimeoutMs <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }

        this.webSocketUrl = webSocketUrl;
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    /**
     * Establishes the WebSocket connection to Chrome's DevTools Protocol.
     *
     * @throws CdpConnectionException if the connection fails
     */
    public void connect() {
        lock.lock();
        try {
            if (closed) {
                throw new IllegalStateException("Cannot connect: CdpSession has been closed");
            }

            if (devToolsService != null) {
                throw new IllegalStateException("Already connected to Chrome DevTools");
            }

            logger.info("Connecting to Chrome DevTools at: {}", webSocketUrl);

            try {
                // Validate WebSocket URL format and extract host/port
                URI uri = new URI(webSocketUrl);
                if (!uri.getScheme().startsWith("ws")) {
                    throw new CdpConnectionException("Invalid WebSocket URL scheme: " + uri.getScheme());
                }

                String host = uri.getHost();
                int port = uri.getPort();

                if (host == null || port == -1) {
                    throw new CdpConnectionException("WebSocket URL must contain host and port: " + webSocketUrl);
                }

                // Create ChromeService to connect to the running Chrome instance
                ChromeService chromeService = new ChromeServiceImpl(host, port);

                // Get the list of tabs and find the one matching our WebSocket URL
                List<ChromeTab> tabs = chromeService.getTabs();
                ChromeTab targetTab = null;

                for (ChromeTab tab : tabs) {
                    if (webSocketUrl.equals(tab.getWebSocketDebuggerUrl())) {
                        targetTab = tab;
                        break;
                    }
                }

                if (targetTab == null) {
                    // If exact match not found, try to use the first available tab
                    if (!tabs.isEmpty()) {
                        logger.warn("Exact tab not found for WebSocket URL: {}, using first available tab", webSocketUrl);
                        targetTab = tabs.get(0);
                    } else {
                        throw new CdpConnectionException("No tabs found in Chrome instance");
                    }
                }

                // Create DevTools service for the tab
                devToolsService = chromeService.createDevToolsService(targetTab);

                logger.info("Successfully connected to Chrome DevTools");

                // Enable logging of CDP protocol events for debugging
                if (logger.isTraceEnabled()) {
                    enableProtocolLogging();
                }

            } catch (URISyntaxException e) {
                throw new CdpConnectionException("Invalid WebSocket URL format: " + webSocketUrl, e);
            } catch (Exception e) {
                throw new CdpConnectionException("Failed to establish WebSocket connection to Chrome", e);
            }

        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the Page domain for navigation and PDF generation.
     *
     * @return the Page domain
     * @throws IllegalStateException if not connected
     */
    public Page getPage() {
        ensureConnected();
        return devToolsService.getPage();
    }

    /**
     * Returns the Runtime domain for JavaScript execution.
     *
     * @return the Runtime domain
     * @throws IllegalStateException if not connected
     */
    public com.github.kklisura.cdt.protocol.commands.Runtime getRuntime() {
        ensureConnected();
        return devToolsService.getRuntime();
    }

    /**
     * Returns the Network domain for network monitoring.
     *
     * @return the Network domain
     * @throws IllegalStateException if not connected
     */
    public Network getNetwork() {
        ensureConnected();
        return devToolsService.getNetwork();
    }

    /**
     * Returns the Emulation domain for device emulation.
     *
     * @return the Emulation domain
     * @throws IllegalStateException if not connected
     */
    public Emulation getEmulation() {
        ensureConnected();
        return devToolsService.getEmulation();
    }

    /**
     * Returns the DOM domain for DOM manipulation.
     *
     * @return the DOM domain
     * @throws IllegalStateException if not connected
     */
    public DOM getDOM() {
        ensureConnected();
        return devToolsService.getDOM();
    }

    /**
     * Returns the Performance domain for performance monitoring.
     *
     * @return the Performance domain
     * @throws IllegalStateException if not connected
     */
    public Performance getPerformance() {
        ensureConnected();
        return devToolsService.getPerformance();
    }

    /**
     * Returns the Security domain for security features.
     *
     * @return the Security domain
     * @throws IllegalStateException if not connected
     */
    public Security getSecurity() {
        ensureConnected();
        return devToolsService.getSecurity();
    }

    /**
     * Checks if the session is currently connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        lock.lock();
        try {
            return devToolsService != null && !closed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the WebSocket URL used for this session.
     *
     * @return the WebSocket URL
     */
    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    /**
     * Closes the WebSocket connection and releases resources.
     * This method is idempotent and can be called multiple times safely.
     */
    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }

            closed = true;

            if (devToolsService != null) {
                logger.info("Closing Chrome DevTools session");

                try {
                    devToolsService.close();
                    logger.debug("Chrome DevTools session closed successfully");
                } catch (Exception e) {
                    logger.warn("Error while closing Chrome DevTools service", e);
                } finally {
                    devToolsService = null;
                }
            }

        } finally {
            lock.unlock();
        }
    }

    /**
     * Ensures the session is connected before accessing domains.
     *
     * @throws IllegalStateException if not connected
     */
    private void ensureConnected() {
        if (closed) {
            throw new IllegalStateException("CdpSession has been closed");
        }
        if (devToolsService == null) {
            throw new IllegalStateException("Not connected to Chrome DevTools. Call connect() first.");
        }
    }

    /**
     * Enables logging of CDP protocol events for debugging purposes.
     */
    private void enableProtocolLogging() {
        try {
            // Note: The chrome-devtools-java-client library logs protocol events
            // automatically when the appropriate logger (com.github.kklisura.cdt)
            // is set to TRACE level
            logger.trace("CDP protocol event logging is enabled");
        } catch (Exception e) {
            logger.debug("Failed to enable protocol logging", e);
        }
    }
}
