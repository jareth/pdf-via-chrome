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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for advanced PDF generation features.
 * <p>
 * Tests combinations of:
 * - CSS injection
 * - JavaScript execution
 * - Page range selection
 * - Custom headers and footers
 * <p>
 * These tests validate that advanced features work correctly both individually
 * and in combination with each other.
 * <p>
 * The @Testcontainers annotation ensures Docker is available. If Docker is not available,
 * these tests will be skipped gracefully.
 */
@Testcontainers(disabledWithoutDocker = true)
class AdvancedFeaturesIT {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedFeaturesIT.class);

    private PdfGenerator pdfGenerator;
    private static String multiPageHtml;
    private static String printStylesCss;
    private static String pageManipulationJs;

    @BeforeAll
    static void setUpClass() throws IOException {
        logger.info("Loading test resources for AdvancedFeaturesIT");

        // Load test resources once for all tests
        Path htmlPath = Paths.get("src/test/resources/test-pages/multi-page.html");
        Path cssPath = Paths.get("src/test/resources/test-css/print-styles.css");
        Path jsPath = Paths.get("src/test/resources/test-js/page-manipulation.js");

        multiPageHtml = Files.readString(htmlPath);
        printStylesCss = Files.readString(cssPath);
        pageManipulationJs = Files.readString(jsPath);

        logger.info("Test resources loaded successfully");
    }

    @BeforeEach
    void setUp() {
        logger.info("Setting up PdfGenerator for test");

        // Create fresh PdfGenerator for each test
        pdfGenerator = PdfGenerator.create()
                .withHeadless(true)
                .withNoSandbox(true)
                .withDisableDevShmUsage(true)
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

    // ========== Individual Feature Tests ==========

    @Test
    void testCssInjection() throws IOException {
        logger.info("Testing CSS injection feature");

        // When - Generate PDF with custom CSS
        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withCustomCss(printStylesCss)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify content is present
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "Page 2: Data Tables");
        assertPdfContainsText(pdfBytes, "multi-page test document");

        logger.info("CSS injection test passed");
    }

    @Test
    void testJavaScriptExecution() throws IOException {
        logger.info("Testing JavaScript execution feature");

        // When - Generate PDF with JavaScript that modifies the page
        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .executeJavaScript(pageManipulationJs)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify main content is present
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "multi-page test document");

        // Note: The JavaScript removes elements with class 'removable' and shows dynamic content
        // We can't easily verify that content was removed via text extraction,
        // but we can verify the PDF was generated successfully

        logger.info("JavaScript execution test passed");
    }

    @Test
    void testPageRangeSelection() throws IOException {
        logger.info("Testing page range selection feature");

        // When - Generate PDF with only pages 1-3
        PdfOptions options = PdfOptions.builder()
                .pageRanges("1-3")
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify page count
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }

        // Verify content from first 3 pages is present
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "Page 2: Data Tables");
        assertPdfContainsText(pdfBytes, "Page 3: More Content");

        logger.info("Page range selection test passed");
    }

    @Test
    void testSpecificPageSelection() throws IOException {
        logger.info("Testing specific page selection (1,3,5)");

        // When - Generate PDF with only pages 1, 3, and 5
        PdfOptions options = PdfOptions.builder()
                .pageRanges("1,3,5")
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify page count
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }

        // Verify content from selected pages
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "Page 3: More Content");
        assertPdfContainsText(pdfBytes, "Page 5: Final Page");

        logger.info("Specific page selection test passed");
    }

    @Test
    void testHeadersAndFootersWithPageNumbers() throws IOException {
        logger.info("Testing headers and footers with page numbers");

        // When - Generate PDF with page numbers in footer
        PdfOptions options = PdfOptions.builder()
                .simplePageNumbers()
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify all pages are present
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(5);
        }

        logger.info("Headers and footers test passed");
    }

    // ========== Combined Feature Tests ==========

    @Test
    void testCssAndJavaScriptCombined() throws IOException {
        logger.info("Testing CSS injection + JavaScript execution combined");

        // When - Generate PDF with both CSS and JavaScript
        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withCustomCss(printStylesCss)
                .executeJavaScript(pageManipulationJs)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify content is present
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "Data Tables");
        assertPdfContainsText(pdfBytes, "multi-page test document");

        logger.info("CSS + JavaScript combined test passed");
    }

    @Test
    void testHeadersAndPageRangesCombined() throws IOException {
        logger.info("Testing headers/footers + page ranges combined");

        // When - Generate PDF with headers and specific page range
        PdfOptions options = PdfOptions.builder()
                .standardHeaderFooter()
                .pageRanges("1-3")
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify page count
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }

        // Verify content
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "Page 2: Data Tables");

        logger.info("Headers + page ranges combined test passed");
    }

    @Test
    void testCssJavaScriptAndPageRanges() throws IOException {
        logger.info("Testing CSS + JavaScript + page ranges combined");

        // When - Generate PDF with CSS, JavaScript, and page ranges
        PdfOptions options = PdfOptions.builder()
                .pageRanges("2-4")
                .printBackground(true)
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withCustomCss(printStylesCss)
                .executeJavaScript(pageManipulationJs)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify page count
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }

        // Verify content from selected pages
        assertPdfContainsText(pdfBytes, "Page 2: Data Tables");
        assertPdfContainsText(pdfBytes, "Page 3: More Content");
        assertPdfContainsText(pdfBytes, "Page 4: Code Examples");

        logger.info("CSS + JavaScript + page ranges combined test passed");
    }

    @Test
    void testAllFeaturesCombined() throws IOException {
        logger.info("Testing all advanced features combined");

        // When - Generate PDF with all features: CSS, JavaScript, page ranges, and headers/footers
        PdfOptions options = PdfOptions.builder()
                .standardHeaderFooter()
                .pageRanges("1-3")
                .printBackground(true)
                .margins("1cm")
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withCustomCss(printStylesCss)
                .executeJavaScript(pageManipulationJs)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify page count
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }

        // Verify content
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "Page 2: Data Tables");
        assertPdfContainsText(pdfBytes, "multi-page test document");

        logger.info("All features combined test passed");
    }

    @Test
    void testJavaScriptModifyingPageBeforeCssInjection() throws IOException {
        logger.info("Testing JavaScript modifying page structure before CSS is applied");

        // Custom JavaScript that modifies the DOM structure
        String customJs = """
            // Change the title
            document.title = "Modified by JavaScript";

            // Add a new heading
            const newHeading = document.createElement('h1');
            newHeading.textContent = 'JavaScript Modified Heading';
            newHeading.id = 'js-heading';
            document.body.insertBefore(newHeading, document.body.firstChild);
            """;

        // Custom CSS that styles the new heading
        String customCss = """
            #js-heading {
                color: #ff0000 !important;
                font-size: 32px !important;
                text-align: center !important;
                background-color: #ffff00 !important;
                padding: 20px !important;
            }
            """;

        // When - Generate PDF with JavaScript first, then CSS
        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .executeJavaScript(customJs)
                .withCustomCss(customCss)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify the JavaScript-added content is present
        assertPdfContainsText(pdfBytes, "JavaScript Modified Heading");

        logger.info("JavaScript + CSS order test passed");
    }

    @Test
    void testComplexHeaderTemplateWithStyling() throws IOException {
        logger.info("Testing complex header template with custom styling");

        // Complex header with styling
        String complexHeader = """
            <div style="
                width: 100%;
                font-size: 10px;
                padding: 10px 1cm;
                border-bottom: 2px solid #003366;
                background-color: #f0f0f0;
                display: flex;
                justify-content: space-between;
            ">
                <span style="font-weight: bold; color: #003366;">
                    <span class="title"></span>
                </span>
                <span style="color: #666666;">
                    Page <span class="pageNumber"></span> of <span class="totalPages"></span>
                </span>
            </div>
            """;

        String complexFooter = """
            <div style="
                width: 100%;
                font-size: 9px;
                padding: 5px 1cm;
                border-top: 1px solid #cccccc;
                text-align: center;
                color: #999999;
            ">
                Generated on <span class="date"></span>
            </div>
            """;

        // When - Generate PDF with complex headers and footers
        PdfOptions options = PdfOptions.builder()
                .displayHeaderFooter(true)
                .headerTemplate(complexHeader)
                .footerTemplate(complexFooter)
                .printBackground(true)
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        logger.info("Complex header template test passed");
    }

    @Test
    void testCssInjectionFromFile() throws IOException {
        logger.info("Testing CSS injection from external file");

        // When - Generate PDF with CSS loaded from file
        Path cssFile = Paths.get("src/test/resources/test-css/print-styles.css");
        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withCustomCssFromFile(cssFile)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "multi-page test document");

        logger.info("CSS injection from file test passed");
    }

    @Test
    void testJavaScriptExecutionFromFile() throws IOException {
        logger.info("Testing JavaScript execution from external file");

        // When - Generate PDF with JavaScript loaded from file
        Path jsFile = Paths.get("src/test/resources/test-js/page-manipulation.js");
        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .executeJavaScriptFromFile(jsFile)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "multi-page test document");

        logger.info("JavaScript execution from file test passed");
    }

    @Test
    void testMixedPageRangeFormats() throws IOException {
        logger.info("Testing mixed page range formats (1-2,4)");

        // When - Generate PDF with mixed page range format
        PdfOptions options = PdfOptions.builder()
                .pageRanges("1-2,4")
                .build();

        byte[] pdfBytes = pdfGenerator.fromHtml(multiPageHtml)
                .withOptions(options)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify page count (pages 1, 2, and 4)
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }

        // Verify content from selected pages
        assertPdfContainsText(pdfBytes, "Page 1: Introduction");
        assertPdfContainsText(pdfBytes, "Page 2: Data Tables");
        assertPdfContainsText(pdfBytes, "Page 4: Code Examples");

        logger.info("Mixed page range formats test passed");
    }

    @Test
    void testBaseUrlWithRelativeImages() throws IOException {
        logger.info("Testing base URL feature for resolving relative image paths");

        // HTML with relative image path (would fail without base URL)
        String htmlWithRelativeImage = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Base URL Test</title>
            </head>
            <body>
                <h1>Testing Base URL</h1>
                <p>This document contains a reference to a relative resource.</p>
                <img src="/images/test.png" alt="Test Image"/>
                <p>If base URL works correctly, the image path will be resolved.</p>
            </body>
            </html>
            """;

        // When - Generate PDF with base URL
        // Note: This test verifies the feature works without errors
        // A full end-to-end test with actual image loading would require a running web server
        byte[] pdfBytes = pdfGenerator.fromHtml(htmlWithRelativeImage)
                .withBaseUrl("http://localhost:8080/")
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify content is present
        assertPdfContainsText(pdfBytes, "Testing Base URL");
        assertPdfContainsText(pdfBytes, "This document contains a reference to a relative resource");

        logger.info("Base URL test passed");
    }

    @Test
    void testBaseUrlCombinedWithOtherFeatures() throws IOException {
        logger.info("Testing base URL combined with CSS and JavaScript");

        // HTML with relative paths
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Combined Features Test</title>
            </head>
            <body>
                <div id="content">
                    <h1>Combined Features</h1>
                    <img src="/logo.png" alt="Logo"/>
                </div>
            </body>
            </html>
            """;

        // Custom CSS to style the content
        String css = """
            #content {
                border: 2px solid blue;
                padding: 20px;
            }
            h1 {
                color: green;
            }
            """;

        // JavaScript to add a message
        String js = """
            const msg = document.createElement('p');
            msg.textContent = 'Features combined successfully';
            document.getElementById('content').appendChild(msg);
            """;

        // When - Generate PDF with base URL, CSS, and JavaScript
        byte[] pdfBytes = pdfGenerator.fromHtml(html)
                .withBaseUrl("http://localhost:8080/")
                .withCustomCss(css)
                .executeJavaScript(js)
                .generate();

        // Then - Verify PDF is valid
        assertThat(pdfBytes).isNotEmpty();
        assertValidPdf(pdfBytes);

        // Verify all features worked
        assertPdfContainsText(pdfBytes, "Combined Features");
        assertPdfContainsText(pdfBytes, "Features combined successfully");

        logger.info("Base URL combined with other features test passed");
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
