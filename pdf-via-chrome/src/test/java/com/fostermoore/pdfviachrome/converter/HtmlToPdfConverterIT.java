package com.fostermoore.pdfviachrome.converter;

import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpClient;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.fostermoore.pdfviachrome.util.ChromePathDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for HtmlToPdfConverter that use actual Chrome instances.
 * <p>
 * These tests require Chrome to be installed on the system.
 * They are disabled by default and can be enabled by setting the
 * CHROME_INTEGRATION_TESTS system property to "true".
 * </p>
 * <p>
 * To run: mvn verify -DCHROME_INTEGRATION_TESTS=true
 * </p>
 */
@EnabledIfSystemProperty(named = "CHROME_INTEGRATION_TESTS", matches = "true")
class HtmlToPdfConverterIT {

    private static final String SIMPLE_HTML = "<html><body><h1>Hello World</h1></body></html>";

    private static final String HTML_WITH_STYLES = """
        <html>
        <head>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    margin: 20px;
                    background-color: #f0f0f0;
                }
                h1 {
                    color: #333;
                }
                .box {
                    background-color: #4CAF50;
                    color: white;
                    padding: 20px;
                    margin: 10px 0;
                }
            </style>
        </head>
        <body>
            <h1>Styled Content</h1>
            <div class="box">This is a styled box</div>
            <p>This is a paragraph with some text.</p>
        </body>
        </html>
        """;

    private static final String HTML_WITH_MULTIPLE_PAGES = """
        <html>
        <head>
            <style>
                .page { page-break-after: always; height: 800px; }
                .page:last-child { page-break-after: auto; }
            </style>
        </head>
        <body>
            <div class="page"><h1>Page 1</h1><p>Content for page 1</p></div>
            <div class="page"><h1>Page 2</h1><p>Content for page 2</p></div>
            <div class="page"><h1>Page 3</h1><p>Content for page 3</p></div>
        </body>
        </html>
        """;

    private static final String MALFORMED_HTML = "<html><body><h1>Unclosed tag<p>Missing closing tags";

    @Test
    void testConvertSimpleHtmlToPdf() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent()
            .withFailMessage("Chrome not found. Please install Chrome to run integration tests.");

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                byte[] pdfData = converter.convert(SIMPLE_HTML, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(pdfData.length).isGreaterThan(100);

                // Verify PDF magic number
                String header = new String(pdfData, 0, 5, StandardCharsets.US_ASCII);
                assertThat(header).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertHtmlWithStylesToPdf() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.builder()
                    .printBackground(true) // Include background colors
                    .build();

                byte[] pdfData = converter.convert(HTML_WITH_STYLES, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertWithCustomPdfOptions() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.builder()
                    .landscape(true)
                    .paperSize(PdfOptions.PaperFormat.A4)
                    .margins("1cm")
                    .scale(0.9)
                    .printBackground(true)
                    .build();

                byte[] pdfData = converter.convert(SIMPLE_HTML, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertMultipleHtmlDocuments() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // Convert first document
                byte[] pdf1 = converter.convert("<html><body><h1>Document 1</h1></body></html>", pdfOptions);
                assertThat(pdf1).isNotNull().isNotEmpty();

                // Convert second document
                byte[] pdf2 = converter.convert("<html><body><h1>Document 2</h1></body></html>", pdfOptions);
                assertThat(pdf2).isNotNull().isNotEmpty();

                // Convert third document
                byte[] pdf3 = converter.convert("<html><body><h1>Document 3</h1></body></html>", pdfOptions);
                assertThat(pdf3).isNotNull().isNotEmpty();

                // All PDFs should be valid
                assertThat(new String(pdf1, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
                assertThat(new String(pdf2, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
                assertThat(new String(pdf3, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertMalformedHtml() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // Chrome should handle malformed HTML gracefully
                byte[] pdfData = converter.convert(MALFORMED_HTML, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertEmptyHtml() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                byte[] pdfData = converter.convert("", pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertLargeHtmlDocument() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                // Generate large HTML document
                StringBuilder largeHtml = new StringBuilder("<html><body>");
                for (int i = 0; i < 1000; i++) {
                    largeHtml.append("<h2>Section ").append(i).append("</h2>");
                    largeHtml.append("<p>This is paragraph ").append(i).append(" with some content.</p>");
                }
                largeHtml.append("</body></html>");

                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                byte[] pdfData = converter.convert(largeHtml.toString(), pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertWithHeaderAndFooter() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.builder()
                    .displayHeaderFooter(true)
                    .headerTemplate("<div style='font-size:10px; text-align:center; width:100%;'>Header</div>")
                    .footerTemplate("<div style='font-size:10px; text-align:center; width:100%;'>Page <span class='pageNumber'></span> of <span class='totalPages'></span></div>")
                    .marginTop("1cm")
                    .marginBottom("1cm")
                    .build();

                byte[] pdfData = converter.convert(HTML_WITH_MULTIPLE_PAGES, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertWithDisconnectedSession() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            CdpSession session = CdpClient.createSession(process);
            HtmlToPdfConverter converter = new HtmlToPdfConverter(session);

            // Close the session
            session.close();

            // Attempting to convert should fail
            PdfOptions pdfOptions = PdfOptions.defaults();
            assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, pdfOptions))
                .isInstanceOf(PdfGenerationException.class)
                .hasMessageContaining("not connected");
        }
    }

    @Test
    void testConvertWithShortTimeout() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                // Use a very short timeout (1ms) - this might fail or succeed depending on timing
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session, 1);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // This may throw a timeout exception, but if it succeeds, the PDF should be valid
                try {
                    byte[] pdfData = converter.convert(SIMPLE_HTML, pdfOptions);
                    assertThat(pdfData).isNotNull();
                    assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
                } catch (PdfGenerationException e) {
                    // Timeout is acceptable
                    assertThat(e.getMessage()).containsAnyOf("Timeout", "timeout");
                }
            }
        }
    }

    @Test
    void testConvertWithDefaultMethod() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                HtmlToPdfConverter converter = new HtmlToPdfConverter(session);

                // Test the convenience method that uses default options
                byte[] pdfData = converter.convert(SIMPLE_HTML);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }
}
