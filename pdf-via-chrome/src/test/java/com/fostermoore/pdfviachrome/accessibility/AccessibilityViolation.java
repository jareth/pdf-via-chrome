package com.fostermoore.pdfviachrome.accessibility;

/**
 * Represents a single accessibility violation found during PDF validation.
 * Used to capture violations from veraPDF validation (Tier 1).
 *
 * @param flavour PDF standard flavour (e.g., "PDF/UA-1", "PDF/A-2B")
 * @param rule Rule identifier that was violated (e.g., "6.1-1: Document structure")
 * @param message Human-readable description of the violation
 * @param severity Severity level ("error", "warning")
 */
public record AccessibilityViolation(
    String flavour,
    String rule,
    String message,
    String severity
) {
}
