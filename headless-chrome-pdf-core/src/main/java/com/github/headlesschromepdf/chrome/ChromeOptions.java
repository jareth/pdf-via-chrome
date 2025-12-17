package com.github.headlesschromepdf.chrome;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration options for launching Chrome browser.
 *
 * This class provides a builder-style API for configuring Chrome launch parameters,
 * including headless mode, debugging port, user data directory, and custom flags.
 */
public class ChromeOptions {

    private final Path chromePath;
    private final boolean headless;
    private final int remoteDebuggingPort;
    private final Path userDataDir;
    private final List<String> additionalFlags;
    private final boolean disableGpu;
    private final boolean disableDevShmUsage;
    private final boolean noSandbox;
    private final String windowSize;
    private final int startupTimeoutSeconds;
    private final int shutdownTimeoutSeconds;

    private ChromeOptions(Builder builder) {
        this.chromePath = builder.chromePath;
        this.headless = builder.headless;
        this.remoteDebuggingPort = builder.remoteDebuggingPort;
        this.userDataDir = builder.userDataDir;
        this.additionalFlags = Collections.unmodifiableList(new ArrayList<>(builder.additionalFlags));
        this.disableGpu = builder.disableGpu;
        this.disableDevShmUsage = builder.disableDevShmUsage;
        this.noSandbox = builder.noSandbox;
        this.windowSize = builder.windowSize;
        this.startupTimeoutSeconds = builder.startupTimeoutSeconds;
        this.shutdownTimeoutSeconds = builder.shutdownTimeoutSeconds;
    }

    public Path getChromePath() {
        return chromePath;
    }

    public boolean isHeadless() {
        return headless;
    }

    public int getRemoteDebuggingPort() {
        return remoteDebuggingPort;
    }

    public Path getUserDataDir() {
        return userDataDir;
    }

    public List<String> getAdditionalFlags() {
        return additionalFlags;
    }

    public boolean isDisableGpu() {
        return disableGpu;
    }

    public boolean isDisableDevShmUsage() {
        return disableDevShmUsage;
    }

    public boolean isNoSandbox() {
        return noSandbox;
    }

    public String getWindowSize() {
        return windowSize;
    }

    public int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    /**
     * Creates a new builder for ChromeOptions.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing ChromeOptions instances.
     */
    public static class Builder {
        private Path chromePath;
        private boolean headless = true;
        private int remoteDebuggingPort = 0; // 0 means random port
        private Path userDataDir;
        private List<String> additionalFlags = new ArrayList<>();
        private boolean disableGpu = true;
        private boolean disableDevShmUsage = false;
        private boolean noSandbox = false;
        private String windowSize;
        private int startupTimeoutSeconds = 30;
        private int shutdownTimeoutSeconds = 5;

        /**
         * Sets the path to the Chrome executable.
         * If not set, ChromeManager will attempt to auto-detect Chrome.
         *
         * @param chromePath the path to Chrome executable
         * @return this builder
         */
        public Builder chromePath(Path chromePath) {
            this.chromePath = chromePath;
            return this;
        }

        /**
         * Sets whether to run Chrome in headless mode.
         * Default is true.
         *
         * @param headless true to run headless, false otherwise
         * @return this builder
         */
        public Builder headless(boolean headless) {
            this.headless = headless;
            return this;
        }

        /**
         * Sets the remote debugging port.
         * Use 0 (default) for a random available port.
         *
         * @param port the port number, or 0 for random (must be 0-65535)
         * @return this builder
         * @throws IllegalArgumentException if port is not in valid range
         */
        public Builder remoteDebuggingPort(int port) {
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException(
                    "Remote debugging port must be between 0 and 65535, got: " + port
                );
            }
            this.remoteDebuggingPort = port;
            return this;
        }

        /**
         * Sets the user data directory for Chrome.
         * If not set, a temporary directory will be created.
         *
         * @param userDataDir the user data directory path
         * @return this builder
         */
        public Builder userDataDir(Path userDataDir) {
            this.userDataDir = userDataDir;
            return this;
        }

        /**
         * Adds additional Chrome command-line flags.
         *
         * @param flag the flag to add
         * @return this builder
         * @throws IllegalArgumentException if flag is null
         */
        public Builder addFlag(String flag) {
            if (flag == null) {
                throw new IllegalArgumentException("Flag cannot be null");
            }
            this.additionalFlags.add(flag);
            return this;
        }

