package com.fostermoore.pdfviachrome.accessibility;

import java.util.List;

/**
 * Result of veraPDF (Tier 1) validation.
 *
 * @param violations List of violations found (empty if compliant or if validation was skipped)
 * @param validationRan Whether PDF/A validation actually executed (false when no PDF/A flavour detected)
 */
public record VeraPdfResult(
    List<AccessibilityViolation> violations,
    boolean validationRan
) {
}
