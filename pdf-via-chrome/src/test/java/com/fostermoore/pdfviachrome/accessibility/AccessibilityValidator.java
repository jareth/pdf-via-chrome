package com.fostermoore.pdfviachrome.accessibility;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.VeraPDFFoundry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating PDF accessibility compliance.
 * <p>
 * Implements two-tier validation approach:
 * <ul>
 *   <li>Tier 1: veraPDF standard validation for PDF/UA and PDF/A compliance</li>
 *   <li>Tier 2: Custom PDFBox checks for WCAG 2.1 compliance features</li>
 * </ul>
 * <p>
 * All methods are static for convenient access from integration tests.
 */
public class AccessibilityValidator {

    private static final Logger logger = LoggerFactory.getLogger(AccessibilityValidator.class);

    static {
        try {
            VeraGreenfieldFoundryProvider.initialise();
            logger.info("veraPDF initialized");
        } catch (Exception e) {
            throw new ExceptionInInitializerError("veraPDF initialization failed: " + e.getMessage());
        }
    }

    // Private constructor to prevent instantiation
    private AccessibilityValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validates PDF using veraPDF for PDF/UA and PDF/A compliance (Tier 1).
     * <p>
     * This method attempts to validate against detected PDF flavours. If validation
     * profiles are not available (common with the basic veraPDF dependencies),
     * it gracefully handles the error and returns an empty list.
     *
     * @param pdfBytes PDF document as byte array
     * @return List of accessibility violations found (empty if profiles unavailable)
     * @throws IOException if PDF parsing fails
     */
    public static List<AccessibilityViolation> validateWithVeraPdf(byte[] pdfBytes) throws IOException {
        List<AccessibilityViolation> violations = new ArrayList<>();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
             VeraPDFFoundry foundry = Foundries.defaultInstance();
             PDFAParser parser = foundry.createParser(inputStream)) {

            // Detect PDF/A and PDF/UA flavours
            PDFAFlavour detectedFlavour = parser.getFlavour();
            logger.debug("Detected PDF flavour: {}", detectedFlavour);

            // Only validate if a specific flavour is detected
            if (detectedFlavour != null && detectedFlavour != PDFAFlavour.NO_FLAVOUR) {
                violations.addAll(validateAgainstFlavour(foundry, parser, detectedFlavour));
            } else {
                // For regular PDFs (no specific flavour), try available profiles
                logger.info("No specific PDF/A or PDF/UA flavour detected. Skipping standard validation.");
                logger.info("Chrome-generated PDFs are typically not marked as PDF/A or PDF/UA.");
            }

        } catch (Exception e) {
            logger.error("Error during veraPDF validation", e);
            throw new IOException("veraPDF validation failed", e);
        }

