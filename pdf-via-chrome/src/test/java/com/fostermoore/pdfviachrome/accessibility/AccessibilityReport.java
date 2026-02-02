package com.fostermoore.pdfviachrome.accessibility;

import java.util.List;

/**
 * Comprehensive accessibility validation report combining Tier 1 (veraPDF) and Tier 2 (PDFBox) results.
 *
 * @param veraPdfViolations List of violations from veraPDF validation (PDF/UA, PDF/A)
 * @param isTagged Whether the PDF is marked as tagged for accessibility
 * @param hasMetadata Whether required metadata (title, language) is present
 * @param hasStructureTree Whether the PDF has a structure tree
 * @param readingOrderIssues List of reading order issues found (empty if valid)
 */
public record AccessibilityReport(
    List<AccessibilityViolation> veraPdfViolations,
    boolean isTagged,
    boolean hasMetadata,
    boolean hasStructureTree,
    List<String> readingOrderIssues
) {
    /**
     * Checks if the PDF is fully compliant with all accessibility requirements.
     *
     * @return true if all validation checks pass, false otherwise
     */
    public boolean isCompliant() {
        return veraPdfViolations.isEmpty() &&
               isTagged &&
               hasMetadata &&
               hasStructureTree &&
               readingOrderIssues.isEmpty();
    }

    /**
     * Gets the total number of issues found across all validation checks.
     *
     * @return Total count of violations and issues
     */
    public int getTotalIssues() {
        return veraPdfViolations.size() + readingOrderIssues.size() +
               (isTagged ? 0 : 1) +
               (hasMetadata ? 0 : 1) +
               (hasStructureTree ? 0 : 1);
    }
}
