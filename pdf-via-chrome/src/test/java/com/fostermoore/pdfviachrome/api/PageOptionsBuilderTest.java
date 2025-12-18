package com.fostermoore.pdfviachrome.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PageOptions.Builder.
 */
class PageOptionsBuilderTest {

    @Test
    void testBuildWithDefaults() {
        PageOptions options = PageOptions.builder().build();

        assertThat(options).isNotNull();
        assertThat(options.getViewportWidth()).isEqualTo(1920);
        assertThat(options.getViewportHeight()).isEqualTo(1080);
        assertThat(options.getUserAgent()).isNull();
        assertThat(options.getPageLoadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(options.isJavascriptEnabled()).isTrue();
        assertThat(options.getDeviceScaleFactor()).isEqualTo(1.0);
    }

    @Test
    void testBuildWithAllOptions() {
        Duration timeout = Duration.ofSeconds(60);

        PageOptions options = PageOptions.builder()
            .viewportWidth(1024)
            .viewportHeight(768)
            .userAgent("Custom User Agent")
            .pageLoadTimeout(timeout)
            .javascriptEnabled(false)
            .deviceScaleFactor(2.0)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1024);
        assertThat(options.getViewportHeight()).isEqualTo(768);
        assertThat(options.getUserAgent()).isEqualTo("Custom User Agent");
        assertThat(options.getPageLoadTimeout()).isEqualTo(timeout);
        assertThat(options.isJavascriptEnabled()).isFalse();
        assertThat(options.getDeviceScaleFactor()).isEqualTo(2.0);
    }

    @Test
    void testBuilderMethodChaining() {
        PageOptions options = PageOptions.builder()
            .viewportWidth(800)
            .viewportHeight(600)
            .javascriptEnabled(false)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(800);
        assertThat(options.getViewportHeight()).isEqualTo(600);
        assertThat(options.isJavascriptEnabled()).isFalse();
    }

    @Test
    void testDefaults() {
        PageOptions options = PageOptions.defaults();

        assertThat(options).isNotNull();
        assertThat(options.getViewportWidth()).isEqualTo(1920);
        assertThat(options.getViewportHeight()).isEqualTo(1080);
        assertThat(options.isJavascriptEnabled()).isTrue();
    }

