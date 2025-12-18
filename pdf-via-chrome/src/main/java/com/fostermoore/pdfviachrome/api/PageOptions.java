package com.fostermoore.pdfviachrome.api;

import java.time.Duration;

/**
 * Configuration options for page-specific settings before PDF generation.
 *
 * This class provides a builder-style API for configuring page settings such as
 * viewport size, user agent, page load timeout, and JavaScript enablement. These options
 * are applied before navigation using the Chrome DevTools Protocol (CDP):
 * - Emulation.setDeviceMetricsOverride (viewport)
 * - Emulation.setUserAgentOverride (user agent)
 * - Runtime.enable/disable (JavaScript)
 */
public class PageOptions {

    private final int viewportWidth;
    private final int viewportHeight;
    private final String userAgent;
    private final Duration pageLoadTimeout;
    private final boolean javascriptEnabled;
    private final double deviceScaleFactor;

    private PageOptions(Builder builder) {
        this.viewportWidth = builder.viewportWidth;
        this.viewportHeight = builder.viewportHeight;
        this.userAgent = builder.userAgent;
        this.pageLoadTimeout = builder.pageLoadTimeout;
        this.javascriptEnabled = builder.javascriptEnabled;
        this.deviceScaleFactor = builder.deviceScaleFactor;
    }

    /**
     * Returns the viewport width in pixels.
     *
     * @return the viewport width in pixels
     */
    public int getViewportWidth() {
        return viewportWidth;
    }

    /**
     * Returns the viewport height in pixels.
     *
     * @return the viewport height in pixels
     */
    public int getViewportHeight() {
        return viewportHeight;
    }

    /**
     * Returns the custom user agent string.
     *
     * @return the user agent string, or null if using Chrome's default
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Returns the page load timeout.
     *
     * @return the page load timeout duration
     */
    public Duration getPageLoadTimeout() {
        return pageLoadTimeout;
    }

    /**
     * Returns whether JavaScript is enabled for the page.
     *
     * @return true if JavaScript is enabled, false otherwise
     */
    public boolean isJavascriptEnabled() {
        return javascriptEnabled;
    }

    /**
     * Returns the device scale factor (pixel ratio).
     *
     * @return the device scale factor
     */
    public double getDeviceScaleFactor() {
        return deviceScaleFactor;
    }

    /**
     * Creates a new builder for PageOptions.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates PageOptions with default settings.
     *
     * @return PageOptions with default values
     */
    public static PageOptions defaults() {
        return new Builder().build();
    }

    /**
     * Builder class for constructing PageOptions instances.
     */
    public static class Builder {
        private int viewportWidth = 1920;
        private int viewportHeight = 1080;
        private String userAgent = null;
        private Duration pageLoadTimeout = Duration.ofSeconds(30);
        private boolean javascriptEnabled = true;
        private double deviceScaleFactor = 1.0;

        /**
         * Sets the viewport width in pixels.
         *
         * @param width the viewport width in pixels (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if width is not positive
         */
        public Builder viewportWidth(int width) {
            if (width <= 0) {
                throw new IllegalArgumentException("Viewport width must be positive");
            }
            this.viewportWidth = width;
            return this;
        }

        /**
         * Sets the viewport height in pixels.
         *
         * @param height the viewport height in pixels (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if height is not positive
         */
        public Builder viewportHeight(int height) {
            if (height <= 0) {
                throw new IllegalArgumentException("Viewport height must be positive");
            }
            this.viewportHeight = height;
            return this;
        }

        /**
         * Sets the viewport dimensions (width and height) in pixels.
         *
         * @param width the viewport width in pixels (must be positive)
         * @param height the viewport height in pixels (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if width or height is not positive
         */
        public Builder viewport(int width, int height) {
            return viewportWidth(width).viewportHeight(height);
        }

        /**
         * Sets a custom user agent string.
         *
         * @param userAgent the custom user agent string (null uses Chrome's default)
         * @return this builder
         */
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Sets the page load timeout.
         *
         * @param timeout the page load timeout (must not be null or negative)
         * @return this builder
         * @throws IllegalArgumentException if timeout is null or negative
         */
        public Builder pageLoadTimeout(Duration timeout) {
            if (timeout == null) {
                throw new IllegalArgumentException("Page load timeout cannot be null");
            }
            if (timeout.isNegative()) {
                throw new IllegalArgumentException("Page load timeout cannot be negative");
            }
            this.pageLoadTimeout = timeout;
            return this;
        }

        /**
         * Sets whether JavaScript is enabled for the page.
         *
         * @param enabled true to enable JavaScript, false to disable
         * @return this builder
         */
        public Builder javascriptEnabled(boolean enabled) {
            this.javascriptEnabled = enabled;
            return this;
        }

        /**
         * Sets the device scale factor (pixel ratio).
         * This affects the rendering of high-DPI displays.
         *
         * @param scaleFactor the device scale factor (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if scaleFactor is not positive
         */
        public Builder deviceScaleFactor(double scaleFactor) {
            if (scaleFactor <= 0) {
                throw new IllegalArgumentException("Device scale factor must be positive");
            }
            this.deviceScaleFactor = scaleFactor;
            return this;
        }

        /**
         * Builds the PageOptions instance.
         *
         * @return a new PageOptions instance
         */
        public PageOptions build() {
            return new PageOptions(this);
        }
    }
}
