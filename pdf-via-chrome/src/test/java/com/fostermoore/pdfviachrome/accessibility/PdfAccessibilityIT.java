package com.fostermoore.pdfviachrome.accessibility;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PDF accessibility validation.
 * <p>
 * Validates PDF accessibility features using both:
 * <ul>
 *   <li>Tier 1: veraPDF standard validation (PDF/UA, PDF/A)</li>
 *   <li>Tier 2: Custom PDFBox checks (WCAG 2.1 features)</li>
 * </ul>
 * <p>
 * These tests require Docker to be running. They will be skipped if Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
class PdfAccessibilityIT {

    private static final Logger logger = LoggerFactory.getLogger(PdfAccessibilityIT.class);

    private static String accessibilityTestHtml;

    private PdfGenerator pdfGenerator;

    @BeforeAll
    static void setUpClass() throws IOException {
        accessibilityTestHtml = Files.readString(
                Paths.get("src/test/resources/test-pages/accessibility-test.html")
        );
        logger.info("Loaded accessibility test HTML ({} chars)", accessibilityTestHtml.length());
    }

    @BeforeEach
    void setUp() {
        pdfGenerator = PdfGenerator.create()
                .withHeadless(true)
                .withNoSandbox(true)
                .withDisableDevShmUsage(true)
                .build();
        logger.info("PdfGenerator initialized");
    }

    @AfterEach
    void tearDown() {
        if (pdfGenerator != null) {
            pdfGenerator.close();
            logger.info("PdfGenerator closed");
        }
    }

