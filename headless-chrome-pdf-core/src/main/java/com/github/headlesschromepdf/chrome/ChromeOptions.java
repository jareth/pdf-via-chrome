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
         * @param port the port number, or 0 for random
         * @return this builder
         */
        public Builder remoteDebuggingPort(int port) {
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
         */
        public Builder addFlag(String flag) {
            this.additionalFlags.add(flag);
            return this;
        }

        /**
         * Adds multiple Chrome command-line flags.
         *
         * @param flags the flags to add
         * @return this builder
         */
        public Builder addFlags(List<String> flags) {
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
         * Sets the timeout in seconds for Chrome startup.
         * Default is 30 seconds.
         *
         * @param timeoutSeconds the timeout in seconds
         * @return this builder
         */
        public Builder startupTimeout(int timeoutSeconds) {
            this.startupTimeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * Sets the timeout in seconds for graceful Chrome shutdown.
         * If the process doesn't terminate within this time, it will be forcibly killed.
         * Default is 5 seconds.
         *
         * @param timeoutSeconds the timeout in seconds
         * @return this builder
         */
        public Builder shutdownTimeout(int timeoutSeconds) {
            this.shutdownTimeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * Builds the ChromeOptions instance.
         *
         * @return a new ChromeOptions instance
         */
        public ChromeOptions build() {
            return new ChromeOptions(this);
        }
    }
}
