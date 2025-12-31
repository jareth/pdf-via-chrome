package com.fostermoore.pdfviachrome;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import com.fostermoore.pdfviachrome.converter.HtmlToPdfConverter;
import com.fostermoore.pdfviachrome.converter.UrlToPdfConverter;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for CSS injection functionality.
 * <p>
 * These tests verify that custom CSS can be injected into pages before PDF generation
 * for both HTML content and URL sources.
 * </p>
 */
class CssInjectionTest {

    @TempDir
    Path tempDir;

    private PdfGenerator generator;

    @BeforeEach
    void setUp() {
        generator = PdfGenerator.create()
            .withHeadless(true)
            .build();
    }

    @AfterEach
    void tearDown() {
        if (generator != null) {
            generator.close();
        }
    }

    @Test
    void testCssInjectionWithHtml() {
        // Given: HTML with a visible element
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .visible { display: block; color: blue; }
                    .hidden { display: none; }
                </style>
            </head>
            <body>
                <h1>Test Document</h1>
                <div class="visible" id="test-div">This should be hidden by injected CSS</div>
            </body>
            </html>
            """;

        // CSS to hide the element
        String customCss = "#test-div { display: none !important; }";

        // When: Generate PDF with custom CSS
        byte[] pdfData = generator.fromHtml(html)
            .withCustomCss(customCss)
            .generate();

        // Then: PDF should be generated successfully
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);
        assertThat(pdfData).startsWith("%PDF-".getBytes());
    }

    @Test
    void testCssInjectionFromFile() throws IOException {
        // Given: HTML with elements
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial; }
                </style>
            </head>
            <body>
                <h1 class="title">Test Title</h1>
                <p class="content">Test content</p>
            </body>
            </html>
            """;

        // Create CSS file
        Path cssFile = tempDir.resolve("custom.css");
        String cssContent = """
            .title {
                color: red !important;
                font-size: 24px !important;
            }
            .content {
                color: green !important;
            }
            """;
        Files.writeString(cssFile, cssContent);

        // When: Generate PDF with CSS from file
        byte[] pdfData = generator.fromHtml(html)
            .withCustomCssFromFile(cssFile)
            .generate();

        // Then: PDF should be generated successfully
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);
        assertThat(pdfData).startsWith("%PDF-".getBytes());
    }

    @Test
    void testCssInjectionWithComplexCss() {
        // Given: HTML with multiple elements
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .header { background: blue; padding: 10px; }
                    .footer { background: gray; padding: 5px; }
                </style>
            </head>
            <body>
                <div class="header">Header</div>
                <div class="content">Main Content</div>
                <div class="footer">Footer</div>
            </body>
            </html>
            """;

        // Complex CSS with media queries and multiple selectors
        String customCss = """
            @media print {
                .header { display: none; }
                .footer { display: none; }
            }
            .content {
                font-size: 16px;
                line-height: 1.5;
                margin: 20px;
            }
            """;

        // When: Generate PDF with complex CSS
        byte[] pdfData = generator.fromHtml(html)
            .withCustomCss(customCss)
            .generate();

        // Then: PDF should be generated successfully
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);
        assertThat(pdfData).startsWith("%PDF-".getBytes());
    }

    @Test
    void testCssInjectionWithSpecialCharacters() {
        // Given: HTML content
        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="test">Test Content</div>
            </body>
            </html>
            """;

        // CSS with special characters (quotes, newlines, etc.)
        String customCss = """
            .test {
                content: 'Special "quoted" text';
                background: url('data:image/svg+xml;utf8,<svg></svg>');
            }
            /* Comment with special chars: @#$%^&*() */
            """;

        // When: Generate PDF with CSS containing special characters
        byte[] pdfData = generator.fromHtml(html)
            .withCustomCss(customCss)
            .generate();

        // Then: PDF should be generated successfully
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);
        assertThat(pdfData).startsWith("%PDF-".getBytes());
    }

    @Test
    void testCssInjectionWithNullCss() {
        // Given: HTML content
        String html = "<html><body><h1>Test</h1></body></html>";

        // When/Then: Null CSS should throw IllegalArgumentException
        assertThatThrownBy(() ->
            generator.fromHtml(html)
                .withCustomCss(null)
                .generate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CSS cannot be null or empty");
    }

    @Test
    void testCssInjectionWithEmptyCss() {
        // Given: HTML content
        String html = "<html><body><h1>Test</h1></body></html>";

        // When/Then: Empty CSS should throw IllegalArgumentException
        assertThatThrownBy(() ->
            generator.fromHtml(html)
                .withCustomCss("   ")
                .generate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CSS cannot be null or empty");
    }

    @Test
    void testCssInjectionFromNonExistentFile() {
        // Given: HTML content and non-existent file
        String html = "<html><body><h1>Test</h1></body></html>";
        Path nonExistentFile = tempDir.resolve("does-not-exist.css");

        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() ->
            generator.fromHtml(html)
                .withCustomCssFromFile(nonExistentFile)
                .generate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CSS file does not exist");
    }

    @Test
    void testCssInjectionFromDirectory() throws IOException {
        // Given: HTML content and a directory instead of a file
        String html = "<html><body><h1>Test</h1></body></html>";
        Path directory = tempDir.resolve("css-dir");
        Files.createDirectory(directory);

        // When/Then: Should throw IllegalArgumentException
        assertThatThrownBy(() ->
            generator.fromHtml(html)
                .withCustomCssFromFile(directory)
                .generate()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CSS file path is not a regular file");
    }

    @Test
    void testCssInjectionWithPdfOptions() {
        // Given: HTML content
        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <h1>Test Title</h1>
                <p>Test paragraph</p>
            </body>
            </html>
            """;

        String customCss = "body { background: white; color: black; }";

        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .printBackground(true)
            .build();

        // When: Generate PDF with both custom CSS and PDF options
        byte[] pdfData = generator.fromHtml(html)
            .withCustomCss(customCss)
            .withOptions(options)
            .generate();

        // Then: PDF should be generated successfully
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);
        assertThat(pdfData).startsWith("%PDF-".getBytes());
    }

    @Test
    void testCssInjectionWithMultipleCalls() throws InterruptedException {
        // Given: HTML content
        String html = "<html><body><h1>Test</h1></body></html>";
        String css1 = "h1 { color: red; }";
        String css2 = "h1 { color: blue; }";

        // Add delay before generation to ensure Chrome/CDP is fully ready (prevents flaky failures under load)
        Thread.sleep(200);

        // When: Call withCustomCss multiple times (last call should win)
        byte[] pdfData = generator.fromHtml(html)
            .withCustomCss(css1)
            .withCustomCss(css2)  // This should override css1
            .generate();

        // Then: PDF should be generated successfully
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);
        assertThat(pdfData).startsWith("%PDF-".getBytes());
    }

    @Test
    void testHtmlToPdfConverterWithCss() throws Exception {
        // Given: Chrome manager and CDP session
        ChromeOptions chromeOptions = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);

                String html = """
                    <!DOCTYPE html>
                    <html>
                    <body>
                        <div class="test">Test Content</div>
                    </body>
                    </html>
                    """;

                String customCss = ".test { color: red; font-size: 20px; }";

                PdfOptions options = PdfOptions.defaults();

                // When: Convert with custom CSS
                byte[] pdfData = converter.convert(html, options, customCss);

                // Then: PDF should be generated successfully
                assertThat(pdfData).isNotNull();
                assertThat(pdfData.length).isGreaterThan(0);
                assertThat(pdfData).startsWith("%PDF-".getBytes());
            }
        }
    }

    @Test
    void testUrlToPdfConverterWithCss() throws Exception {
        // Given: Chrome manager and CDP session
        ChromeOptions chromeOptions = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);

                // Using data URL to avoid external dependencies
                String dataUrl = "data:text/html;charset=utf-8," +
                    "<html><body><div class='test'>Test</div></body></html>";

                String customCss = ".test { background: yellow; }";

                PdfOptions options = PdfOptions.defaults();

                // When: Convert with custom CSS
                byte[] pdfData = converter.convert(dataUrl, options, customCss);

                // Then: PDF should be generated successfully
                assertThat(pdfData).isNotNull();
                assertThat(pdfData.length).isGreaterThan(0);
                assertThat(pdfData).startsWith("%PDF-".getBytes());
            }
        }
    }

    @Test
    void testCssInjectionPersistsAcrossMultipleGenerations() {
        // Given: HTML content
        String html1 = "<html><body><h1>Document 1</h1></body></html>";
        String html2 = "<html><body><h1>Document 2</h1></body></html>";
        String css1 = "h1 { color: red; }";
        String css2 = "h1 { color: blue; }";

        // When: Generate multiple PDFs with different CSS
        byte[] pdf1 = generator.fromHtml(html1)
            .withCustomCss(css1)
            .generate();

        byte[] pdf2 = generator.fromHtml(html2)
            .withCustomCss(css2)
            .generate();

        // Then: Both PDFs should be generated successfully
        assertThat(pdf1).isNotNull().hasSizeGreaterThan(0);
        assertThat(pdf2).isNotNull().hasSizeGreaterThan(0);
        assertThat(pdf1).startsWith("%PDF-".getBytes());
        assertThat(pdf2).startsWith("%PDF-".getBytes());
    }

    @Test
    void testCssInjectionWithoutCss() {
        // Given: HTML content
        String html = "<html><body><h1>Test</h1></body></html>";

        // When: Generate PDF without custom CSS
        byte[] pdfData = generator.fromHtml(html)
            .generate();

        // Then: PDF should be generated successfully
        assertThat(pdfData).isNotNull();
        assertThat(pdfData.length).isGreaterThan(0);
        assertThat(pdfData).startsWith("%PDF-".getBytes());
    }
}
