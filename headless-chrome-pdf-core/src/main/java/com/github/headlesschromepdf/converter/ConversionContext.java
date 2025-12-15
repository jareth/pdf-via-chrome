package com.github.headlesschromepdf.converter;

import com.github.headlesschromepdf.api.PdfOptions;
import com.github.headlesschromepdf.cdp.CdpSession;

/**
 * Context object that holds the state and configuration for a PDF conversion operation.
 * <p>
 * This class encapsulates all the information needed for a single conversion operation,
 * including the CDP session, PDF options, timeout settings, and conversion metadata.
 * It serves as a container to pass conversion context between different components
 * and stages of the conversion process.
 * </p>
 */
public class ConversionContext {

    private final CdpSession session;
    private final PdfOptions options;
    private final int loadTimeoutMs;
    private final String sourceIdentifier;
    private long startTimeMs;
    private long endTimeMs;

    private ConversionContext(Builder builder) {
        this.session = builder.session;
        this.options = builder.options;
        this.loadTimeoutMs = builder.loadTimeoutMs;
        this.sourceIdentifier = builder.sourceIdentifier;
    }

    /**
     * Gets the CDP session for this conversion.
     *
     * @return the CDP session
     */
    public CdpSession getSession() {
        return session;
    }

    /**
     * Gets the PDF options for this conversion.
     *
     * @return the PDF options
     */
    public PdfOptions getOptions() {
        return options;
    }

    /**
     * Gets the load timeout in milliseconds.
     *
     * @return the load timeout in milliseconds
     */
    public int getLoadTimeoutMs() {
        return loadTimeoutMs;
    }

    /**
     * Gets the source identifier (e.g., URL or "HTML content").
     *
     * @return the source identifier
     */
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * Gets the start time of the conversion in milliseconds since epoch.
     *
     * @return the start time in milliseconds, or 0 if not started
     */
    public long getStartTimeMs() {
        return startTimeMs;
    }

    /**
     * Sets the start time of the conversion.
     *
     * @param startTimeMs the start time in milliseconds since epoch
     */
    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    /**
     * Gets the end time of the conversion in milliseconds since epoch.
     *
     * @return the end time in milliseconds, or 0 if not completed
     */
    public long getEndTimeMs() {
        return endTimeMs;
    }

    /**
     * Sets the end time of the conversion.
     *
     * @param endTimeMs the end time in milliseconds since epoch
     */
    public void setEndTimeMs(long endTimeMs) {
        this.endTimeMs = endTimeMs;
    }

    /**
     * Gets the duration of the conversion in milliseconds.
     *
     * @return the duration in milliseconds, or 0 if not completed
     */
    public long getDurationMs() {
        if (startTimeMs == 0 || endTimeMs == 0) {
            return 0;
        }
        return endTimeMs - startTimeMs;
    }

    /**
     * Marks the conversion as started.
     */
    public void markStarted() {
        this.startTimeMs = System.currentTimeMillis();
    }

    /**
     * Marks the conversion as completed.
     */
    public void markCompleted() {
        this.endTimeMs = System.currentTimeMillis();
    }

    /**
     * Creates a new builder for ConversionContext.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing ConversionContext instances.
     */
    public static class Builder {
        private CdpSession session;
        private PdfOptions options = PdfOptions.defaults();
        private int loadTimeoutMs = 30000;
        private String sourceIdentifier = "HTML content";

        /**
         * Sets the CDP session.
         *
         * @param session the CDP session
         * @return this builder
         */
        public Builder session(CdpSession session) {
            this.session = session;
            return this;
        }

        /**
         * Sets the PDF options.
         *
         * @param options the PDF options
         * @return this builder
         */
        public Builder options(PdfOptions options) {
            this.options = options != null ? options : PdfOptions.defaults();
            return this;
        }

        /**
         * Sets the load timeout in milliseconds.
         *
         * @param loadTimeoutMs the load timeout in milliseconds
         * @return this builder
         * @throws IllegalArgumentException if timeout is not positive
         */
        public Builder loadTimeoutMs(int loadTimeoutMs) {
            if (loadTimeoutMs <= 0) {
                throw new IllegalArgumentException("Load timeout must be positive");
            }
            this.loadTimeoutMs = loadTimeoutMs;
            return this;
        }

        /**
         * Sets the source identifier (e.g., URL or description).
         *
         * @param sourceIdentifier the source identifier
         * @return this builder
         */
        public Builder sourceIdentifier(String sourceIdentifier) {
            this.sourceIdentifier = sourceIdentifier != null ? sourceIdentifier : "HTML content";
            return this;
        }

        /**
         * Builds the ConversionContext instance.
         *
         * @return a new ConversionContext instance
         * @throws IllegalArgumentException if session is null
         */
        public ConversionContext build() {
            if (session == null) {
                throw new IllegalArgumentException("CdpSession cannot be null");
            }
            return new ConversionContext(this);
        }
    }
}
