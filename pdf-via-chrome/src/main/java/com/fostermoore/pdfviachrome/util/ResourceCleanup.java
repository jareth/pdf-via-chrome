package com.fostermoore.pdfviachrome.util;

import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for managing cleanup of Chrome processes and resources via JVM shutdown hooks.
 *
 * This class maintains a global registry of active Chrome processes and ensures they are
 * terminated when the JVM exits, even if normal cleanup (AutoCloseable.close()) is not called.
 * This prevents zombie Chrome processes from accumulating.
 */
public class ResourceCleanup {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCleanup.class);

    /**
     * Global registry of Chrome processes and their associated shutdown hooks.
     * Key: ChromeProcess instance
     * Value: The shutdown hook Thread for that process
     */
    private static final Map<ChromeProcess, Thread> shutdownHooks = new ConcurrentHashMap<>();

    /**
     * Flag to track if JVM is shutting down to avoid removeShutdownHook() calls during shutdown.
     */
    private static volatile boolean isShuttingDown = false;

    static {
        // Register a master shutdown hook to set the isShuttingDown flag
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isShuttingDown = true;
            logger.debug("JVM shutdown detected");
        }, "ResourceCleanup-ShutdownDetector"));
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private ResourceCleanup() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Registers a shutdown hook for the given Chrome process.
     * The hook will terminate the process and clean up temporary directories on JVM exit.
     *
     * @param chromeProcess the Chrome process to register
     * @return the shutdown hook Thread that was registered
     */
    public static Thread registerShutdownHook(ChromeProcess chromeProcess) {
        if (chromeProcess == null) {
            throw new IllegalArgumentException("ChromeProcess cannot be null");
        }

        Thread shutdownHook = new Thread(() -> {
            logger.debug("Executing shutdown hook for Chrome process (PID: {})", chromeProcess.getPid());

            Process process = chromeProcess.getProcess();
            if (process.isAlive()) {
                logger.info("Forcibly terminating Chrome process (PID: {}) during JVM shutdown",
                           chromeProcess.getPid());
                process.destroyForcibly();

                try {
                    process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting for Chrome process to terminate");
                }
            }

            // Clean up temporary user data directory
            if (chromeProcess.isTemporaryUserDataDir()) {
                cleanupUserDataDir(chromeProcess.getUserDataDir());
            }
        }, "ChromeCleanup-PID-" + chromeProcess.getPid());

        Runtime.getRuntime().addShutdownHook(shutdownHook);
        shutdownHooks.put(chromeProcess, shutdownHook);

        logger.debug("Registered shutdown hook for Chrome process (PID: {})", chromeProcess.getPid());

        return shutdownHook;
    }

    /**
     * Removes the shutdown hook for the given Chrome process.
     * This should be called when the process is properly closed via normal cleanup.
     *
     * @param chromeProcess the Chrome process to unregister
     * @return true if the hook was successfully removed, false if already shutting down or hook not found
     */
    public static boolean removeShutdownHook(ChromeProcess chromeProcess) {
        if (chromeProcess == null) {
            return false;
        }

        // Don't try to remove shutdown hooks if JVM is already shutting down
        // This would throw IllegalStateException
        if (isShuttingDown) {
            logger.trace("JVM is shutting down, skipping shutdown hook removal");
            return false;
        }

        Thread shutdownHook = shutdownHooks.remove(chromeProcess);

        if (shutdownHook == null) {
            logger.debug("No shutdown hook found for Chrome process (PID: {})", chromeProcess.getPid());
            return false;
        }

        try {
            boolean removed = Runtime.getRuntime().removeShutdownHook(shutdownHook);
            if (removed) {
                logger.debug("Removed shutdown hook for Chrome process (PID: {})", chromeProcess.getPid());
            } else {
                logger.debug("Shutdown hook already executed for Chrome process (PID: {})",
                           chromeProcess.getPid());
            }
            return removed;
        } catch (IllegalStateException e) {
            // This can happen if JVM is shutting down between our isShuttingDown check and this call
            logger.trace("Could not remove shutdown hook (JVM may be shutting down)", e);
            return false;
        }
    }

    /**
     * Gets the number of currently registered shutdown hooks.
     * This is primarily useful for testing.
     *
     * @return the number of active shutdown hooks
     */
    public static int getRegisteredHookCount() {
        return shutdownHooks.size();
    }

    /**
     * Checks if a shutdown hook is registered for the given Chrome process.
     *
     * @param chromeProcess the Chrome process to check
     * @return true if a hook is registered, false otherwise
     */
    public static boolean isHookRegistered(ChromeProcess chromeProcess) {
        return chromeProcess != null && shutdownHooks.containsKey(chromeProcess);
    }

    /**
     * Checks if the JVM is currently shutting down.
     *
     * @return true if shutting down, false otherwise
     */
    public static boolean isShuttingDown() {
        return isShuttingDown;
    }

    /**
     * Cleans up a user data directory by recursively deleting it.
     *
     * @param userDataDir the directory to clean up
     */
    private static void cleanupUserDataDir(Path userDataDir) {
        if (userDataDir == null) {
            return;
        }

        logger.debug("Cleaning up user data directory during shutdown: {}", userDataDir);

        // On Windows, Chrome may hold file locks for a short time after termination.
        // Wait briefly to allow the OS to release file handles.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Interrupted while waiting before cleanup");
        }

        // Try deletion with retries for Windows file lock issues
        deleteDirectoryWithRetries(userDataDir, 3, 200);
    }

    /**
     * Attempts to delete a directory with retry logic for file lock issues.
     *
     * @param directory the directory to delete
     * @param maxRetries maximum number of retry attempts
     * @param delayMs delay in milliseconds between retries
     */
    private static void deleteDirectoryWithRetries(Path directory, int maxRetries, long delayMs) {
        if (!Files.exists(directory)) {
            return;
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                deleteDirectoryRecursively(directory);

                // Check if directory was successfully deleted
                if (!Files.exists(directory)) {
                    logger.debug("Successfully deleted user data directory: {}", directory);
                    return;
                }

                // Directory still exists, try again
                if (attempt < maxRetries) {
                    logger.trace("Directory still exists after deletion attempt {}, retrying...", attempt);
                    Thread.sleep(delayMs);
                }

            } catch (IOException e) {
                if (attempt == maxRetries) {
                    // Only log as warning on final attempt
                    logger.warn("Failed to delete user data directory after {} attempts during shutdown: {} ({})",
                               maxRetries, directory, e.getMessage());
                } else {
                    logger.trace("Deletion attempt {} failed, will retry: {}", attempt, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.debug("Interrupted during retry delay");
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Interrupted during directory deletion");
                return;
            }
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     * Uses best-effort deletion - continues even if individual files fail to delete.
     *
     * @param directory the directory to delete
     * @throws IOException if the directory walk fails
     */
    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {
            stream.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                  .forEach(path -> {
                      try {
                          Files.deleteIfExists(path);
                      } catch (IOException e) {
                          // Only log at TRACE level to reduce noise in test output
                          // DirectoryNotEmptyException and file lock errors are common on Windows
                          logger.trace("Failed to delete: {} ({})", path, e.getClass().getSimpleName());
                      }
                  });
        }
    }
}
