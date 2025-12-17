package com.github.headlesschromepdf.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PdfOptions.Builder.
 */
class PdfOptionsBuilderTest {

    @Test
    void testBuildWithDefaults() {
        PdfOptions options = PdfOptions.builder().build();

        assertThat(options).isNotNull();
        assertThat(options.isLandscape()).isFalse();
        assertThat(options.isDisplayHeaderFooter()).isFalse();
        assertThat(options.isPrintBackground()).isFalse();
        assertThat(options.getScale()).isEqualTo(1.0);
        assertThat(options.getPaperWidth()).isEqualTo(8.5); // Letter width
        assertThat(options.getPaperHeight()).isEqualTo(11.0); // Letter height
        assertThat(options.getMarginTop()).isEqualTo(0.4);
        assertThat(options.getMarginBottom()).isEqualTo(0.4);
        assertThat(options.getMarginLeft()).isEqualTo(0.4);
        assertThat(options.getMarginRight()).isEqualTo(0.4);
        assertThat(options.getPageRanges()).isEmpty();
        assertThat(options.getHeaderTemplate()).isEmpty();
        assertThat(options.getFooterTemplate()).isEmpty();
        assertThat(options.isPreferCssPageSize()).isFalse();
    }

    @Test
    void testBuildWithAllOptions() {
        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .displayHeaderFooter(true)
            .printBackground(true)
            .scale(1.5)
            .paperWidth(10.0)
            .paperHeight(15.0)
            .marginTop(1.0)
            .marginBottom(1.0)
            .marginLeft(0.5)
            .marginRight(0.5)
            .pageRanges("1-5, 8, 11-13")
            .headerTemplate("<div>Header</div>")
            .footerTemplate("<div>Footer</div>")
            .preferCssPageSize(true)
            .build();

        assertThat(options.isLandscape()).isTrue();
        assertThat(options.isDisplayHeaderFooter()).isTrue();
        assertThat(options.isPrintBackground()).isTrue();
        assertThat(options.getScale()).isEqualTo(1.5);
        assertThat(options.getPaperWidth()).isEqualTo(10.0);
        assertThat(options.getPaperHeight()).isEqualTo(15.0);
        assertThat(options.getMarginTop()).isEqualTo(1.0);
        assertThat(options.getMarginBottom()).isEqualTo(1.0);
        assertThat(options.getMarginLeft()).isEqualTo(0.5);
        assertThat(options.getMarginRight()).isEqualTo(0.5);
        assertThat(options.getPageRanges()).isEqualTo("1-5, 8, 11-13");
        assertThat(options.getHeaderTemplate()).isEqualTo("<div>Header</div>");
        assertThat(options.getFooterTemplate()).isEqualTo("<div>Footer</div>");
        assertThat(options.isPreferCssPageSize()).isTrue();
    }

    @Test
    void testBuilderMethodChaining() {
        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .printBackground(true)
            .scale(0.8)
            .build();

        assertThat(options.isLandscape()).isTrue();
        assertThat(options.isPrintBackground()).isTrue();
        assertThat(options.getScale()).isEqualTo(0.8);
    }

    @Test
    void testDefaults() {
        PdfOptions options = PdfOptions.defaults();

        assertThat(options).isNotNull();
        assertThat(options.isLandscape()).isFalse();
        assertThat(options.getScale()).isEqualTo(1.0);
    }

    @Test
    void testPaperSizeFormats() {
        PdfOptions letter = PdfOptions.builder()
            .paperSize(PdfOptions.PaperFormat.LETTER)
            .build();
        assertThat(letter.getPaperWidth()).isEqualTo(8.5);
        assertThat(letter.getPaperHeight()).isEqualTo(11.0);

        PdfOptions a4 = PdfOptions.builder()
            .paperSize(PdfOptions.PaperFormat.A4)
            .build();
        assertThat(a4.getPaperWidth()).isEqualTo(8.27);
        assertThat(a4.getPaperHeight()).isEqualTo(11.7);

        PdfOptions legal = PdfOptions.builder()
            .paperSize(PdfOptions.PaperFormat.LEGAL)
            .build();
        assertThat(legal.getPaperWidth()).isEqualTo(8.5);
        assertThat(legal.getPaperHeight()).isEqualTo(14.0);
    }

    // Validation Tests

