package com.fostermoore.pdfviachrome;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import com.fostermoore.pdfviachrome.api.PdfOptions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PDF generation with PdfGenerator API.
 *
 * These tests validate end-to-end PDF generation from HTML content using
 * the high-level PdfGenerator API with local Chrome installation.
 *
 * The @Testcontainers annotation ensures Docker is available. If Docker is not available,
 * these tests will be skipped gracefully.
 *
 * Note: This test suite uses local Chrome installation (not containerized) because
 * the PdfGenerator API manages Chrome lifecycle internally. For tests using containerized
 * Chrome, see HtmlToPdfIT.java.
 */
@Testcontainers(disabledWithoutDocker = true)
class PdfGenerationIT {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationIT.class);

    private PdfGenerator pdfGenerator;

    @BeforeEach
    void setUp() {
        logger.info("Setting up PdfGenerator for integration test");

        // Create PdfGenerator with settings compatible with both local and Docker environments
        pdfGenerator = PdfGenerator.create()
                .withHeadless(true)
                .withNoSandbox(true)  // Required for Docker environments
                .withDisableDevShmUsage(true)  // Required for Docker with limited /dev/shm
                .build();

        logger.info("PdfGenerator setup complete");
    }

    @AfterEach
    void tearDown() {
        if (pdfGenerator != null) {
            pdfGenerator.close();
            logger.info("PdfGenerator closed");
        }
    }

    @Test
    void testSimpleHtmlToPdf() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>Test PDF</title></head>
                <body>
                    <h1>Hello, PDF!</h1>
                    <p>This is a simple test document.</p>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html).generate();

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Hello, PDF!");
        assertPdfContainsText(pdfBytes, "This is a simple test document.");
    }

    @Test
    void testHtmlWithCssToPdf() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        .header { color: #003366; font-size: 24px; font-weight: bold; }
                        .content { color: #333; line-height: 1.6; }
                        .highlight { background-color: #ffff00; padding: 5px; }
                    </style>
                </head>
                <body>
                    <div class="header">Styled Document</div>
                    <div class="content">
                        This document has CSS styling applied.
                        <span class="highlight">This text is highlighted.</span>
                    </div>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .printBackground(true)  // Enable background colors
                .build();

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withOptions(options)
                .generate();

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Styled Document");
        assertPdfContainsText(pdfBytes, "This document has CSS styling applied.");
    }

    @Test
    void testHtmlWithImagesToPdf() throws IOException {
        // Given - HTML with embedded base64 image (1x1 red pixel PNG)
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>Image Test</title></head>
                <body>
                    <h1>Document with Image</h1>
                    <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg=="
                         alt="Red pixel" width="50" height="50">
                    <p>This document contains an embedded image.</p>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .printBackground(true)
                .build();

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withOptions(options)
                .generate();

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Document with Image");
    }

    @Test
    void testLandscapeOrientation() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Landscape Document</h1>
                    <p>This document is in landscape orientation.</p>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .landscape(true)
                .build();

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withOptions(options)
                .generate();

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify landscape orientation (width > height)
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            var page = document.getPage(0);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            assertThat(width).isGreaterThan(height);
        }
    }

    @Test
    void testCustomPaperSize() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>A4 Document</h1>
                    <p>This document uses A4 paper size.</p>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .paperSize(PdfOptions.PaperFormat.A4)
                .build();

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withOptions(options)
                .generate();

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "A4 Document");

        // Verify A4 dimensions (210mm x 297mm = 8.27in x 11.69in = 595pt x 842pt)
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            var page = document.getPage(0);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();

            // Allow 1 point tolerance
            assertThat(width).isBetween(594f, 596f);
            assertThat(height).isBetween(841f, 843f);
        }
    }

    @Test
    void testCustomMargins() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Document with Custom Margins</h1>
                    <p>This document has 1cm margins on all sides.</p>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .margins("1cm")
                .build();

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withOptions(options)
                .generate();

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Document with Custom Margins");
    }

    @Test
    void testScaling() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Scaled Document</h1>
                    <p>This document is scaled to 80%.</p>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .scale(0.8)
                .build();

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withOptions(options)
                .generate();

        // Then
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Scaled Document");
    }

    @Test
    void testMultiplePdfsFromSameGenerator() throws IOException {
        // Given
        String html1 = "<html><body><h1>First Document</h1></body></html>";
        String html2 = "<html><body><h1>Second Document</h1></body></html>";
        String html3 = "<html><body><h1>Third Document</h1></body></html>";

        // When - Generate multiple PDFs from the same generator instance
        byte[] pdf1 = pdfGenerator.fromHtml(html1).generate();
        byte[] pdf2 = pdfGenerator.fromHtml(html2).generate();
        byte[] pdf3 = pdfGenerator.fromHtml(html3).generate();

        // Then - All PDFs should be valid and contain correct content
        assertValidPdf(pdf1);
        assertPdfContainsText(pdf1, "First Document");

        assertValidPdf(pdf2);
        assertPdfContainsText(pdf2, "Second Document");

        assertValidPdf(pdf3);
        assertPdfContainsText(pdf3, "Third Document");
    }

    @Test
    void testComplexHtmlStructure() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        table { border-collapse: collapse; width: 100%; }
                        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                        th { background-color: #4CAF50; color: white; }
                        ul { line-height: 1.8; }
                    </style>
                </head>
                <body>
                    <h1>Complex HTML Structure</h1>

                    <h2>Features List</h2>
                    <ul>
                        <li>Tables with styling</li>
                        <li>Multiple headings</li>
                        <li>Mixed content types</li>
                    </ul>

                    <h2>Data Table</h2>
                    <table>
                        <tr>
                            <th>Name</th>
                            <th>Value</th>
                        </tr>
                        <tr>
                            <td>Test 1</td>
                            <td>100</td>
                        </tr>
                        <tr>
                            <td>Test 2</td>
                            <td>200</td>
                        </tr>
                    </table>
                </body>
                </html>
                """;

        PdfOptions options = PdfOptions.builder()
                .printBackground(true)
                .margins("0.5in")
                .build();

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withOptions(options)
                .generate();

        // Then
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Complex HTML Structure");
        assertPdfContainsText(pdfBytes, "Features List");
        assertPdfContainsText(pdfBytes, "Data Table");
        assertPdfContainsText(pdfBytes, "Test 1");
        assertPdfContainsText(pdfBytes, "Test 2");
    }

    // ========== Helper Methods ==========

    /**
     * Validates that the byte array is a valid PDF document.
     * Checks for PDF signature and ability to load with PDFBox.
     */
    private void assertValidPdf(byte[] pdfBytes) throws IOException {
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
