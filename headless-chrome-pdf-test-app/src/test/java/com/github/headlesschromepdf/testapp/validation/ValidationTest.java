package com.github.headlesschromepdf.testapp.validation;

import com.github.headlesschromepdf.testapp.dto.HtmlRequest;
import com.github.headlesschromepdf.testapp.dto.PdfOptionsDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive validation tests for request DTOs.
 */
@DisplayName("Request DTO Validation Tests")
class ValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("HtmlRequest Validation")
    class HtmlRequestValidationTests {

        @Test
        @DisplayName("Should accept valid request with minimal fields")
        void shouldAcceptValidMinimalRequest() {
            HtmlRequest request = new HtmlRequest();
            request.setContent("<html><body>Test</body></html>");

            Set<ConstraintViolation<HtmlRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject null content")
        void shouldRejectNullContent() {
            HtmlRequest request = new HtmlRequest();
            request.setContent(null);

            Set<ConstraintViolation<HtmlRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("HTML content is required");
        }

        @Test
        @DisplayName("Should reject empty content")
        void shouldRejectEmptyContent() {
            HtmlRequest request = new HtmlRequest();
            request.setContent("");

            Set<ConstraintViolation<HtmlRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("HTML content is required");
        }

        @Test
        @DisplayName("Should reject blank content")
        void shouldRejectBlankContent() {
            HtmlRequest request = new HtmlRequest();
            request.setContent("   ");

            Set<ConstraintViolation<HtmlRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("HTML content is required");
        }

        @Test
        @DisplayName("Should reject content exceeding max size")
        void shouldRejectOversizedContent() {
            HtmlRequest request = new HtmlRequest();
            // Create string larger than 10 million characters
            StringBuilder largeContent = new StringBuilder();
            for (int i = 0; i < 10_000_001; i++) {
                largeContent.append("a");
            }
            request.setContent(largeContent.toString());

            Set<ConstraintViolation<HtmlRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("HTML content cannot exceed 10,000,000 characters");
        }

        @Test
        @DisplayName("Should accept content at max size limit")
        void shouldAcceptContentAtMaxSize() {
            HtmlRequest request = new HtmlRequest();
            // Create string exactly 10 million characters
            StringBuilder largeContent = new StringBuilder();
            for (int i = 0; i < 10_000_000; i++) {
                largeContent.append("a");
            }
            request.setContent(largeContent.toString());

            Set<ConstraintViolation<HtmlRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should cascade validation to nested PdfOptionsDto")
        void shouldCascadeValidationToOptions() {
            HtmlRequest request = new HtmlRequest();
            request.setContent("<html><body>Test</body></html>");

            PdfOptionsDto options = new PdfOptionsDto();
            options.setScale(5.0); // Invalid scale (max is 2.0)
            request.setOptions(options);

            Set<ConstraintViolation<HtmlRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Scale must not exceed 2.0");
        }
    }

    @Nested
    @DisplayName("PdfOptionsDto Validation")
    class PdfOptionsDtoValidationTests {

        @Test
        @DisplayName("Should accept valid options with all fields")
        void shouldAcceptValidCompleteOptions() {
            PdfOptionsDto options = new PdfOptionsDto();
            options.setLandscape(true);
            options.setPrintBackground(true);
            options.setScale(1.5);
            options.setPaperFormat("A4");
            options.setMargins("1cm");
            options.setPageRanges("1-5, 8, 11-13");

            Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept valid options with minimal fields")
        void shouldAcceptValidMinimalOptions() {
            PdfOptionsDto options = new PdfOptionsDto();

            Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

            assertThat(violations).isEmpty();
        }

        @Nested
        @DisplayName("Scale Validation")
        class ScaleValidationTests {

            @Test
            @DisplayName("Should reject scale below minimum (0.1)")
            void shouldRejectScaleBelowMinimum() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setScale(0.05);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("Scale must be at least 0.1");
            }

            @Test
            @DisplayName("Should reject scale above maximum (2.0)")
            void shouldRejectScaleAboveMaximum() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setScale(2.5);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("Scale must not exceed 2.0");
            }

            @Test
            @DisplayName("Should accept scale at minimum boundary (0.1)")
            void shouldAcceptScaleAtMinimum() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setScale(0.1);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).isEmpty();
            }

            @Test
            @DisplayName("Should accept scale at maximum boundary (2.0)")
            void shouldAcceptScaleAtMaximum() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setScale(2.0);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).isEmpty();
            }
        }

        @Nested
        @DisplayName("Paper Format Validation")
        class PaperFormatValidationTests {

            @Test
            @DisplayName("Should accept valid paper formats")
            void shouldAcceptValidPaperFormats() {
                String[] validFormats = {"LETTER", "LEGAL", "TABLOID", "LEDGER", "A0", "A1", "A2", "A3", "A4", "A5", "A6"};

                for (String format : validFormats) {
                    PdfOptionsDto options = new PdfOptionsDto();
                    options.setPaperFormat(format);

                    Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                    assertThat(violations).as("Format %s should be valid", format).isEmpty();
                }
            }

            @Test
            @DisplayName("Should reject invalid paper format")
            void shouldRejectInvalidPaperFormat() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setPaperFormat("INVALID");

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .contains("Paper format must be one of");
            }
        }

        @Nested
        @DisplayName("Paper Dimensions Validation")
        class PaperDimensionsValidationTests {

            @Test
            @DisplayName("Should accept positive paper width")
            void shouldAcceptPositivePaperWidth() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setPaperWidth(8.5);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).isEmpty();
            }

            @Test
            @DisplayName("Should reject zero or negative paper width")
            void shouldRejectNonPositivePaperWidth() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setPaperWidth(0.0);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("Paper width must be positive");
            }

            @Test
            @DisplayName("Should accept positive paper height")
            void shouldAcceptPositivePaperHeight() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setPaperHeight(11.0);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).isEmpty();
            }

            @Test
            @DisplayName("Should reject zero or negative paper height")
            void shouldRejectNonPositivePaperHeight() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setPaperHeight(-1.0);

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("Paper height must be positive");
            }
        }

        @Nested
        @DisplayName("Margin Validation")
        class MarginValidationTests {

            @Test
            @DisplayName("Should accept valid margin formats")
            void shouldAcceptValidMarginFormats() {
                String[] validMargins = {"1cm", "0.5in", "10px", "2mm", "1.5cm", "0.25in"};

                for (String margin : validMargins) {
                    PdfOptionsDto options = new PdfOptionsDto();
                    options.setMarginTop(margin);

                    Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                    assertThat(violations).as("Margin '%s' should be valid", margin).isEmpty();
                }
            }

            @Test
            @DisplayName("Should reject invalid margin format (missing unit)")
            void shouldRejectMarginWithoutUnit() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setMarginTop("10");

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .contains("Margin must be a valid size");
            }

            @Test
            @DisplayName("Should reject invalid margin format (invalid unit)")
            void shouldRejectMarginWithInvalidUnit() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setMarginBottom("10pt");

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .contains("Margin must be a valid size");
            }

            @Test
            @DisplayName("Should validate all margin fields")
            void shouldValidateAllMarginFields() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setMargins("1cm");
                options.setMarginTop("0.5in");
                options.setMarginBottom("10px");
                options.setMarginLeft("2mm");
                options.setMarginRight("1.5cm");

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).isEmpty();
            }
        }

        @Nested
        @DisplayName("Page Ranges Validation")
        class PageRangesValidationTests {

            @Test
            @DisplayName("Should accept valid page ranges")
            void shouldAcceptValidPageRanges() {
                String[] validRanges = {"1-5", "1-5, 8", "1-5, 8, 11-13", "1", "1,2,3", "1-10, 15-20, 25"};

                for (String range : validRanges) {
                    PdfOptionsDto options = new PdfOptionsDto();
                    options.setPageRanges(range);

                    Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                    assertThat(violations).as("Page range '%s' should be valid", range).isEmpty();
                }
            }

            @Test
            @DisplayName("Should reject invalid page ranges")
            void shouldRejectInvalidPageRanges() {
                PdfOptionsDto options = new PdfOptionsDto();
                options.setPageRanges("invalid");

                Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage())
                    .contains("Page ranges must be in format");
            }
        }

        @Test
        @DisplayName("Should accept multiple validation errors")
        void shouldAccumulateMultipleErrors() {
            PdfOptionsDto options = new PdfOptionsDto();
            options.setScale(5.0); // Invalid
            options.setPaperFormat("INVALID"); // Invalid
            options.setMarginTop("10"); // Invalid (missing unit)
            options.setPaperWidth(-1.0); // Invalid (negative)

            Set<ConstraintViolation<PdfOptionsDto>> violations = validator.validate(options);

            assertThat(violations).hasSize(4);
        }
    }
}
