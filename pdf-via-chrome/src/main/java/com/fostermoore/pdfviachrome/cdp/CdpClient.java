package com.fostermoore.pdfviachrome.cdp;

import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import com.fostermoore.pdfviachrome.exception.CdpConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating CDP sessions.
 *
 * This class provides convenient methods for creating and configuring
 * CdpSession instances from Chrome processes or WebSocket URLs.
 */
public class CdpClient {

    private static final Logger logger = LoggerFactory.getLogger(CdpClient.class);

    /**
     * Creates and connects a new CDP session from a Chrome process.
     *
     * @param chromeProcess the Chrome process containing the WebSocket URL
     * @return a connected CdpSession
     * @throws CdpConnectionException if connection fails
     * @throws IllegalArgumentException if chromeProcess is null or not alive
     */
    public static CdpSession createSession(ChromeProcess chromeProcess) {
        if (chromeProcess == null) {
            throw new IllegalArgumentException("ChromeProcess cannot be null");
        }
        if (!chromeProcess.isAlive()) {
            throw new IllegalArgumentException("Chrome process is not running");
        }

        String webSocketUrl = chromeProcess.getWebSocketDebuggerUrl();
        return createSession(webSocketUrl);
    }


    /**
     * Creates and connects a new CDP session from a WebSocket URL.
     *
     * @param webSocketUrl the WebSocket debugger URL
     * @return a connected CdpSession
     * @throws CdpConnectionException if connection fails
     * @throws IllegalArgumentException if webSocketUrl is null or empty
     */
    public static CdpSession createSession(String webSocketUrl) {
        logger.debug("Creating CDP session for WebSocket URL: {}", webSocketUrl);

        CdpSession session = new CdpSession(webSocketUrl);
        session.connect();

        return session;
    }

    /**
     * Builder for creating CDP sessions with custom configuration.
     */
    public static class Builder {
        private String webSocketUrl;
        private ChromeProcess chromeProcess;

        /**
         * Sets the WebSocket URL for the CDP session.
         *
         * @param webSocketUrl the WebSocket debugger URL
         * @return this builder
         */
        public Builder webSocketUrl(String webSocketUrl) {
            this.webSocketUrl = webSocketUrl;
            return this;
        }

        /**
         * Sets the Chrome process to extract the WebSocket URL from.
         *
         * @param chromeProcess the Chrome process
         * @return this builder
         */
        public Builder chromeProcess(ChromeProcess chromeProcess) {
            this.chromeProcess = chromeProcess;
            return this;
        }

        /**
         * Builds and connects the CDP session.
         *
         * @return a connected CdpSession
         * @throws CdpConnectionException if connection fails
         * @throws IllegalStateException if neither webSocketUrl nor chromeProcess is set
         */
        public CdpSession build() {
            if (chromeProcess != null) {
                return createSession(chromeProcess);
            } else if (webSocketUrl != null) {
                return createSession(webSocketUrl);
            } else {
                throw new IllegalStateException(
                    "Either webSocketUrl or chromeProcess must be set");
            }
        }
    }

    /**
     * Creates a new builder for CDP session configuration.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
