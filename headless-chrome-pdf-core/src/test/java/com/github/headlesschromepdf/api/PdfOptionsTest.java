package com.github.headlesschromepdf.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PdfOptions and its builder.
 */
class PdfOptionsTest {

    @Test
    void testBuilder_defaults() {
        PdfOptions options = PdfOptions.builder().build();

        assertThat(options.isLandscape()).isFalse();
        assertThat(options.isDisplayHeaderFooter()).isFalse();
        assertThat(options.isPrintBackground()).isFalse();
        assertThat(options.getScale()).isEqualTo(1.0);
        assertThat(options.getPaperWidth()).isEqualTo(8.5);
        assertThat(options.getPaperHeight()).isEqualTo(11.0);
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
    void testDefaults_createsSameAsBuilder() {
        PdfOptions defaultOptions = PdfOptions.defaults();
        PdfOptions builderOptions = PdfOptions.builder().build();

        assertThat(defaultOptions.isLandscape()).isEqualTo(builderOptions.isLandscape());
        assertThat(defaultOptions.getScale()).isEqualTo(builderOptions.getScale());
        assertThat(defaultOptions.getPaperWidth()).isEqualTo(builderOptions.getPaperWidth());
    }

    @Test
    void testBuilder_landscape() {
        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .build();

        assertThat(options.isLandscape()).isTrue();
    }

    @Test
    void testBuilder_displayHeaderFooter() {
        PdfOptions options = PdfOptions.builder()
            .displayHeaderFooter(true)
            .build();

        assertThat(options.isDisplayHeaderFooter()).isTrue();
    }

    @Test
    void testBuilder_printBackground() {
        PdfOptions options = PdfOptions.builder()
            .printBackground(true)
            .build();

        assertThat(options.isPrintBackground()).isTrue();
    }

    @Test
    void testBuilder_scale() {
        PdfOptions options = PdfOptions.builder()
            .scale(1.5)
            .build();

        assertThat(options.getScale()).isEqualTo(1.5);
    }

    @Test
    void testBuilder_scale_minimum() {
        PdfOptions options = PdfOptions.builder()
            .scale(0.1)
            .build();

        assertThat(options.getScale()).isEqualTo(0.1);
    }

    @Test
    void testBuilder_scale_maximum() {
        PdfOptions options = PdfOptions.builder()
            .scale(2.0)
            .build();

        assertThat(options.getScale()).isEqualTo(2.0);
    }

    @Test
    void testBuilder_scale_tooSmall_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().scale(0.05).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Scale must be between 0.1 and 2.0");
    }

