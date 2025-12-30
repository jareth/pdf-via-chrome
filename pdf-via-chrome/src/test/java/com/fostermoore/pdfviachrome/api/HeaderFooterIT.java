package com.fostermoore.pdfviachrome.api;

import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.converter.HtmlToPdfConverter;
import com.fostermoore.pdfviachrome.util.ChromeContainerTestHelper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PDF header and footer functionality using Testcontainers.
 *
 * These tests validate header and footer generation using HtmlToPdfConverter
 * with Chrome running in a Docker container.
 *
 * The tests use Testcontainers to spin up an isolated Chrome instance in Docker,
 * ensuring consistent behavior regardless of the host environment.
 *
 * Note: These tests require Docker to be running. They will be skipped if Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class HeaderFooterIT {

    private static final Logger logger = LoggerFactory.getLogger(HeaderFooterIT.class);

    /**
     * Chrome container with remote debugging enabled.
     * Using zenika/alpine-chrome as base image - it's designed for headless Chrome with CDP access.
     */
    @Container
    static GenericContainer<?> chromeContainer = new GenericContainer<>(
            DockerImageName.parse("zenika/alpine-chrome:latest"))
            .withExposedPorts(9222)
            .withCommand(
                    "--headless",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--remote-debugging-address=0.0.0.0",
                    "--remote-debugging-port=9222",
                    "--remote-allow-origins=*",  // Required for Chrome 98+ to allow WebSocket connections
                    "--disable-web-security",
                    "about:blank"
            )
            .withStartupTimeout(Duration.ofMinutes(2))
            .waitingFor(Wait.forHttp("/json/version").forPort(9222).forStatusCode(200));

    private CdpSession cdpSession;
    private HtmlToPdfConverter converter;

    @BeforeEach
    void setUp() throws IOException {
        logger.info("Setting up CDP session with containerized Chrome");

        // Get the WebSocket debugger URL from the containerized Chrome
        String host = chromeContainer.getHost();
        int port = chromeContainer.getMappedPort(9222);
        String webSocketUrl = ChromeContainerTestHelper.getWebSocketDebuggerUrl(host, port);

        logger.info("Connecting to Chrome at WebSocket URL: {}", webSocketUrl);

        // Connect CDP session to containerized Chrome
        cdpSession = new CdpSession(webSocketUrl);
        cdpSession.connect();

        // Create converter
        converter = new HtmlToPdfConverter(cdpSession);

        logger.info("Test setup complete");
    }

    @AfterEach
    void tearDown() {
        logger.info("Tearing down test resources");

        if (cdpSession != null) {
            try {
                cdpSession.close();
            } catch (Exception e) {
                logger.warn("Error closing CDP session", e);
            }
        }

        logger.info("Test teardown complete");
    }

    @Test
    void testSimplePageNumbers() throws IOException {
        // Given
        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Page Numbers Test</title></head>
            <body>
                <h1>Page 1</h1>
                <div style="page-break-after: always;"></div>
                <h1>Page 2</h1>
                <div style="page-break-after: always;"></div>
                <h1>Page 3</h1>
            </body>
            </html>
            """;

        PdfOptions options = PdfOptions.builder()
            .simplePageNumbers()
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Page 1");
        assertPdfContainsText(pdf, "Page 2");
        assertPdfContainsText(pdf, "Page 3");

        // Verify displayHeaderFooter was enabled
        assertThat(options.isDisplayHeaderFooter()).isTrue();
        assertThat(options.getFooterTemplate()).contains("pageNumber");
    }

    @Test
    void testHeaderWithTitle() throws IOException {
        // Given
        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Test Document Title</title></head>
            <body>
                <h1>Header Test</h1>
                <p>This document should have a header with the title.</p>
            </body>
            </html>
            """;

        PdfOptions options = PdfOptions.builder()
            .headerWithTitle()
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Header Test");

        assertThat(options.isDisplayHeaderFooter()).isTrue();
        assertThat(options.getHeaderTemplate()).contains("title");
    }

    @Test
    void testFooterWithDate() throws IOException {
        // Given
        String html = "<html><body><h1>Footer with Date Test</h1></body></html>";

        PdfOptions options = PdfOptions.builder()
            .footerWithDate()
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Footer with Date Test");

        assertThat(options.isDisplayHeaderFooter()).isTrue();
        assertThat(options.getFooterTemplate()).contains("date");
    }

    @Test
    void testStandardHeaderFooter() throws IOException {
        // Given
        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Standard Header/Footer Document</title></head>
            <body>
                <h1>Page 1 Content</h1>
                <p>This is some content on the first page.</p>
                <div style="page-break-after: always;"></div>
                <h1>Page 2 Content</h1>
                <p>This is some content on the second page.</p>
            </body>
            </html>
            """;

        PdfOptions options = PdfOptions.builder()
            .standardHeaderFooter()
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Page 1 Content");
        assertPdfContainsText(pdf, "Page 2 Content");

        assertThat(options.isDisplayHeaderFooter()).isTrue();
        assertThat(options.getHeaderTemplate()).contains("title");
        assertThat(options.getFooterTemplate()).contains("pageNumber");
    }

    @Test
    void testCustomHeaderTemplate() throws IOException {
        // Given
        String html = "<html><body><h1>Custom Header Test</h1></body></html>";

        String customHeader = """
            <div style="font-size: 12px; text-align: right; width: 100%; padding-right: 1cm;">
                <span class="title"></span> - Page <span class="pageNumber"></span>
            </div>
            """;

        PdfOptions options = PdfOptions.builder()
            .displayHeaderFooter(true)
            .headerTemplate(customHeader)
            .margins("0.75in")  // Need margins for headers
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Custom Header Test");
    }

    @Test
    void testCustomFooterTemplate() throws IOException {
        // Given
        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Custom Footer Test</title></head>
            <body>
                <h1>Page 1</h1>
                <div style="page-break-after: always;"></div>
                <h1>Page 2</h1>
            </body>
            </html>
            """;

        String customFooter = """
            <div style="font-size: 10px; width: 100%; display: flex; justify-content: space-between; padding: 0 1cm;">
                <span><span class="date"></span></span>
                <span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span>
                <span><span class="url"></span></span>
            </div>
            """;

        PdfOptions options = PdfOptions.builder()
            .displayHeaderFooter(true)
            .footerTemplate(customFooter)
            .margins("0.75in")  // Need margins for footers
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Page 1");
        assertPdfContainsText(pdf, "Page 2");
    }

    @Test
    void testHeaderAndFooterTogether() throws IOException {
        // Given
        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Combined Header and Footer Test</title></head>
            <body>
                <h1>Testing Both Header and Footer</h1>
                <p>This document has both a custom header and footer.</p>
                <div style="page-break-after: always;"></div>
                <h1>Second Page</h1>
                <p>More content here.</p>
            </body>
            </html>
            """;

        String header = "<div style=\"font-size: 10px; text-align: center; width: 100%;\"><span class=\"title\"></span></div>";
        String footer = "<div style=\"font-size: 10px; text-align: center; width: 100%;\">Page <span class=\"pageNumber\"></span></div>";

        PdfOptions options = PdfOptions.builder()
            .displayHeaderFooter(true)
            .headerTemplate(header)
            .footerTemplate(footer)
            .printBackground(true)
            .margins("0.75in")  // Need margins for headers/footers
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Testing Both Header and Footer");
        assertPdfContainsText(pdf, "Second Page");
    }

    @Test
    void testConvenienceMethodOverriddenByCustomTemplate() throws IOException {
        // Given
        String html = "<html><body><h1>Override Test</h1></body></html>";

        // First use convenience method, then override with custom template
        PdfOptions options = PdfOptions.builder()
            .simplePageNumbers()
            .footerTemplate("<div style=\"text-align: center; width: 100%;\">Custom Footer</div>")
            .margins("0.75in")  // Need margins for footers
            .build();

        // When
        byte[] pdf = converter.convert(html, options);

        // Then
        assertValidPdf(pdf);
        assertPdfContainsText(pdf, "Override Test");

        // Verify custom template was used
        assertThat(options.getFooterTemplate()).contains("Custom Footer");
        assertThat(options.getFooterTemplate()).doesNotContain("pageNumber");
    }

    // ========== Helper Methods ==========

    /**
     * Validates that the byte array is a valid PDF document.
     */
    private void assertValidPdf(byte[] pdfBytes) throws IOException {
        assertThat(pdfBytes).isNotEmpty();

        // Check PDF signature
        assertThat(pdfBytes.length).isGreaterThan(4);
        String header = new String(pdfBytes, 0, 4);
        assertThat(header).isEqualTo("%PDF");

        // Verify it can be loaded with PDFBox
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(0);
        }
    }

    /**
     * Validates that the PDF contains the specified text.
     */
    private void assertPdfContainsText(byte[] pdfBytes, String expectedText) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            assertThat(text).contains(expectedText);
        }
    }
}
