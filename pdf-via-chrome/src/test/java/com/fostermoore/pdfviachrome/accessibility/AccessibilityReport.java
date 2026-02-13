package com.fostermoore.pdfviachrome.accessibility;

import java.util.List;

/**
 * Comprehensive accessibility validation report combining Tier 1 (veraPDF) and Tier 2 (PDFBox) results.
 *
 * @param veraPdfResult Tier 1 result including violations and whether validation ran
 * @param isTagged Whether the PDF is marked as tagged for accessibility
 * @param hasMetadata Whether required metadata (title, language) is present
 * @param hasStructureTree Whether the PDF has a structure tree
 * @param readingOrderIssues List of reading order issues found (empty if valid)
 */
public record AccessibilityReport(
    VeraPdfResult veraPdfResult,
    boolean isTagged,
    boolean hasMetadata,
    boolean hasStructureTree,
    List<String> readingOrderIssues
) {
    /**
     * Checks if the PDF passes all accessibility checks that were actually executed.
     * <p>
     * A PDF is considered compliant when:
     * <ul>
     *   <li>Tier 1 (veraPDF): Either validation passed with no violations, or was skipped
     *       because the PDF is not marked as PDF/A (use {@link #tier1Ran()} to distinguish)</li>
     *   <li>Tier 2 (PDFBox): Tagged, has metadata, has structure tree, no reading order issues</li>
     * </ul>
     *
     * @return true if all executed validation checks pass
     */
    public boolean isCompliant() {
        return veraPdfResult.violations().isEmpty() &&
               isTagged &&
               hasMetadata &&
               hasStructureTree &&
               readingOrderIssues.isEmpty();
    }

    /**
     * Whether Tier 1 (veraPDF PDF/A) validation actually ran.
     * When false, the PDF was not marked as PDF/A so standard validation was skipped.
     *
     * @return true if PDF/A validation was executed
     */
    public boolean tier1Ran() {
        return veraPdfResult.validationRan();
    }

    /**
     * Gets the list of veraPDF violations.
     *
     * @return List of Tier 1 violations (empty if compliant or skipped)
     */
    public List<AccessibilityViolation> veraPdfViolations() {
        return veraPdfResult.violations();
    }

    /**
     * Gets the total number of issues found across all validation checks.
     *
     * @return Total count of violations and issues
     */
    public int getTotalIssues() {
        return veraPdfResult.violations().size() + readingOrderIssues.size() +
               (isTagged ? 0 : 1) +
               (hasMetadata ? 0 : 1) +
               (hasStructureTree ? 0 : 1);
    }
}
