package com.github.headlesschromepdf;

import com.github.headlesschromepdf.api.PdfOptions;
import com.github.headlesschromepdf.cdp.CdpSession;
import com.github.headlesschromepdf.converter.HtmlToPdfConverter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the HtmlToPdfConverter class using Testcontainers.
 *
 * These tests validate the low-level HTML-to-PDF conversion using
 * CdpSession and HtmlToPdfConverter with Chrome running in a Docker container.
 *
 * The tests use Testcontainers to spin up an isolated Chrome instance in Docker,
 * ensuring consistent behavior regardless of the host environment.
 *
 * Note: These tests require Docker to be running. They will be skipped if Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class HtmlToPdfIT {

    private static final Logger logger = LoggerFactory.getLogger(HtmlToPdfIT.class);

    /**
     * Chrome container with remote debugging enabled.
     * Using selenium/standalone-chrome as base image with custom command to enable CDP access.
     */
    @Container
    static GenericContainer<?> chromeContainer = new GenericContainer<>(
            DockerImageName.parse("selenium/standalone-chrome:latest"))
            .withExposedPorts(9222)
            .withCommand(
                    "/opt/google/chrome/google-chrome",
                    "--headless",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--remote-debugging-address=0.0.0.0",
                    "--remote-debugging-port=9222",
                    "--disable-web-security",
                    "--user-data-dir=/tmp/chrome-user-data",
                    "about:blank"
            )
            .withStartupTimeout(Duration.ofMinutes(2))
            .waitingFor(Wait.forListeningPort());

    private CdpSession cdpSession;
    private HtmlToPdfConverter converter;

    @BeforeEach
    void setUp() throws IOException {
        logger.info("Setting up CDP session with containerized Chrome");

        // Get the WebSocket debugger URL from the containerized Chrome
        String host = chromeContainer.getHost();
        int port = chromeContainer.getMappedPort(9222);
        String webSocketUrl = getWebSocketDebuggerUrl(host, port);

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

    /**
     * Gets the WebSocket debugger URL from Chrome's debugging endpoint.
     */
    private String getWebSocketDebuggerUrl(String host, int port) throws IOException {
        String jsonUrl = String.format("http://%s:%d/json/version", host, port);
        logger.debug("Fetching WebSocket URL from: {}", jsonUrl);

        URL url = new URL(jsonUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // Parse JSON to extract webSocketDebuggerUrl
            // Simple parsing: look for "webSocketDebuggerUrl":"ws://..."
            String json = response.toString();
            int startIndex = json.indexOf("\"webSocketDebuggerUrl\":\"") + 24;
            int endIndex = json.indexOf("\"", startIndex);
            String wsUrl = json.substring(startIndex, endIndex);

            // Replace localhost with actual host
            wsUrl = wsUrl.replace("localhost", host).replace("127.0.0.1", host);

            logger.debug("Extracted WebSocket URL: {}", wsUrl);
            return wsUrl;
        } finally {
            conn.disconnect();
        }
    }

    @Test
    void testConvertSimpleHtml() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>Simple Test</title></head>
                <body>
                    <h1>Test Heading</h1>
                    <p>Test paragraph content.</p>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = converter.convert(html);

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Test Heading");
        assertPdfContainsText(pdfBytes, "Test paragraph content.");
    }

    @Test
    void testConvertWithDefaultOptions() throws IOException {
        // Given
        String html = "<html><body><h1>Default Options Test</h1></body></html>";

        // When
        byte[] pdfBytes = converter.convert(html, PdfOptions.defaults());

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Default Options Test");
    }

    @Test
    void testConvertWithCustomOptions() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Custom Options Test</h1>
                    <div style="background-color: yellow; padding: 10px;">
                        This has a yellow background.
                    </div>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .landscape(true)
                .printBackground(true)
                .scale(0.9)
                .margins("0.5in")
                .build();

        // When
        byte[] pdfBytes = converter.convert(html, options);

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Custom Options Test");

        // Verify landscape orientation
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDPage page = document.getPage(0);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            assertThat(width).isGreaterThan(height);
        }
    }

    @Test
    void testConvertWithA4PaperSize() throws IOException {
        // Given
        String html = "<html><body><h1>A4 Test</h1></body></html>";

        PdfOptions options = PdfOptions.builder()
                .paperSize(PdfOptions.PaperFormat.A4)
                .build();

        // When
        byte[] pdfBytes = converter.convert(html, options);

        // Then
        assertValidPdf(pdfBytes);

        // Verify A4 dimensions (595pt x 842pt)
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDPage page = document.getPage(0);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();

            assertThat(width).isBetween(594f, 596f);
            assertThat(height).isBetween(841f, 843f);
        }
    }

    @Test
    void testConvertWithLegalPaperSize() throws IOException {
        // Given
        String html = "<html><body><h1>Legal Test</h1></body></html>";

        PdfOptions options = PdfOptions.builder()
                .paperSize(PdfOptions.PaperFormat.LEGAL)
                .build();

        // When
        byte[] pdfBytes = converter.convert(html, options);

        // Then
        assertValidPdf(pdfBytes);

        // Verify Legal dimensions (8.5in x 14in = 612pt x 1008pt)
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDPage page = document.getPage(0);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();

            assertThat(width).isBetween(611f, 613f);
            assertThat(height).isBetween(1007f, 1009f);
        }
    }

    @Test
    void testConvertHtmlWithTable() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        table { border-collapse: collapse; width: 100%; }
                        th, td { border: 1px solid black; padding: 8px; }
                        th { background-color: #f2f2f2; }
                    </style>
                </head>
                <body>
                    <h1>Table Test</h1>
                    <table>
                        <tr>
                            <th>Header 1</th>
                            <th>Header 2</th>
                        </tr>
                        <tr>
                            <td>Row 1 Col 1</td>
                            <td>Row 1 Col 2</td>
                        </tr>
                        <tr>
                            <td>Row 2 Col 1</td>
                            <td>Row 2 Col 2</td>
                        </tr>
                    </table>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .printBackground(true)
                .build();

        // When
        byte[] pdfBytes = converter.convert(html, options);

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Table Test");
        assertPdfContainsText(pdfBytes, "Header 1");
        assertPdfContainsText(pdfBytes, "Row 1 Col 1");
    }

    @Test
    void testConvertHtmlWithSpecialCharacters() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h1>Special Characters Test</h1>
                    <p>English: Hello</p>
                    <p>Symbols: © ® ™ € £ ¥</p>
                    <p>Math: ∑ ∫ √ ≈ ≠</p>
                    <p>Quotes: "Hello" 'World'</p>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = converter.convert(html);

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Special Characters Test");
        assertPdfContainsText(pdfBytes, "Hello");
    }

    @Test
    void testConvertEmptyBody() throws IOException {
        // Given
        String html = "<!DOCTYPE html><html><body></body></html>";

        // When
        byte[] pdfBytes = converter.convert(html);

        // Then
        assertValidPdf(pdfBytes);

        // Should still produce a valid PDF with at least one page
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void testConvertMinimalHtml() throws IOException {
        // Given
        String html = "<h1>Minimal</h1>";

        // When
        byte[] pdfBytes = converter.convert(html);

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Minimal");
    }

    @Test
    void testConvertMultiPageContent() throws IOException {
        // Given - HTML that should span multiple pages
        StringBuilder htmlBuilder = new StringBuilder("""
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Multi-Page Document</h1>
                """);

        // Add enough content to fill multiple pages
        for (int i = 1; i <= 100; i++) {
            htmlBuilder.append("<p>This is paragraph number ").append(i)
                    .append(". It contains some text to fill the page.</p>");
        }

        htmlBuilder.append("</body></html>");

        // When
        byte[] pdfBytes = converter.convert(htmlBuilder.toString());

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Multi-Page Document");
        assertPdfContainsText(pdfBytes, "paragraph number 1");
        assertPdfContainsText(pdfBytes, "paragraph number 100");

        // Verify multiple pages were created
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void testConvertWithNullHtmlThrowsException() {
        // When/Then
        assertThatThrownBy(() -> converter.convert(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML content cannot be null");
    }

    @Test
    void testConvertWithNullOptionsThrowsException() {
        // Given
        String html = "<html><body>Test</body></html>";

        // When/Then
        assertThatThrownBy(() -> converter.convert(html, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PdfOptions cannot be null");
    }

    @Test
    void testConverterCreationWithNullSessionThrowsException() {
        // When/Then
        assertThatThrownBy(() -> new HtmlToPdfConverter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CdpSession cannot be null");
    }

    @Test
    void testConverterCreationWithInvalidTimeoutThrowsException() {
        // When/Then
        assertThatThrownBy(() -> new HtmlToPdfConverter(cdpSession, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Load timeout must be positive");

        assertThatThrownBy(() -> new HtmlToPdfConverter(cdpSession, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Load timeout must be positive");
    }

    @Test
    void testConvertWithCustomTimeout() throws IOException {
        // Given
        HtmlToPdfConverter customConverter = new HtmlToPdfConverter(cdpSession, 5000);
        String html = "<html><body><h1>Custom Timeout Test</h1></body></html>";

        // When
        byte[] pdfBytes = customConverter.convert(html);

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Custom Timeout Test");
    }

    @Test
    void testSequentialConversions() throws IOException {
        // Given
        String html1 = "<html><body><h1>First Document</h1></body></html>";
        String html2 = "<html><body><h1>Second Document</h1></body></html>";
        String html3 = "<html><body><h1>Third Document</h1></body></html>";

        // When - Convert multiple documents using the same converter
        byte[] pdf1 = converter.convert(html1);
        byte[] pdf2 = converter.convert(html2);
        byte[] pdf3 = converter.convert(html3);

        // Then - All PDFs should be valid and contain correct content
        assertValidPdf(pdf1);
        assertPdfContainsText(pdf1, "First Document");

        assertValidPdf(pdf2);
        assertPdfContainsText(pdf2, "Second Document");

        assertValidPdf(pdf3);
        assertPdfContainsText(pdf3, "Third Document");
    }

    @Test
    void testConvertWithHeadersAndFooters() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Document with Headers and Footers</h1>
                    <p>This document should have custom headers and footers.</p>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .displayHeaderFooter(true)
                .headerTemplate("<div style='font-size:10px; text-align:center; width:100%;'>Page Header</div>")
                .footerTemplate("<div style='font-size:10px; text-align:center; width:100%;'>Page <span class='pageNumber'></span></div>")
                .margins("0.75in")  // Need margins for headers/footers
                .build();

        // When
        byte[] pdfBytes = converter.convert(html, options);

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Document with Headers and Footers");
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
