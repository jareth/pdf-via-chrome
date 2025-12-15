package com.github.headlesschromepdf.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PageOptions and its builder.
 */
class PageOptionsTest {

    @Test
    void testBuilder_defaults() {
        PageOptions options = PageOptions.builder().build();

        assertThat(options.getViewportWidth()).isEqualTo(1920);
        assertThat(options.getViewportHeight()).isEqualTo(1080);
        assertThat(options.getUserAgent()).isNull();
        assertThat(options.getPageLoadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(options.isJavascriptEnabled()).isTrue();
        assertThat(options.getDeviceScaleFactor()).isEqualTo(1.0);
    }

    @Test
    void testDefaults_createsSameAsBuilder() {
        PageOptions defaultOptions = PageOptions.defaults();
        PageOptions builderOptions = PageOptions.builder().build();

        assertThat(defaultOptions.getViewportWidth()).isEqualTo(builderOptions.getViewportWidth());
        assertThat(defaultOptions.getViewportHeight()).isEqualTo(builderOptions.getViewportHeight());
        assertThat(defaultOptions.getUserAgent()).isEqualTo(builderOptions.getUserAgent());
        assertThat(defaultOptions.getPageLoadTimeout()).isEqualTo(builderOptions.getPageLoadTimeout());
        assertThat(defaultOptions.isJavascriptEnabled()).isEqualTo(builderOptions.isJavascriptEnabled());
        assertThat(defaultOptions.getDeviceScaleFactor()).isEqualTo(builderOptions.getDeviceScaleFactor());
    }

    @Test
    void testBuilder_viewportWidth() {
        PageOptions options = PageOptions.builder()
            .viewportWidth(1280)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1280);
    }

    @Test
    void testBuilder_viewportWidth_zero_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().viewportWidth(0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Viewport width must be positive");
    }