        /**
         * Adds multiple Chrome command-line flags.
         *
         * @param flags the flags to add
         * @return this builder
         * @throws IllegalArgumentException if flags is null or contains null elements
         */
        public Builder addFlags(List<String> flags) {
            if (flags == null) {
                throw new IllegalArgumentException("Flags list cannot be null");
            }
            // Check for null elements safely
            for (String flag : flags) {
                if (flag == null) {
                    throw new IllegalArgumentException("Flags list cannot contain null elements");
                }
            }
            this.additionalFlags.addAll(flags);
            return this;
        }

        /**
         * Sets whether to disable GPU hardware acceleration.
         * Default is true. This is useful for headless mode and Docker environments.
         *
         * @param disableGpu true to disable GPU, false otherwise
         * @return this builder
         */
        public Builder disableGpu(boolean disableGpu) {
            this.disableGpu = disableGpu;
            return this;
        }

        /**
         * Sets whether to disable /dev/shm usage.
         * This is useful in Docker environments where /dev/shm may be too small.
         * Default is false.
         *
         * @param disableDevShmUsage true to disable, false otherwise
         * @return this builder
         */
        public Builder disableDevShmUsage(boolean disableDevShmUsage) {
            this.disableDevShmUsage = disableDevShmUsage;
            return this;
        }

        /**
         * Sets whether to run Chrome without sandboxing.
         * WARNING: This reduces security and should only be used in trusted environments
         * like Docker containers where sandboxing may not work.
         * Default is false.
         *
         * @param noSandbox true to disable sandbox, false otherwise
         * @return this builder
         */
        public Builder noSandbox(boolean noSandbox) {
            this.noSandbox = noSandbox;
            return this;
        }

        /**
         * Sets the window size for Chrome.
         * This is useful for controlling the viewport size when generating PDFs.
         *
         * @param windowSize the window size in format "width,height" (e.g., "1920,1080")
         * @return this builder
         */
        public Builder windowSize(String windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        /**
         * Sets the window size for Chrome using width and height.
         * This is a convenience method for windowSize(String).
         *
         * @param width the window width in pixels
         * @param height the window height in pixels
         * @return this builder
         */
        public Builder withWindowSize(int width, int height) {
            this.windowSize = width + "," + height;
            return this;
        }

        /**
         * Applies Docker-optimized defaults.
         * Enables noSandbox and disableDevShmUsage flags which are typically
         * required for running Chrome in Docker containers.
         *
         * @return this builder
         */
        public Builder dockerDefaults() {
            this.noSandbox = true;
            this.disableDevShmUsage = true;
            return this;
        }

        /**
         * Sets the timeout in seconds for Chrome startup.
         * Default is 30 seconds.
         *
         * @param timeoutSeconds the timeout in seconds (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if timeoutSeconds is not positive
         */
        public Builder startupTimeout(int timeoutSeconds) {
            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException(
                    "Startup timeout must be positive, got: " + timeoutSeconds
                );
            }
            this.startupTimeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * Sets the timeout in seconds for graceful Chrome shutdown.
         * If the process doesn't terminate within this time, it will be forcibly killed.
         * Default is 5 seconds.
         *
         * @param timeoutSeconds the timeout in seconds (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if timeoutSeconds is not positive
         */
        public Builder shutdownTimeout(int timeoutSeconds) {
            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException(
                    "Shutdown timeout must be positive, got: " + timeoutSeconds
                );
            }
            this.shutdownTimeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * Builds the ChromeOptions instance.
         * Validates the Chrome path if provided.
         *
         * @return a new ChromeOptions instance
         * @throws IllegalArgumentException if chromePath is provided but doesn't exist or isn't executable
         */
        public ChromeOptions build() {
            // Validate chromePath if provided
            if (chromePath != null) {
                if (!java.nio.file.Files.exists(chromePath)) {
                    throw new IllegalArgumentException("Chrome path does not exist: " + chromePath);
                }
                if (!java.nio.file.Files.isExecutable(chromePath)) {
                    throw new IllegalArgumentException("Chrome path is not executable: " + chromePath);
                }
            }

            return new ChromeOptions(this);
        }
    }
}
