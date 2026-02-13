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
 *   <li>Tier 1: veraPDF standard validation (PDF/A)</li>
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

    private byte[] generatePdf() throws IOException {
        byte[] pdfBytes = pdfGenerator.fromHtml(accessibilityTestHtml).generate();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
        return pdfBytes;
    }

    @Test
    @DisplayName("TC-1: Verify tagged structure validation runs")
    void testPdfIsTagged() throws IOException {
        byte[] pdfBytes = generatePdf();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            // Verify that tagged-structure validation executes without error
            boolean isTagged = AccessibilityValidator.isTaggedPdf(document);
            logger.info("PDF tagged: {}", isTagged);
            // Chrome produces tagged PDFs from well-structured HTML
            assertThat(isTagged).isTrue();
        }
    }

    @Test
    @DisplayName("TC-2: Verify metadata validation runs")
    void testDocumentMetadata() throws IOException {
        byte[] pdfBytes = generatePdf();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            boolean hasMetadata = AccessibilityValidator.hasRequiredMetadata(document);
            logger.info("PDF has metadata: {}", hasMetadata);
            // Chrome preserves title and language from the test HTML
            assertThat(hasMetadata).isTrue();
        }
    }

    @Test
    @DisplayName("TC-3: Verify structure tree validation runs")
    void testStructureTreeExists() throws IOException {
        byte[] pdfBytes = generatePdf();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            boolean hasStructureTree = AccessibilityValidator.hasStructureTree(document);
            logger.info("PDF has structure tree: {}", hasStructureTree);
            // Chrome generates a structure tree from well-structured HTML
            assertThat(hasStructureTree).isTrue();
        }
    }

    @Test
    @DisplayName("TC-4: Verify reading order validation runs")
    void testReadingOrder() throws IOException {
        byte[] pdfBytes = generatePdf();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<String> issues = AccessibilityValidator.validateReadingOrder(document);
            logger.info("Reading order issues: {}", issues.isEmpty() ? "none" : issues.size());
            // Well-structured test HTML should produce valid reading order
            assertThat(issues).isEmpty();
        }
    }

    @Test
    @DisplayName("TC-5: Validate veraPDF standard compliance")
    void testVeraPdfCompliance() throws IOException {
        byte[] pdfBytes = generatePdf();

        // veraPDF validates PDF/A standards
        VeraPdfResult result = AccessibilityValidator.validateWithVeraPdf(pdfBytes);
        logger.info("veraPDF validation ran: {}, violations: {}", result.validationRan(), result.violations().size());

        // Chrome PDFs are not marked as PDF/A, so veraPDF skips standard validation
        assertThat(result.validationRan()).isFalse();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    @DisplayName("TC-6: Full accessibility validation combining both tiers")
    void testCombinedAccessibilityValidation() throws IOException {
        byte[] pdfBytes = generatePdf();

        AccessibilityReport report = AccessibilityValidator.validateAll(pdfBytes);

        logger.info("Accessibility report - tagged: {}, metadata: {}, structureTree: {}, " +
                        "tier1Ran: {}, issues: {}",
                report.isTagged(), report.hasMetadata(), report.hasStructureTree(),
                report.tier1Ran(), report.getTotalIssues());

        assertThat(report).isNotNull();
        // Chrome PDFs are not PDF/A, so Tier 1 validation is skipped
        assertThat(report.tier1Ran()).isFalse();
        // Chrome produces accessible PDFs from well-structured HTML with proper metadata
        assertThat(report.isTagged()).isTrue();
        assertThat(report.hasMetadata()).isTrue();
        assertThat(report.hasStructureTree()).isTrue();
        assertThat(report.isCompliant()).isTrue();
    }
}