    @Test
    void testScale_Valid() {
        assertThatCode(() -> PdfOptions.builder().scale(0.1).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PdfOptions.builder().scale(1.0).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PdfOptions.builder().scale(2.0).build())
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.05, 2.1, 3.0, -1.0, 10.0})
    void testScale_Invalid(double invalidScale) {
        assertThatThrownBy(() -> PdfOptions.builder().scale(invalidScale).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Scale must be between 0.1 and 2.0");
    }

    @Test
    void testPaperWidth_Invalid() {
        assertThatThrownBy(() -> PdfOptions.builder().paperWidth(0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Paper width must be positive");

        assertThatThrownBy(() -> PdfOptions.builder().paperWidth(-1.0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Paper width must be positive");
    }

    @Test
    void testPaperHeight_Invalid() {
        assertThatThrownBy(() -> PdfOptions.builder().paperHeight(0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Paper height must be positive");

        assertThatThrownBy(() -> PdfOptions.builder().paperHeight(-1.0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Paper height must be positive");
    }

    @Test
    void testMargin_Negative() {
        assertThatThrownBy(() -> PdfOptions.builder().marginTop(-0.5).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Margin cannot be negative");

        assertThatThrownBy(() -> PdfOptions.builder().marginBottom(-0.5).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Margin cannot be negative");

        assertThatThrownBy(() -> PdfOptions.builder().marginLeft(-0.5).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Margin cannot be negative");

        assertThatThrownBy(() -> PdfOptions.builder().marginRight(-0.5).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Margin cannot be negative");
    }

    @Test
    void testMarginWithUnit_ValidFormats() {
        PdfOptions options1 = PdfOptions.builder()
            .marginTop("1cm")
            .build();
        assertThat(options1.getMarginTop()).isCloseTo(0.3937, within(0.001)); // 1cm = 0.3937in

        PdfOptions options2 = PdfOptions.builder()
            .marginTop("1in")
            .build();
        assertThat(options2.getMarginTop()).isEqualTo(1.0);

        PdfOptions options3 = PdfOptions.builder()
            .marginTop("96px")
            .build();
        assertThat(options3.getMarginTop()).isEqualTo(1.0); // 96px = 1in

        PdfOptions options4 = PdfOptions.builder()
            .marginTop("2.54cm")
            .build();
        assertThat(options4.getMarginTop()).isCloseTo(1.0, within(0.001)); // 2.54cm = 1in
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "cm", "1x", "1.5", "abc", "-1cm"})
    void testMarginWithUnit_InvalidFormat(String invalidMargin) {
        assertThatThrownBy(() -> PdfOptions.builder().marginTop(invalidMargin).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testMarginWithUnit_ValidWithSpace() {
        // Whitespace before unit is allowed by regex
        PdfOptions options = PdfOptions.builder()
            .marginTop("1 cm")
            .build();
        assertThat(options.getMarginTop()).isCloseTo(0.3937, within(0.001)); // 1cm = 0.3937in
    }

    @Test
    void testMarginWithUnit_Null() {
        assertThatThrownBy(() -> PdfOptions.builder().marginTop((String) null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Margin string cannot be null");
    }

    @Test
    void testMarginsMethod() {
        PdfOptions options = PdfOptions.builder()
            .margins(1.5)
            .build();

        assertThat(options.getMarginTop()).isEqualTo(1.5);
        assertThat(options.getMarginBottom()).isEqualTo(1.5);
        assertThat(options.getMarginLeft()).isEqualTo(1.5);
        assertThat(options.getMarginRight()).isEqualTo(1.5);
    }

    @Test
    void testMarginsMethodWithUnit() {
        PdfOptions options = PdfOptions.builder()
            .margins("2cm")
            .build();

        double expectedInches = 2.0 / 2.54; // 2cm in inches
        assertThat(options.getMarginTop()).isCloseTo(expectedInches, within(0.001));
        assertThat(options.getMarginBottom()).isCloseTo(expectedInches, within(0.001));
        assertThat(options.getMarginLeft()).isCloseTo(expectedInches, within(0.001));
        assertThat(options.getMarginRight()).isCloseTo(expectedInches, within(0.001));
    }

    @Test
    void testPageRanges_Valid() {
        assertThatCode(() -> PdfOptions.builder().pageRanges("1-5").build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PdfOptions.builder().pageRanges("1,2,3").build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PdfOptions.builder().pageRanges("1-5, 8, 11-13").build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PdfOptions.builder().pageRanges("").build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PdfOptions.builder().pageRanges("1").build())
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0-5", "1-0", "5-3", "abc", "1,2,abc", "1--5", "1-", "-5"})
    void testPageRanges_Invalid(String invalidRanges) {
        assertThatThrownBy(() -> PdfOptions.builder().pageRanges(invalidRanges).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPageRanges_Null() {
        assertThatCode(() -> PdfOptions.builder().pageRanges(null).build())
            .doesNotThrowAnyException();

        PdfOptions options = PdfOptions.builder().pageRanges(null).build();
        assertThat(options.getPageRanges()).isEmpty();
    }

    @Test
    void testHeaderFooterTemplates_Null() {
        PdfOptions options = PdfOptions.builder()
            .headerTemplate(null)
            .footerTemplate(null)
            .build();

        assertThat(options.getHeaderTemplate()).isEmpty();
        assertThat(options.getFooterTemplate()).isEmpty();
    }

    @Test
    void testImmutability() {
        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .scale(1.5)
            .build();

        // Verify getters return correct values
        assertThat(options.isLandscape()).isTrue();
        assertThat(options.getScale()).isEqualTo(1.5);

        // Create another instance with different values
        PdfOptions options2 = PdfOptions.builder()
            .landscape(false)
            .scale(0.8)
            .build();

        // Verify first instance is unchanged (immutable)
        assertThat(options.isLandscape()).isTrue();
        assertThat(options.getScale()).isEqualTo(1.5);
        assertThat(options2.isLandscape()).isFalse();
        assertThat(options2.getScale()).isEqualTo(0.8);
    }

    @Test
    void testBuilderReuse() {
        PdfOptions.Builder builder = PdfOptions.builder()
            .landscape(true)
            .scale(1.5);

        PdfOptions options1 = builder.build();
        assertThat(options1.isLandscape()).isTrue();
        assertThat(options1.getScale()).isEqualTo(1.5);

        // Modify builder and create another instance
        builder.landscape(false).scale(0.8);
        PdfOptions options2 = builder.build();

        assertThat(options2.isLandscape()).isFalse();
        assertThat(options2.getScale()).isEqualTo(0.8);

        // Original instance should remain unchanged
        assertThat(options1.isLandscape()).isTrue();
        assertThat(options1.getScale()).isEqualTo(1.5);
    }
}