    @Test
    void testViewportMethod() {
        PageOptions options = PageOptions.builder()
            .viewport(1366, 768)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1366);
        assertThat(options.getViewportHeight()).isEqualTo(768);
    }

    // Validation Tests

    @Test
    void testViewportWidth_Valid() {
        assertThatCode(() -> PageOptions.builder().viewportWidth(1).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder().viewportWidth(1920).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder().viewportWidth(3840).build())
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -1920})
    void testViewportWidth_Invalid(int invalidWidth) {
        assertThatThrownBy(() -> PageOptions.builder().viewportWidth(invalidWidth).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Viewport width must be positive");
    }

    @Test
    void testViewportHeight_Valid() {
        assertThatCode(() -> PageOptions.builder().viewportHeight(1).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder().viewportHeight(1080).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder().viewportHeight(2160).build())
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -1080})
    void testViewportHeight_Invalid(int invalidHeight) {
        assertThatThrownBy(() -> PageOptions.builder().viewportHeight(invalidHeight).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Viewport height must be positive");
    }

    @Test
    void testViewport_Valid() {
        assertThatCode(() -> PageOptions.builder().viewport(1920, 1080).build())
            .doesNotThrowAnyException();

        PageOptions options = PageOptions.builder().viewport(800, 600).build();
        assertThat(options.getViewportWidth()).isEqualTo(800);
        assertThat(options.getViewportHeight()).isEqualTo(600);
    }

    @Test
    void testViewport_InvalidWidth() {
        assertThatThrownBy(() -> PageOptions.builder().viewport(0, 1080).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Viewport width must be positive");
    }

    @Test
    void testViewport_InvalidHeight() {
        assertThatThrownBy(() -> PageOptions.builder().viewport(1920, 0).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Viewport height must be positive");
    }

    @Test
    void testUserAgent_Null() {
        assertThatCode(() -> PageOptions.builder().userAgent(null).build())
            .doesNotThrowAnyException();

        PageOptions options = PageOptions.builder().userAgent(null).build();
        assertThat(options.getUserAgent()).isNull();
    }

    @Test
    void testUserAgent_Custom() {
        String customUserAgent = "Mozilla/5.0 (Custom) AppleWebKit/537.36";

        PageOptions options = PageOptions.builder()
            .userAgent(customUserAgent)
            .build();

        assertThat(options.getUserAgent()).isEqualTo(customUserAgent);
    }

    @Test
    void testPageLoadTimeout_Valid() {
        assertThatCode(() -> PageOptions.builder()
            .pageLoadTimeout(Duration.ofSeconds(10))
            .build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder()
            .pageLoadTimeout(Duration.ofMinutes(2))
            .build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder()
            .pageLoadTimeout(Duration.ZERO)
            .build())
            .doesNotThrowAnyException();

        PageOptions options = PageOptions.builder()
            .pageLoadTimeout(Duration.ofSeconds(45))
            .build();

        assertThat(options.getPageLoadTimeout()).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void testPageLoadTimeout_Null() {
        assertThatThrownBy(() -> PageOptions.builder()
            .pageLoadTimeout(null)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page load timeout cannot be null");
    }

    @Test
    void testPageLoadTimeout_Negative() {
        assertThatThrownBy(() -> PageOptions.builder()
            .pageLoadTimeout(Duration.ofSeconds(-10))
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page load timeout cannot be negative");
    }

    @Test
    void testJavascriptEnabled() {
        PageOptions enabledOptions = PageOptions.builder()
            .javascriptEnabled(true)
            .build();

        assertThat(enabledOptions.isJavascriptEnabled()).isTrue();

        PageOptions disabledOptions = PageOptions.builder()
            .javascriptEnabled(false)
            .build();

        assertThat(disabledOptions.isJavascriptEnabled()).isFalse();
    }

    @Test
    void testDeviceScaleFactor_Valid() {
        assertThatCode(() -> PageOptions.builder().deviceScaleFactor(0.5).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder().deviceScaleFactor(1.0).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder().deviceScaleFactor(2.0).build())
            .doesNotThrowAnyException();

        assertThatCode(() -> PageOptions.builder().deviceScaleFactor(3.5).build())
            .doesNotThrowAnyException();

        PageOptions options = PageOptions.builder().deviceScaleFactor(2.5).build();
        assertThat(options.getDeviceScaleFactor()).isEqualTo(2.5);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.5, -1.0, -2.0})
    void testDeviceScaleFactor_Invalid(double invalidFactor) {
        assertThatThrownBy(() -> PageOptions.builder().deviceScaleFactor(invalidFactor).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Device scale factor must be positive");
    }

    @Test
    void testImmutability() {
        PageOptions options = PageOptions.builder()
            .viewportWidth(1024)
            .viewportHeight(768)
            .javascriptEnabled(false)
            .build();

        // Verify getters return correct values
        assertThat(options.getViewportWidth()).isEqualTo(1024);
        assertThat(options.getViewportHeight()).isEqualTo(768);
        assertThat(options.isJavascriptEnabled()).isFalse();

        // Create another instance with different values
        PageOptions options2 = PageOptions.builder()
            .viewportWidth(800)
            .viewportHeight(600)
            .javascriptEnabled(true)
            .build();

        // Verify first instance is unchanged (immutable)
        assertThat(options.getViewportWidth()).isEqualTo(1024);
        assertThat(options.getViewportHeight()).isEqualTo(768);
        assertThat(options.isJavascriptEnabled()).isFalse();
        assertThat(options2.getViewportWidth()).isEqualTo(800);
        assertThat(options2.getViewportHeight()).isEqualTo(600);
        assertThat(options2.isJavascriptEnabled()).isTrue();
    }

    @Test
    void testBuilderReuse() {
        PageOptions.Builder builder = PageOptions.builder()
            .viewportWidth(1024)
            .javascriptEnabled(false);

        PageOptions options1 = builder.build();
        assertThat(options1.getViewportWidth()).isEqualTo(1024);
        assertThat(options1.isJavascriptEnabled()).isFalse();

        // Modify builder and create another instance
        builder.viewportWidth(800).javascriptEnabled(true);
        PageOptions options2 = builder.build();

        assertThat(options2.getViewportWidth()).isEqualTo(800);
        assertThat(options2.isJavascriptEnabled()).isTrue();

        // Original instance should remain unchanged
        assertThat(options1.getViewportWidth()).isEqualTo(1024);
        assertThat(options1.isJavascriptEnabled()).isFalse();
    }

    @Test
    void testCommonViewportSizes() {
        // Desktop
        PageOptions desktop = PageOptions.builder()
            .viewport(1920, 1080)
            .build();
        assertThat(desktop.getViewportWidth()).isEqualTo(1920);
        assertThat(desktop.getViewportHeight()).isEqualTo(1080);

        // Tablet
        PageOptions tablet = PageOptions.builder()
            .viewport(768, 1024)
            .build();
        assertThat(tablet.getViewportWidth()).isEqualTo(768);
        assertThat(tablet.getViewportHeight()).isEqualTo(1024);

        // Mobile
        PageOptions mobile = PageOptions.builder()
            .viewport(375, 667)
            .build();
        assertThat(mobile.getViewportWidth()).isEqualTo(375);
        assertThat(mobile.getViewportHeight()).isEqualTo(667);
    }

    @Test
    void testHighDpiConfiguration() {
        PageOptions retinaOptions = PageOptions.builder()
            .viewport(1920, 1080)
            .deviceScaleFactor(2.0)
            .build();

        assertThat(retinaOptions.getViewportWidth()).isEqualTo(1920);
        assertThat(retinaOptions.getViewportHeight()).isEqualTo(1080);
        assertThat(retinaOptions.getDeviceScaleFactor()).isEqualTo(2.0);
    }
}
