package com.fostermoore.pdfviachrome.testapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for HTML-to-PDF conversion endpoint.
 */
public class HtmlRequest {

    /**
     * HTML content to convert to PDF.
     * Maximum size is 10MB (approximately 10 million characters).
     */
    @NotBlank(message = "HTML content is required")
    @Size(max = 10_000_000, message = "HTML content cannot exceed 10,000,000 characters")
    private String content;

    /**
     * Optional PDF generation options.
     * Will be validated if provided.
     */
    @Valid
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