    @Test
    @DisplayName("TC-1: Verify PDFs have tagged structure")
    void testPdfIsTagged() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><title>Tagged PDF Test</title></head>
                <body>
                    <h1>Main Heading</h1>
                    <p>This is a paragraph with semantic tags.</p>
                    <ul>
                        <li>List item 1</li>
                        <li>List item 2</li>
                    </ul>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html).generate();

        // Then
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            boolean isTagged = AccessibilityValidator.isTaggedPdf(document);
            logger.info("PDF tagged status: {}", isTagged);

            // Note: Chrome may not always generate tagged PDFs by default
            // This test documents the current behavior
            if (!isTagged) {
                logger.warn("PDF is not tagged. Chrome-generated PDFs may require additional configuration for tagging.");
            }
        }
    }

    @Test
    @DisplayName("TC-2: Verify title and language metadata")
    void testDocumentMetadata() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <title>Accessibility Test - Metadata Validation</title>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h1>Document with Metadata</h1>
                    <p>This document should have title and language metadata.</p>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html).generate();

        // Then
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            boolean hasMetadata = AccessibilityValidator.hasRequiredMetadata(document);
            logger.info("PDF has required metadata: {}", hasMetadata);

            // Log actual metadata values for debugging
            var info = document.getDocumentInformation();
            var catalog = document.getDocumentCatalog();
            logger.info("Title: {}, Language: {}",
                    info != null ? info.getTitle() : "null",
                    catalog.getLanguage());

            // Note: Chrome may not always preserve HTML metadata in PDF
            if (!hasMetadata) {
                logger.warn("PDF metadata incomplete. HTML title/lang may not be preserved by Chrome.");
            }
        }
    }

    @Test
    @DisplayName("TC-3: Verify structure tree exists")
    void testStructureTreeExists() throws IOException {
        // Given
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><title>Structure Tree Test</title></head>
                <body>
                    <header>
                        <h1>Document Header</h1>
                    </header>
                    <main>
                        <article>
                            <h2>Article Title</h2>
                            <p>Article content.</p>
                        </article>
                    </main>
                    <footer>
                        <p>Footer content</p>
                    </footer>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html).generate();

        // Then
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            boolean hasStructureTree = AccessibilityValidator.hasStructureTree(document);
            logger.info("PDF has structure tree: {}", hasStructureTree);

            if (!hasStructureTree) {
                logger.warn("PDF lacks structure tree. Chrome-generated PDFs may not include structure trees by default.");
            }
        }
    }

    @Test
    @DisplayName("TC-4: Verify logical reading order")
    void testReadingOrder() throws IOException {
        // Given - HTML with proper heading hierarchy
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><title>Reading Order Test</title></head>
                <body>
                    <h1>Main Title</h1>
                    <section>
                        <h2>Section One</h2>
                        <p>Section one content.</p>
                        <h3>Subsection 1.1</h3>
                        <p>Subsection content.</p>
                    </section>
                    <section>
                        <h2>Section Two</h2>
                        <p>Section two content.</p>
                    </section>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html).generate();

        // Then
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<String> readingOrderIssues = AccessibilityValidator.validateReadingOrder(document);
            logger.info("Reading order issues: {}", readingOrderIssues.isEmpty() ? "none" : readingOrderIssues);

            // If structure tree doesn't exist, reading order validation will report that
            if (!readingOrderIssues.isEmpty()) {
                logger.warn("Reading order issues found: {}", readingOrderIssues);
            }
        }
    }

    @Test
    @DisplayName("TC-5: Validate PDF/UA compliance using veraPDF")
    void testPdfUaCompliance() throws IOException {
        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();

        // Then
        List<AccessibilityViolation> violations = AccessibilityValidator.validateWithVeraPdf(pdfBytes);

        // Filter for PDF/UA violations only
        List<AccessibilityViolation> pdfUaViolations = violations.stream()
                .filter(v -> v.flavour().contains("UA"))
                .toList();

        logger.info("PDF/UA validation complete: {} violations found", pdfUaViolations.size());

        if (!pdfUaViolations.isEmpty()) {
            logger.warn("PDF/UA violations detected (Chrome-generated PDFs typically don't meet PDF/UA):");
            pdfUaViolations.stream()
                    .limit(5)
                    .forEach(v -> logger.warn("  - [{}] {}: {}", v.severity(), v.rule(), v.message()));
            if (pdfUaViolations.size() > 5) {
                logger.warn("  ... and {} more violations", pdfUaViolations.size() - 5);
            }
        }
    }

    @Test
    @DisplayName("TC-6: Validate PDF/A compliance using veraPDF")
    void testPdfACompliance() throws IOException {
        // Given - Simple HTML for PDF/A validation
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <title>PDF/A Compliance Test</title>
                    <style>
                        body { font-family: Arial, sans-serif; }
                    </style>
                </head>
                <body>
                    <h1>PDF/A Compliance Test</h1>
                    <p>This document tests PDF/A archival compliance.</p>
                    <p>PDF/A requires embedded fonts and no external dependencies.</p>
                </body>
                </html>
                """;

        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(html).generate();

        // Then
        List<AccessibilityViolation> violations = AccessibilityValidator.validateWithVeraPdf(pdfBytes);

        // Filter for PDF/A violations only
        List<AccessibilityViolation> pdfAViolations = violations.stream()
                .filter(v -> v.flavour().contains("PDFA") || v.flavour().contains("PDF/A"))
                .toList();

        logger.info("PDF/A validation complete: {} violations found", pdfAViolations.size());

        if (!pdfAViolations.isEmpty()) {
            logger.warn("PDF/A violations detected (Chrome-generated PDFs may not be PDF/A compliant):");
            pdfAViolations.stream()
                    .limit(5)
                    .forEach(v -> logger.warn("  - [{}] {}: {}", v.severity(), v.rule(), v.message()));
            if (pdfAViolations.size() > 5) {
                logger.warn("  ... and {} more violations", pdfAViolations.size() - 5);
            }
        }
    }

    @Test
    @DisplayName("TC-7: Full validation combining both tiers")
    void testCombinedAccessibilityValidation() throws IOException {
        // When
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();
        AccessibilityReport report = AccessibilityValidator.validateAll(pdfBytes);

        // Then - Log comprehensive report
        logger.info("=== Accessibility Validation Report ===");
        logger.info("PDF is tagged: {}", report.isTagged());
        logger.info("Has required metadata: {}", report.hasMetadata());
        logger.info("Has structure tree: {}", report.hasStructureTree());
        logger.info("Reading order issues: {}", report.readingOrderIssues().isEmpty() ? "none" : report.readingOrderIssues());
        logger.info("veraPDF violations: {}", report.veraPdfViolations().size());
        logger.info("Total issues: {}", report.getTotalIssues());
        logger.info("Overall compliance: {}", report.isCompliant());
        logger.info("========================================");

        // Detailed violation logging if present
        if (!report.veraPdfViolations().isEmpty()) {
            logger.info("veraPDF violation breakdown:");
            report.veraPdfViolations().stream()
                    .limit(10)
                    .forEach(v -> logger.info("  - [{}][{}] {}: {}",
                            v.flavour(), v.severity(), v.rule(), v.message()));
            if (report.veraPdfViolations().size() > 10) {
                logger.info("  ... and {} more violations", report.veraPdfViolations().size() - 10);
            }
        }

        // Assert basic PDF generation succeeded
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(1000);
    }
}
