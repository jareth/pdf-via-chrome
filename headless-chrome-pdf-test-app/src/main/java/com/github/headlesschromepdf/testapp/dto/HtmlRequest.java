package com.github.headlesschromepdf.testapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for HTML-to-PDF conversion endpoint.
 */
public class HtmlRequest {

    /**
     * HTML content to convert to PDF.
     */
    @NotBlank(message = "HTML content is required")
    private String content;

    /**
     * Optional PDF generation options.
     */
    private PdfOptionsDto options;

    /**
     * Optional wait strategy (for future use).
     */
    private String waitStrategy;

    public HtmlRequest() {
    }

    public HtmlRequest(String content, PdfOptionsDto options) {
        this.content = content;
        this.options = options;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PdfOptionsDto getOptions() {
        return options;
    }

    public void setOptions(PdfOptionsDto options) {
        this.options = options;
    }

    public String getWaitStrategy() {
        return waitStrategy;
    }

    public void setWaitStrategy(String waitStrategy) {
        this.waitStrategy = waitStrategy;
    }
}