    @Test
    void testBuilder_viewportWidth_negative_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().viewportWidth(-100).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Viewport width must be positive");
    }

    @Test
    void testBuilder_viewportHeight() {
        PageOptions options = PageOptions.builder()
            .viewportHeight(720)
            .build();

        assertThat(options.getViewportHeight()).isEqualTo(720);
    }

    @Test
    void testBuilder_viewportHeight_zero_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().viewportHeight(0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Viewport height must be positive");
    }

    @Test
    void testBuilder_viewportHeight_negative_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().viewportHeight(-100).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Viewport height must be positive");
    }

    @Test
    void testBuilder_viewport_setsWidthAndHeight() {
        PageOptions options = PageOptions.builder()
            .viewport(1366, 768)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1366);
        assertThat(options.getViewportHeight()).isEqualTo(768);
    }

    @Test
    void testBuilder_viewport_invalidWidth_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().viewport(-1, 768).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Viewport width must be positive");
    }

    @Test
    void testBuilder_viewport_invalidHeight_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().viewport(1366, 0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Viewport height must be positive");
    }

    @Test
    void testBuilder_userAgent() {
        String userAgent = "Mozilla/5.0 (Custom) AppleWebKit/537.36";
        PageOptions options = PageOptions.builder()
            .userAgent(userAgent)
            .build();

        assertThat(options.getUserAgent()).isEqualTo(userAgent);
    }

    @Test
    void testBuilder_userAgent_null_allowed() {
        PageOptions options = PageOptions.builder()
            .userAgent(null)
            .build();

        assertThat(options.getUserAgent()).isNull();
    }

    @Test
    void testBuilder_userAgent_empty_allowed() {
        PageOptions options = PageOptions.builder()
            .userAgent("")
            .build();

        assertThat(options.getUserAgent()).isEmpty();
    }

    @Test
    void testBuilder_pageLoadTimeout() {
        Duration timeout = Duration.ofSeconds(60);
        PageOptions options = PageOptions.builder()
            .pageLoadTimeout(timeout)
            .build();

        assertThat(options.getPageLoadTimeout()).isEqualTo(timeout);
    }

    @Test
    void testBuilder_pageLoadTimeout_zero_allowed() {
        Duration timeout = Duration.ZERO;
        PageOptions options = PageOptions.builder()
            .pageLoadTimeout(timeout)
            .build();

        assertThat(options.getPageLoadTimeout()).isEqualTo(timeout);
    }

    @Test
    void testBuilder_pageLoadTimeout_null_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().pageLoadTimeout(null).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Page load timeout cannot be null");
    }

    @Test
    void testBuilder_pageLoadTimeout_negative_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().pageLoadTimeout(Duration.ofSeconds(-1)).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Page load timeout cannot be negative");
    }

    @Test
    void testBuilder_pageLoadTimeout_largeValue() {
        Duration timeout = Duration.ofMinutes(10);
        PageOptions options = PageOptions.builder()
            .pageLoadTimeout(timeout)
            .build();

        assertThat(options.getPageLoadTimeout()).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void testBuilder_javascriptEnabled_true() {
        PageOptions options = PageOptions.builder()
            .javascriptEnabled(true)
            .build();

        assertThat(options.isJavascriptEnabled()).isTrue();
    }

    @Test
    void testBuilder_javascriptEnabled_false() {
        PageOptions options = PageOptions.builder()
            .javascriptEnabled(false)
            .build();

        assertThat(options.isJavascriptEnabled()).isFalse();
    }

    @Test
    void testBuilder_deviceScaleFactor() {
        PageOptions options = PageOptions.builder()
            .deviceScaleFactor(2.0)
            .build();

        assertThat(options.getDeviceScaleFactor()).isEqualTo(2.0);
    }

    @Test
    void testBuilder_deviceScaleFactor_fractional() {
        PageOptions options = PageOptions.builder()
            .deviceScaleFactor(1.5)
            .build();

        assertThat(options.getDeviceScaleFactor()).isEqualTo(1.5);
    }

    @Test
    void testBuilder_deviceScaleFactor_zero_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().deviceScaleFactor(0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Device scale factor must be positive");
    }

    @Test
    void testBuilder_deviceScaleFactor_negative_throwsException() {
        assertThatThrownBy(() ->
            PageOptions.builder().deviceScaleFactor(-1.0).build()
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Device scale factor must be positive");
    }

    @Test
    void testBuilder_deviceScaleFactor_largeValue() {
        PageOptions options = PageOptions.builder()
            .deviceScaleFactor(4.0)
            .build();

        assertThat(options.getDeviceScaleFactor()).isEqualTo(4.0);
    }

    @Test
    void testBuilder_methodChaining() {
        String userAgent = "Mozilla/5.0 (Test)";
        Duration timeout = Duration.ofMinutes(2);

        PageOptions options = PageOptions.builder()
            .viewportWidth(1024)
            .viewportHeight(768)
            .userAgent(userAgent)
            .pageLoadTimeout(timeout)
            .javascriptEnabled(false)
            .deviceScaleFactor(2.0)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1024);
        assertThat(options.getViewportHeight()).isEqualTo(768);
        assertThat(options.getUserAgent()).isEqualTo(userAgent);
        assertThat(options.getPageLoadTimeout()).isEqualTo(timeout);
        assertThat(options.isJavascriptEnabled()).isFalse();
        assertThat(options.getDeviceScaleFactor()).isEqualTo(2.0);
    }

    @Test
    void testBuilder_methodChaining_withViewportMethod() {
        String userAgent = "Custom User Agent";

        PageOptions options = PageOptions.builder()
            .viewport(1440, 900)
            .userAgent(userAgent)
            .pageLoadTimeout(Duration.ofSeconds(45))
            .javascriptEnabled(true)
            .deviceScaleFactor(1.25)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1440);
        assertThat(options.getViewportHeight()).isEqualTo(900);
        assertThat(options.getUserAgent()).isEqualTo(userAgent);
        assertThat(options.getPageLoadTimeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(options.isJavascriptEnabled()).isTrue();
        assertThat(options.getDeviceScaleFactor()).isEqualTo(1.25);
    }

    @Test
    void testBuilder_overridingValues() {
        PageOptions options = PageOptions.builder()
            .viewportWidth(800)
            .viewportWidth(1920)  // Override
            .javascriptEnabled(false)
            .javascriptEnabled(true)  // Override
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1920);
        assertThat(options.isJavascriptEnabled()).isTrue();
    }

    @Test
    void testBuilder_commonMobileViewport() {
        PageOptions options = PageOptions.builder()
            .viewport(375, 667)  // iPhone SE
            .deviceScaleFactor(2.0)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(375);
        assertThat(options.getViewportHeight()).isEqualTo(667);
        assertThat(options.getDeviceScaleFactor()).isEqualTo(2.0);
    }

    @Test
    void testBuilder_commonTabletViewport() {
        PageOptions options = PageOptions.builder()
            .viewport(768, 1024)  // iPad
            .deviceScaleFactor(2.0)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(768);
        assertThat(options.getViewportHeight()).isEqualTo(1024);
        assertThat(options.getDeviceScaleFactor()).isEqualTo(2.0);
    }

    @Test
    void testBuilder_commonDesktopViewport() {
        PageOptions options = PageOptions.builder()
            .viewport(1920, 1080)  // Full HD
            .deviceScaleFactor(1.0)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(1920);
        assertThat(options.getViewportHeight()).isEqualTo(1080);
        assertThat(options.getDeviceScaleFactor()).isEqualTo(1.0);
    }

    @Test
    void testImmutability_buildingMultipleTimes() {
        PageOptions.Builder builder = PageOptions.builder()
            .viewportWidth(1024)
            .viewportHeight(768);

        PageOptions options1 = builder.build();

        builder.viewportWidth(1920).viewportHeight(1080);
        PageOptions options2 = builder.build();

        // First options should remain unchanged
        assertThat(options1.getViewportWidth()).isEqualTo(1024);
        assertThat(options1.getViewportHeight()).isEqualTo(768);

        // Second options should have new values
        assertThat(options2.getViewportWidth()).isEqualTo(1920);
        assertThat(options2.getViewportHeight()).isEqualTo(1080);
    }

    @Test
    void testBuilder_extremelyLargeViewport() {
        PageOptions options = PageOptions.builder()
            .viewport(7680, 4320)  // 8K resolution
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(7680);
        assertThat(options.getViewportHeight()).isEqualTo(4320);
    }

    @Test
    void testBuilder_smallViewport() {
        PageOptions options = PageOptions.builder()
            .viewport(320, 240)
            .build();

        assertThat(options.getViewportWidth()).isEqualTo(320);
        assertThat(options.getViewportHeight()).isEqualTo(240);
    }

    @Test
    void testBuilder_veryShortTimeout() {
        PageOptions options = PageOptions.builder()
            .pageLoadTimeout(Duration.ofMillis(100))
            .build();

        assertThat(options.getPageLoadTimeout()).isEqualTo(Duration.ofMillis(100));
    }

    @Test
    void testBuilder_veryLongTimeout() {
        PageOptions options = PageOptions.builder()
            .pageLoadTimeout(Duration.ofHours(1))
            .build();

        assertThat(options.getPageLoadTimeout()).isEqualTo(Duration.ofHours(1));
    }
}