        logger.info("veraPDF validation complete: {} violations found", violations.size());
        return violations;
    }

    /**
     * Validates PDF against a specific flavour, handling missing profiles gracefully.
     *
     * @param foundry veraPDF foundry instance
     * @param parser PDF parser
     * @param flavour PDF flavour to validate against
     * @return List of violations found
     */
    private static List<AccessibilityViolation> validateAgainstFlavour(
            VeraPDFFoundry foundry, PDFAParser parser, PDFAFlavour flavour) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        try {
            logger.debug("Validating against flavour: {}", flavour);

            // Create validator for this flavour
            PDFAValidator validator = foundry.createValidator(flavour, false);

            // Run validation
            ValidationResult result = validator.validate(parser);

            // Collect violations
            if (!result.isCompliant()) {
                logger.debug("Validation failed for {}: {} violations",
                        flavour, result.getTestAssertions().size());

                for (TestAssertion assertion : result.getTestAssertions()) {
                    if (assertion.getStatus() != TestAssertion.Status.PASSED) {
                        violations.add(new AccessibilityViolation(
                                flavour.toString(),
                                assertion.getRuleId().getClause(),
                                assertion.getMessage(),
                                assertion.getStatus().toString().toLowerCase()
                        ));
                    }
                }
            } else {
                logger.debug("Validation passed for {}", flavour);
            }
        } catch (java.util.NoSuchElementException e) {
            // Profile not available - this is expected with basic veraPDF dependencies
            logger.warn("Validation profile for {} not available. " +
                    "Add verapdf-validation dependency for full profile support.", flavour);
        } catch (Exception e) {
            logger.warn("Error validating against {}: {}", flavour, e.getMessage());
        }

        return violations;
    }

    /**
     * Checks if PDF is marked as tagged for accessibility (Tier 2).
     *
     * @param document PDFBox document instance
     * @return true if PDF is tagged
     */
    public static boolean isTaggedPdf(PDDocument document) {
        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMarkInfo markInfo = catalog.getMarkInfo();

            boolean isMarked = markInfo != null && markInfo.isMarked();
            logger.debug("PDF tagged status: {}", isMarked);
            return isMarked;
        } catch (Exception e) {
            logger.error("Error checking tagged status", e);
            return false;
        }
    }

    /**
     * Checks if PDF has required accessibility metadata (title and language) (Tier 2).
     *
     * @param document PDFBox document instance
     * @return true if both title and language are present
     */
    public static boolean hasRequiredMetadata(PDDocument document) {
        try {
            PDDocumentInformation info = document.getDocumentInformation();
            PDDocumentCatalog catalog = document.getDocumentCatalog();

            boolean hasTitle = info != null && info.getTitle() != null && !info.getTitle().trim().isEmpty();
            String language = catalog.getLanguage();
            boolean hasLanguage = language != null && !language.trim().isEmpty();

            logger.debug("Metadata - Title: {}, Language: {}", hasTitle, hasLanguage);
            return hasTitle && hasLanguage;
        } catch (Exception e) {
            logger.error("Error checking metadata", e);
            return false;
        }
    }

    /**
     * Checks if PDF has a structure tree for logical document structure (Tier 2).
     *
     * @param document PDFBox document instance
     * @return true if structure tree root exists
     */
    public static boolean hasStructureTree(PDDocument document) {
        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDStructureTreeRoot structureTree = catalog.getStructureTreeRoot();

            boolean hasTree = structureTree != null;
            logger.debug("Structure tree present: {}", hasTree);
            return hasTree;
        } catch (Exception e) {
            logger.error("Error checking structure tree", e);
            return false;
        }
    }

    /**
     * Validates logical reading order of PDF structure (Tier 2).
     * <p>
     * Performs basic reading order validation by traversing the structure tree
     * and checking for common issues.
     *
     * @param document PDFBox document instance
     * @return List of reading order issues found (empty if valid)
     */
    public static List<String> validateReadingOrder(PDDocument document) {
        List<String> issues = new ArrayList<>();

        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDStructureTreeRoot structureTree = catalog.getStructureTreeRoot();

            if (structureTree == null) {
                issues.add("No structure tree present - cannot validate reading order");
                return issues;
            }

            // Collect structure elements in document order
            List<String> elementTypes = new ArrayList<>();
            traverseStructureTree(structureTree, elementTypes);

            logger.debug("Document structure: {}", elementTypes);

            // Basic validation: check for heading hierarchy
            int lastHeadingLevel = 0;
            for (int i = 0; i < elementTypes.size(); i++) {
                String type = elementTypes.get(i);

                // Check heading hierarchy (H1 through H6)
                if (type.matches("^H[1-6]$")) {
                    int level = Integer.parseInt(type.substring(1));

                    // First heading should be H1
                    if (lastHeadingLevel == 0 && level != 1) {
                        issues.add("First heading is " + type + " but should be H1");
                    }

                    // Headings should not skip levels (e.g., H1 to H3)
                    if (lastHeadingLevel > 0 && level > lastHeadingLevel + 1) {
                        issues.add("Heading level skipped: H" + lastHeadingLevel +
                                " followed by " + type);
                    }

                    lastHeadingLevel = level;
                }
            }

            if (issues.isEmpty()) {
                logger.debug("Reading order validation passed");
            } else {
                logger.debug("Reading order issues: {}", issues);
            }

        } catch (Exception e) {
            logger.error("Error validating reading order", e);
            issues.add("Error during reading order validation: " + e.getMessage());
        }

        return issues;
    }

    /**
     * Recursively traverses the structure tree to collect element types.
     *
     * @param element Current structure element
     * @param elementTypes List to collect element types
     */
    private static void traverseStructureTree(Object element, List<String> elementTypes) {
        if (element instanceof PDStructureTreeRoot root) {
            for (Object kid : root.getKids()) {
                traverseStructureTree(kid, elementTypes);
            }
        } else if (element instanceof PDStructureElement structElement) {
            String type = structElement.getStructureType();
            if (type != null) {
                elementTypes.add(type);
            }
            List<?> kids = structElement.getKids();
            if (kids != null) {
                for (Object kid : kids) {
                    traverseStructureTree(kid, elementTypes);
                }
            }
        }
    }

    /**
     * Performs comprehensive accessibility validation combining both tiers.
     *
     * @param pdfBytes PDF document as byte array
     * @return Complete accessibility report
     * @throws IOException if PDF processing fails
     */
    public static AccessibilityReport validateAll(byte[] pdfBytes) throws IOException {
        logger.info("Starting comprehensive accessibility validation");

        // Tier 1: veraPDF validation
        List<AccessibilityViolation> veraPdfViolations = validateWithVeraPdf(pdfBytes);

        // Tier 2: PDFBox validation
        boolean isTagged = false;
        boolean hasMetadata = false;
        boolean hasStructureTree = false;
        List<String> readingOrderIssues = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            isTagged = isTaggedPdf(document);
            hasMetadata = hasRequiredMetadata(document);
            hasStructureTree = hasStructureTree(document);
            readingOrderIssues = validateReadingOrder(document);
        } catch (Exception e) {
            logger.error("Error during PDFBox validation", e);
            throw new IOException("PDFBox validation failed", e);
        }

        AccessibilityReport report = new AccessibilityReport(
                veraPdfViolations,
                isTagged,
                hasMetadata,
                hasStructureTree,
                readingOrderIssues
        );

        logger.info("Validation complete - Compliant: {}, Total issues: {}",
                report.isCompliant(), report.getTotalIssues());

        return report;
    }
}
