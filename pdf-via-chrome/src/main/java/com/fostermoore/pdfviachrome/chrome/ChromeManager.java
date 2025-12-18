package com.fostermoore.pdfviachrome.chrome;

import com.fostermoore.pdfviachrome.exception.BrowserTimeoutException;
import com.fostermoore.pdfviachrome.exception.ChromeNotFoundException;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.fostermoore.pdfviachrome.util.ChromePathDetector;
import com.fostermoore.pdfviachrome.util.ProcessRegistry;
import com.fostermoore.pdfviachrome.util.ResourceCleanup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the lifecycle of a Chrome browser process.
 *
 * This class implements AutoCloseable to ensure proper cleanup of browser processes
 * using try-with-resources pattern. It handles launching Chrome with specified options,
 * tracking the process, and graceful shutdown.
 */
public class ChromeManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ChromeManager.class);
    private static final Pattern WS_URL_PATTERN = Pattern.compile("(ws://[^\\s]+)");

    private final ChromeOptions options;
    private ChromeProcess chromeProcess;
    private Thread shutdownHook;
    private volatile boolean closed = false;

    /**
     * Creates a new ChromeManager with the specified options.
     *
     * @param options the Chrome configuration options
     */
    public ChromeManager(ChromeOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("ChromeOptions cannot be null");
        }
        this.options = options;
    }

    /**
     * Starts the Chrome browser process and returns the ChromeProcess instance.
     *
     * @return the ChromeProcess containing the process and CDP endpoint
     * @throws ChromeNotFoundException if Chrome executable cannot be found
     * @throws BrowserTimeoutException if Chrome startup exceeds the timeout
     * @throws PdfGenerationException if Chrome fails to start for other reasons
     */
    public ChromeProcess start() {
        if (closed) {
            throw new IllegalStateException("ChromeManager has been closed");
        }
        if (chromeProcess != null && chromeProcess.isAlive()) {
            throw new IllegalStateException("Chrome is already running");
        }

        logger.info("Starting Chrome browser...");

        // Determine Chrome path
        Path chromePath = resolveChromePath();
        logger.debug("Using Chrome executable: {}", chromePath);

        // Prepare user data directory
        Path userDataDir = prepareUserDataDir();
        boolean isTemporaryUserDataDir = (options.getUserDataDir() == null);
        logger.debug("Using user data directory: {} (temporary: {})", userDataDir, isTemporaryUserDataDir);

        // Build command
        List<String> command = buildChromeCommand(chromePath, userDataDir);
        logger.debug("Chrome command: {}", String.join(" ", command));

        // Launch process
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stderr into stdout for easier parsing
            Process process = processBuilder.start();

            // Extract WebSocket URL from output
            String webSocketUrl = extractWebSocketUrl(process);

            chromeProcess = new ChromeProcess(process, webSocketUrl, userDataDir, isTemporaryUserDataDir);
            logger.info("Chrome started successfully. PID: {}, WebSocket URL: {}",
                       chromeProcess.getPid(), chromeProcess.getWebSocketDebuggerUrl());

            // Register process in global registry for tracking
            ProcessRegistry.getInstance().register(process);

            // Register shutdown hook to ensure cleanup on abnormal JVM termination
            shutdownHook = ResourceCleanup.registerShutdownHook(chromeProcess);

            return chromeProcess;

        } catch (IOException e) {
            cleanupUserDataDir(userDataDir, isTemporaryUserDataDir);
            throw new PdfGenerationException("Failed to start Chrome process", e);
        } catch (TimeoutException e) {
            cleanupUserDataDir(userDataDir, isTemporaryUserDataDir);
            throw new BrowserTimeoutException("Timeout waiting for Chrome to start", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupUserDataDir(userDataDir, isTemporaryUserDataDir);
            throw new PdfGenerationException("Interrupted while starting Chrome", e);
        }
    }

    /**
     * Checks if Chrome is currently running.
     *
     * @return true if Chrome is running, false otherwise
     */
    public boolean isRunning() {
        return chromeProcess != null && chromeProcess.isAlive();
    }

    /**
     * Gets the current ChromeProcess instance.
     *
     * @return the ChromeProcess, or null if Chrome is not running
     */
    public ChromeProcess getChromeProcess() {
        return chromeProcess;
    }

    /**
     * Closes the Chrome browser process and cleans up resources.
     * This method is idempotent and can be called multiple times safely.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        if (chromeProcess == null) {
            logger.debug("No Chrome process to close");
            return;
        }

        logger.info("Closing Chrome process (PID: {})", chromeProcess.getPid());

        Process process = chromeProcess.getProcess();

        // Unregister from process registry
        ProcessRegistry.getInstance().unregister(process);

        // Remove shutdown hook since we're doing proper cleanup
        if (shutdownHook != null) {
            ResourceCleanup.removeShutdownHook(chromeProcess);
            shutdownHook = null;
        }

        // Try graceful shutdown first
        if (process.isAlive()) {
            logger.debug("Attempting graceful shutdown...");
            process.destroy();

            try {
                boolean terminated = process.waitFor(options.getShutdownTimeoutSeconds(), TimeUnit.SECONDS);

                if (!terminated) {
                    logger.warn("Chrome did not terminate gracefully within {} seconds, forcing shutdown",
                               options.getShutdownTimeoutSeconds());
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }

                logger.info("Chrome process terminated successfully");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for Chrome to terminate, forcing shutdown");
                process.destroyForcibly();
            }
        }

        // Clean up temporary user data directory
        if (chromeProcess.isTemporaryUserDataDir()) {
            cleanupUserDataDir(chromeProcess.getUserDataDir(), true);
        }
    }

    /**
     * Resolves the Chrome executable path, either from options or auto-detection.
     *
     * @return the Chrome executable path
     * @throws ChromeNotFoundException if Chrome cannot be found
     */
    private Path resolveChromePath() {
        Path chromePath = options.getChromePath();

        if (chromePath == null) {
            logger.debug("Chrome path not specified, attempting auto-detection...");
            Optional<Path> detected = ChromePathDetector.detectChromePath();

            if (detected.isEmpty()) {
                throw new ChromeNotFoundException(
                    "Chrome executable not found. Please specify the path explicitly using ChromeOptions.chromePath()");
            }

            chromePath = detected.get();
            logger.debug("Auto-detected Chrome at: {}", chromePath);
        }

        if (!Files.exists(chromePath)) {
            throw new ChromeNotFoundException("Chrome executable not found at: " + chromePath);
        }

        return chromePath;
    }

    /**
     * Prepares the user data directory for Chrome.
     *
     * @return the user data directory path
     * @throws PdfGenerationException if the directory cannot be created
     */
    private Path prepareUserDataDir() {
        Path userDataDir = options.getUserDataDir();

        if (userDataDir == null) {
            try {
                userDataDir = Files.createTempDirectory("chrome-user-data-");
                logger.debug("Created temporary user data directory: {}", userDataDir);
            } catch (IOException e) {
                throw new PdfGenerationException("Failed to create temporary user data directory", e);
            }
        } else {
            try {
                Files.createDirectories(userDataDir);
            } catch (IOException e) {
                throw new PdfGenerationException("Failed to create user data directory: " + userDataDir, e);
            }
        }

        return userDataDir;
    }

    /**
     * Builds the Chrome command with all necessary flags.
     *
     * @param chromePath the Chrome executable path
     * @param userDataDir the user data directory
     * @return the command as a list of strings
     */
    private List<String> buildChromeCommand(Path chromePath, Path userDataDir) {
        List<String> command = new ArrayList<>();
        command.add(chromePath.toString());

        // Core flags
        if (options.isHeadless()) {
            command.add("--headless");
        }

        if (options.isDisableGpu()) {
            command.add("--disable-gpu");
        }

        command.add("--remote-debugging-port=" + options.getRemoteDebuggingPort());
        command.add("--remote-allow-origins=*");  // Required for Chrome 98+ to allow CDP WebSocket connections
        command.add("--user-data-dir=" + userDataDir.toString());

        // Docker/container support flags
        if (options.isDisableDevShmUsage()) {
            command.add("--disable-dev-shm-usage");
        }

        if (options.isNoSandbox()) {
            command.add("--no-sandbox");
        }

        // Window size
        if (options.getWindowSize() != null) {
            command.add("--window-size=" + options.getWindowSize());
        }

        // Additional common flags for stability
        command.add("--disable-background-networking");
        command.add("--disable-background-timer-throttling");
        command.add("--disable-backgrounding-occluded-windows");
        command.add("--disable-breakpad");
        command.add("--disable-client-side-phishing-detection");
        command.add("--disable-component-extensions-with-background-pages");
        command.add("--disable-default-apps");
        command.add("--disable-extensions");
        command.add("--disable-features=TranslateUI");
        command.add("--disable-hang-monitor");
        command.add("--disable-ipc-flooding-protection");
        command.add("--disable-popup-blocking");
        command.add("--disable-prompt-on-repost");
        command.add("--disable-renderer-backgrounding");
        command.add("--disable-sync");
        command.add("--metrics-recording-only");
        command.add("--no-first-run");
        command.add("--safebrowsing-disable-auto-update");
        command.add("--password-store=basic");
        command.add("--use-mock-keychain");

        // Add any additional custom flags
        command.addAll(options.getAdditionalFlags());

        // Add initial URL to ensure Chrome creates a fresh tab on startup
        // This is critical for CDP connections to work reliably
        command.add("about:blank");

        return command;
    }

    /**
     * Extracts the WebSocket debugger URL from Chrome's output.
     *
     * @param process the Chrome process
     * @return the WebSocket URL
     * @throws TimeoutException if the URL is not found within the timeout period
     * @throws InterruptedException if the thread is interrupted
     */
    private String extractWebSocketUrl(Process process) throws TimeoutException, InterruptedException {
        CompletableFuture<String> urlFuture = CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.trace("Chrome output: {}", line);

                    Matcher matcher = WS_URL_PATTERN.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading Chrome output", e);
            }
            return null;
        });

        try {
            String url = urlFuture.get(options.getStartupTimeoutSeconds(), TimeUnit.SECONDS);

            if (url == null) {
                if (!process.isAlive()) {
                    throw new TimeoutException("Chrome process terminated before WebSocket URL was found");
                }
                throw new TimeoutException("WebSocket URL not found in Chrome output within timeout period");
            }

            return url;

        } catch (java.util.concurrent.ExecutionException e) {
            throw new TimeoutException("Error extracting WebSocket URL: " + e.getCause().getMessage());
        }
    }

    /**
     * Cleans up the user data directory if it's temporary.
     *
     * @param userDataDir the directory to clean up
     * @param isTemporary whether the directory is temporary
     */
    private void cleanupUserDataDir(Path userDataDir, boolean isTemporary) {
        if (!isTemporary || userDataDir == null) {
            return;
        }

        try {
            logger.debug("Cleaning up temporary user data directory: {}", userDataDir);
            deleteDirectoryRecursively(userDataDir);
        } catch (IOException e) {
            logger.warn("Failed to delete temporary user data directory: {}", userDataDir, e);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param directory the directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {
            stream.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                  .forEach(path -> {
                      try {
                          Files.deleteIfExists(path);
                      } catch (IOException e) {
                          logger.debug("Failed to delete: {}", path, e);
                      }
                  });
        }
    }
}