    @Test
    void testBuilder_scale_tooLarge_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().scale(2.5).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Scale must be between 0.1 and 2.0");
    }

    @Test
    void testBuilder_paperWidth() {
        PdfOptions options = PdfOptions.builder()
            .paperWidth(10.0)
            .build();

        assertThat(options.getPaperWidth()).isEqualTo(10.0);
    }

    @Test
    void testBuilder_paperWidth_negative_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().paperWidth(-1.0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Paper width must be positive");
    }

    @Test
    void testBuilder_paperWidth_zero_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().paperWidth(0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Paper width must be positive");
    }

    @Test
    void testBuilder_paperHeight() {
        PdfOptions options = PdfOptions.builder()
            .paperHeight(14.0)
            .build();

        assertThat(options.getPaperHeight()).isEqualTo(14.0);
    }

    @Test
    void testBuilder_paperHeight_negative_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().paperHeight(-1.0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Paper height must be positive");
    }

    @Test
    void testBuilder_paperHeight_zero_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().paperHeight(0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Paper height must be positive");
    }

    @Test
    void testBuilder_paperSize_letter() {
        PdfOptions options = PdfOptions.builder()
            .paperSize(PdfOptions.PaperFormat.LETTER)
            .build();

        assertThat(options.getPaperWidth()).isEqualTo(8.5);
        assertThat(options.getPaperHeight()).isEqualTo(11.0);
    }

    @Test
    void testBuilder_paperSize_a4() {
        PdfOptions options = PdfOptions.builder()
            .paperSize(PdfOptions.PaperFormat.A4)
            .build();

        assertThat(options.getPaperWidth()).isEqualTo(8.27);
        assertThat(options.getPaperHeight()).isEqualTo(11.7);
    }

    @Test
    void testBuilder_paperSize_legal() {
        PdfOptions options = PdfOptions.builder()
            .paperSize(PdfOptions.PaperFormat.LEGAL)
            .build();

        assertThat(options.getPaperWidth()).isEqualTo(8.5);
        assertThat(options.getPaperHeight()).isEqualTo(14.0);
    }

    @Test
    void testBuilder_marginTop() {
        PdfOptions options = PdfOptions.builder()
            .marginTop(1.0)
            .build();

        assertThat(options.getMarginTop()).isEqualTo(1.0);
    }

    @Test
    void testBuilder_marginTop_negative_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().marginTop(-0.5).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Margin cannot be negative");
    }

    @Test
    void testBuilder_marginBottom() {
        PdfOptions options = PdfOptions.builder()
            .marginBottom(1.0)
            .build();

        assertThat(options.getMarginBottom()).isEqualTo(1.0);
    }

    @Test
    void testBuilder_marginBottom_negative_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().marginBottom(-0.5).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Margin cannot be negative");
    }

    @Test
    void testBuilder_marginLeft() {
        PdfOptions options = PdfOptions.builder()
            .marginLeft(1.0)
            .build();

        assertThat(options.getMarginLeft()).isEqualTo(1.0);
    }

    @Test
    void testBuilder_marginLeft_negative_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().marginLeft(-0.5).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Margin cannot be negative");
    }

    @Test
    void testBuilder_marginRight() {
        PdfOptions options = PdfOptions.builder()
            .marginRight(1.0)
            .build();

        assertThat(options.getMarginRight()).isEqualTo(1.0);
    }

    @Test
    void testBuilder_marginRight_negative_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().marginRight(-0.5).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Margin cannot be negative");
    }

    @Test
    void testBuilder_margins_setsAllMargins() {
        PdfOptions options = PdfOptions.builder()
            .margins(0.75)
            .build();

        assertThat(options.getMarginTop()).isEqualTo(0.75);
        assertThat(options.getMarginBottom()).isEqualTo(0.75);
        assertThat(options.getMarginLeft()).isEqualTo(0.75);
        assertThat(options.getMarginRight()).isEqualTo(0.75);
    }

    @Test
    void testBuilder_margins_negative_throwsException() {
        assertThatThrownBy(() ->
            PdfOptions.builder().margins(-0.5).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Margin cannot be negative");
    }

    @Test
    void testBuilder_pageRanges() {
        PdfOptions options = PdfOptions.builder()
            .pageRanges("1-5, 8, 11-13")
            .build();

        assertThat(options.getPageRanges()).isEqualTo("1-5, 8, 11-13");
    }

    @Test
    void testBuilder_pageRanges_null_becomesEmpty() {
        PdfOptions options = PdfOptions.builder()
            .pageRanges(null)
            .build();

        assertThat(options.getPageRanges()).isEmpty();
    }

    @Test
    void testBuilder_headerTemplate() {
        String template = "<div>Header</div>";
        PdfOptions options = PdfOptions.builder()
            .headerTemplate(template)
            .build();

        assertThat(options.getHeaderTemplate()).isEqualTo(template);
    }

    @Test
    void testBuilder_headerTemplate_null_becomesEmpty() {
        PdfOptions options = PdfOptions.builder()
            .headerTemplate(null)
            .build();

        assertThat(options.getHeaderTemplate()).isEmpty();
    }

    @Test
    void testBuilder_footerTemplate() {
        String template = "<div>Footer</div>";
        PdfOptions options = PdfOptions.builder()
            .footerTemplate(template)
            .build();

        assertThat(options.getFooterTemplate()).isEqualTo(template);
    }

    @Test
    void testBuilder_footerTemplate_null_becomesEmpty() {
        PdfOptions options = PdfOptions.builder()
            .footerTemplate(null)
            .build();

        assertThat(options.getFooterTemplate()).isEmpty();
    }

    @Test
    void testBuilder_preferCssPageSize() {
        PdfOptions options = PdfOptions.builder()
            .preferCssPageSize(true)
            .build();

        assertThat(options.isPreferCssPageSize()).isTrue();
    }

    @Test
    void testBuilder_methodChaining() {
        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .displayHeaderFooter(true)
            .printBackground(true)
            .scale(1.2)
            .paperSize(PdfOptions.PaperFormat.A4)
            .margins(0.5)
            .pageRanges("1-10")
            .headerTemplate("<div>Header</div>")
            .footerTemplate("<div>Footer</div>")
            .preferCssPageSize(false)
            .build();

        assertThat(options.isLandscape()).isTrue();
        assertThat(options.isDisplayHeaderFooter()).isTrue();
        assertThat(options.isPrintBackground()).isTrue();
        assertThat(options.getScale()).isEqualTo(1.2);
        assertThat(options.getPaperWidth()).isEqualTo(8.27);
        assertThat(options.getPaperHeight()).isEqualTo(11.7);
        assertThat(options.getMarginTop()).isEqualTo(0.5);
        assertThat(options.getPageRanges()).isEqualTo("1-10");
        assertThat(options.getHeaderTemplate()).isEqualTo("<div>Header</div>");
        assertThat(options.getFooterTemplate()).isEqualTo("<div>Footer</div>");
        assertThat(options.isPreferCssPageSize()).isFalse();
    }

    @Test
    void testPaperFormat_allFormatsHavePositiveDimensions() {
        for (PdfOptions.PaperFormat format : PdfOptions.PaperFormat.values()) {
            assertThat(format.getWidth()).isPositive();
            assertThat(format.getHeight()).isPositive();
        }
    }
}
