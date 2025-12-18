package com.fostermoore.pdfviachrome.chrome;

import java.nio.file.Path;

/**
 * Represents a running Chrome browser process with its associated metadata.
 *
 * This class wraps the actual Process instance and provides access to
 * the Chrome DevTools Protocol (CDP) WebSocket endpoint URL.
 */
public class ChromeProcess {

    private final Process process;
    private final String webSocketDebuggerUrl;
    private final Path userDataDir;
    private final boolean isTemporaryUserDataDir;

    /**
     * Creates a new ChromeProcess instance.
     *
     * @param process the underlying Process instance
     * @param webSocketDebuggerUrl the WebSocket URL for CDP connection
     * @param userDataDir the user data directory used by Chrome
     * @param isTemporaryUserDataDir whether the user data directory is temporary and should be cleaned up
     */
    public ChromeProcess(Process process, String webSocketDebuggerUrl, Path userDataDir, boolean isTemporaryUserDataDir) {
        if (process == null) {
            throw new IllegalArgumentException("Process cannot be null");
        }
        if (webSocketDebuggerUrl == null || webSocketDebuggerUrl.isEmpty()) {
            throw new IllegalArgumentException("WebSocket debugger URL cannot be null or empty");
        }
        this.process = process;
        this.webSocketDebuggerUrl = webSocketDebuggerUrl;
        this.userDataDir = userDataDir;
        this.isTemporaryUserDataDir = isTemporaryUserDataDir;
    }

    /**
     * Gets the underlying Process instance.
     *
     * @return the Process
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Gets the WebSocket debugger URL for connecting to Chrome DevTools Protocol.
     *
     * @return the WebSocket URL
     */
    public String getWebSocketDebuggerUrl() {
        return webSocketDebuggerUrl;
    }

    /**
     * Gets the user data directory used by this Chrome instance.
     *
     * @return the user data directory path
     */
    public Path getUserDataDir() {
        return userDataDir;
    }

    /**
     * Checks if the user data directory is temporary and should be cleaned up on close.
     *
     * @return true if temporary, false otherwise
     */
    public boolean isTemporaryUserDataDir() {
        return isTemporaryUserDataDir;
    }

    /**
     * Checks if the Chrome process is still running.
     *
     * @return true if the process is alive, false otherwise
     */
    public boolean isAlive() {
        return process.isAlive();
    }

    /**
     * Gets the process ID (PID) of the Chrome process.
     *
     * @return the process ID
     */
    public long getPid() {
        return process.pid();
    }

    @Override
    public String toString() {
        return "ChromeProcess{" +
                "pid=" + getPid() +
                ", webSocketDebuggerUrl='" + webSocketDebuggerUrl + '\'' +
                ", isAlive=" + isAlive() +
                '}';
    }
}
