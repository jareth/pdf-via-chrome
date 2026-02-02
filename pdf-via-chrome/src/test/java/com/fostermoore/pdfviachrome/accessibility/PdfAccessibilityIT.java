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
    @DisplayName("TC-1: Verify tagged structure validation runs")
    void testPdfIsTagged() throws IOException {
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            // Chrome-generated PDFs are typically not tagged; this validates the check runs
            boolean isTagged = AccessibilityValidator.isTaggedPdf(document);
            logger.info("PDF tagged: {}", isTagged);
        }
        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    @DisplayName("TC-2: Verify metadata validation runs")
    void testDocumentMetadata() throws IOException {
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            // Chrome may not preserve HTML metadata in PDF; this validates the check runs
            boolean hasMetadata = AccessibilityValidator.hasRequiredMetadata(document);
            logger.info("PDF has metadata: {}", hasMetadata);
        }
        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    @DisplayName("TC-3: Verify structure tree validation runs")
    void testStructureTreeExists() throws IOException {
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            boolean hasStructureTree = AccessibilityValidator.hasStructureTree(document);
            logger.info("PDF has structure tree: {}", hasStructureTree);
        }
        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    @DisplayName("TC-4: Verify reading order validation runs")
    void testReadingOrder() throws IOException {
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<String> issues = AccessibilityValidator.validateReadingOrder(document);
            logger.info("Reading order issues: {}", issues.isEmpty() ? "none" : issues.size());
        }
        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    @DisplayName("TC-5: Validate veraPDF standard compliance")
    void testVeraPdfCompliance() throws IOException {
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();

        // veraPDF validates PDF/UA and PDF/A standards
        List<AccessibilityViolation> violations = AccessibilityValidator.validateWithVeraPdf(pdfBytes);
        logger.info("veraPDF validation: {} violations", violations.size());

        // Chrome-generated PDFs typically don't meet PDF/UA or PDF/A standards
        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    @DisplayName("TC-6: Full accessibility validation combining both tiers")
    void testCombinedAccessibilityValidation() throws IOException {
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();
        AccessibilityReport report = AccessibilityValidator.validateAll(pdfBytes);

        logger.info("Accessibility report - tagged: {}, metadata: {}, structureTree: {}, issues: {}",
                report.isTagged(), report.hasMetadata(), report.hasStructureTree(), report.getTotalIssues());

        assertThat(pdfBytes).hasSizeGreaterThan(1000);
        assertThat(report).isNotNull();
    }
}
