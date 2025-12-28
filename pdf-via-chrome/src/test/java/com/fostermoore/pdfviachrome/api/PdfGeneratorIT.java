package com.fostermoore.pdfviachrome.api;

import com.fostermoore.pdfviachrome.util.ChromePathDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PdfGenerator with actual Chrome instances.
 *
 * These tests require Chrome to be installed on the system.
 * They are disabled by default and can be enabled by setting the
 * CHROME_INTEGRATION_TESTS environment variable to "true".
 *
 * To run: mvn verify -DCHROME_INTEGRATION_TESTS=true
 */
@EnabledIfEnvironmentVariable(named = "CHROME_INTEGRATION_TESTS", matches = "true")
class PdfGeneratorIT {

    @Test
    void testGeneratePdfFromHtml_simple() throws Exception {
        // Auto-detect Chrome
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent()
            .withFailMessage("Chrome not found on system. Please install Chrome to run integration tests.");

        String html = "<html><body><h1>Hello World</h1><p>This is a test PDF.</p></body></html>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .withTimeout(Duration.ofSeconds(30))
            .build()) {

            byte[] pdf = generator.fromHtml(html).generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(pdf.length).isGreaterThan(100); // PDFs are at least a few hundred bytes

            // Check PDF header
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testGeneratePdfFromHtml_withOptions() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = "<html><body><h1>Landscape PDF</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .landscape(true)
                .paperSize(PdfOptions.PaperFormat.A4)
                .printBackground(true)
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testGeneratePdfFromHtml_withCustomMargins() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = "<html><body><h1>Custom Margins</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .margins(0.5)
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
        }
    }

    @Test
    void testGeneratePdfFromUrl() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .withTimeout(Duration.ofSeconds(60))
            .build()) {

            byte[] pdf = generator.fromUrl("https://example.com")
                .withOptions(PdfOptions.defaults())
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testGenerateMultiplePdfs_sequentially() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            // Generate first PDF
            byte[] pdf1 = generator.fromHtml("<html><body><h1>PDF 1</h1></body></html>")
                .generate();

            assertThat(pdf1).isNotNull();
            assertThat(pdf1).isNotEmpty();

            // Generate second PDF with same generator
            byte[] pdf2 = generator.fromHtml("<html><body><h1>PDF 2</h1></body></html>")
                .withOptions(PdfOptions.builder().landscape(true).build())
                .generate();

            assertThat(pdf2).isNotNull();
            assertThat(pdf2).isNotEmpty();

            // Generate third PDF from URL
            byte[] pdf3 = generator.fromUrl("https://example.com")
                .generate();

            assertThat(pdf3).isNotNull();
            assertThat(pdf3).isNotEmpty();

            // All should be valid PDFs
            assertThat(new String(pdf1, 0, 4)).isEqualTo("%PDF");
            assertThat(new String(pdf2, 0, 4)).isEqualTo("%PDF");
            assertThat(new String(pdf3, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testLazyInitialization() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            // Initially not initialized
            assertThat(generator.isInitialized()).isFalse();

            // Generate PDF (triggers initialization)
            byte[] pdf = generator.fromHtml("<html><body><h1>Test</h1></body></html>")
                .generate();

            assertThat(pdf).isNotNull();

            // Now should be initialized
            assertThat(generator.isInitialized()).isTrue();
        }
    }

    @Test
    void testAutoCloseable_cleanupResources() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build();

        // Generate a PDF to initialize
        byte[] pdf = generator.fromHtml("<html><body><h1>Test</h1></body></html>")
            .generate();

        assertThat(pdf).isNotNull();
        assertThat(generator.isInitialized()).isTrue();

        // Close the generator
        generator.close();

        // After close, should not be able to generate
        assertThatThrownBy(() ->
            generator.fromHtml("<html></html>").generate()
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testComplexHtml_withStyles() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    h1 { color: #333; background-color: #f0f0f0; padding: 10px; }
                    .box { border: 2px solid #000; padding: 15px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <h1>Styled PDF Document</h1>
                <div class="box">
                    <h2>Section 1</h2>
                    <p>This is a paragraph with some <strong>bold text</strong> and <em>italic text</em>.</p>
                </div>
                <div class="box">
                    <h2>Section 2</h2>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                        <li>Item 3</li>
                    </ul>
                </div>
            </body>
            </html>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .printBackground(true)
                .scale(0.9)
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testDockerOptions() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = "<html><body><h1>Docker Test</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .withNoSandbox(true)
            .withDisableDevShmUsage(true)
            .build()) {

            byte[] pdf = generator.fromHtml(html).generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
        }
    }

    @Test
    void testDefaultChromeDetection() throws Exception {
        // Test with auto-detected Chrome (no explicit path)
        String html = "<html><body><h1>Auto-detected Chrome</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create().build()) {

            byte[] pdf = generator.fromHtml(html).generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testPageRanges_multiPageDocument() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        // Create HTML with multiple pages using page breaks
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    @media print {
                        .page-break { page-break-after: always; }
                    }
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    h1 { font-size: 24px; }
                    p { font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="page-break">
                    <h1>Page 1</h1>
                    <p>This is the content of page 1.</p>
                </div>
                <div class="page-break">
                    <h1>Page 2</h1>
                    <p>This is the content of page 2.</p>
                </div>
                <div class="page-break">
                    <h1>Page 3</h1>
                    <p>This is the content of page 3.</p>
                </div>
                <div class="page-break">
                    <h1>Page 4</h1>
                    <p>This is the content of page 4.</p>
                </div>
                <div>
                    <h1>Page 5</h1>
                    <p>This is the content of page 5.</p>
                </div>
            </body>
            </html>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            // Test 1: Generate PDF with all pages (no page ranges)
            PdfOptions allPagesOptions = PdfOptions.builder()
                .printBackground(true)
                .build();

            byte[] allPagesPdf = generator.fromHtml(html)
                .withOptions(allPagesOptions)
                .generate();

            assertThat(allPagesPdf).isNotNull();
            assertThat(allPagesPdf).isNotEmpty();
            assertThat(new String(allPagesPdf, 0, 4)).isEqualTo("%PDF");

            // Test 2: Generate PDF with only pages 1-3
            PdfOptions rangeOptions = PdfOptions.builder()
                .pageRanges("1-3")
                .printBackground(true)
                .build();

            byte[] rangePdf = generator.fromHtml(html)
                .withOptions(rangeOptions)
                .generate();

            assertThat(rangePdf).isNotNull();
            assertThat(rangePdf).isNotEmpty();
            assertThat(new String(rangePdf, 0, 4)).isEqualTo("%PDF");

            // Test 3: Generate PDF with specific pages (1, 3, 5)
            PdfOptions specificPagesOptions = PdfOptions.builder()
                .pageRanges("1,3,5")
                .printBackground(true)
                .build();

            byte[] specificPagesPdf = generator.fromHtml(html)
                .withOptions(specificPagesOptions)
                .generate();

            assertThat(specificPagesPdf).isNotNull();
            assertThat(specificPagesPdf).isNotEmpty();
            assertThat(new String(specificPagesPdf, 0, 4)).isEqualTo("%PDF");

            // Test 4: Generate PDF with mixed ranges (1-2, 4)
            PdfOptions mixedRangesOptions = PdfOptions.builder()
                .pageRanges("1-2,4")
                .printBackground(true)
                .build();

            byte[] mixedRangesPdf = generator.fromHtml(html)
                .withOptions(mixedRangesOptions)
                .generate();

            assertThat(mixedRangesPdf).isNotNull();
            assertThat(mixedRangesPdf).isNotEmpty();
            assertThat(new String(mixedRangesPdf, 0, 4)).isEqualTo("%PDF");

            // Verify that different page selections produce different size PDFs
            // (PDFs with fewer pages should generally be smaller)
            assertThat(rangePdf.length).isLessThan(allPagesPdf.length);
            assertThat(specificPagesPdf.length).isLessThan(allPagesPdf.length);
        }
    }

    @Test
    void testPageRanges_singlePage() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    @media print {
                        .page-break { page-break-after: always; }
                    }
                </style>
            </head>
            <body>
                <div class="page-break"><h1>Page 1</h1></div>
                <div class="page-break"><h1>Page 2</h1></div>
                <div><h1>Page 3</h1></div>
            </body>
            </html>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            // Generate PDF with only page 2
            PdfOptions options = PdfOptions.builder()
                .pageRanges("2")
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testPageRanges_emptyString_generatesAllPages() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = "<html><body><h1>Test Page</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            // Empty page ranges should generate all pages
            PdfOptions options = PdfOptions.builder()
                .pageRanges("")
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }
}
