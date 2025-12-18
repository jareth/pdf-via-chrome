package com.fostermoore.pdfviachrome.testapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for testing and demonstrating the
 * headless-chrome-pdf library.
 *
 * <p>This application provides a web interface for manual testing
 * of PDF generation features including:
 * <ul>
 *   <li>HTML to PDF conversion</li>
 *   <li>URL to PDF conversion</li>
 *   <li>Custom PDF options (margins, paper size, orientation)</li>
 *   <li>Page-specific settings (viewport, user agent)</li>
 * </ul>
 */
@SpringBootApplication
public class PdfTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfTestApplication.class, args);
    }
}
