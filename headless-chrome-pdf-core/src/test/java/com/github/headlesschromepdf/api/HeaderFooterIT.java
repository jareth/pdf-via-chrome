package com.github.headlesschromepdf.api;

import com.github.headlesschromepdf.util.ChromePathDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PDF header and footer functionality with actual Chrome instances.
 *
 * These tests require Chrome to be installed on the system.
 * They are disabled by default and can be enabled by setting the
 * CHROME_INTEGRATION_TESTS environment variable to "true".
 *
 * To run: mvn verify -DCHROME_INTEGRATION_TESTS=true
 */
@EnabledIfEnvironmentVariable(named = "CHROME_INTEGRATION_TESTS", matches = "true")
class HeaderFooterIT {

    @Test
    void testSimplePageNumbers() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent()
            .withFailMessage("Chrome not found on system. Please install Chrome to run integration tests.");

        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Page Numbers Test</title></head>
            <body>
                <h1>Page 1</h1>
                <div style="page-break-after: always;"></div>
                <h1>Page 2</h1>
                <div style="page-break-after: always;"></div>
                <h1>Page 3</h1>
            </body>
            </html>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .withTimeout(Duration.ofSeconds(30))
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .simplePageNumbers()
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

            // Verify displayHeaderFooter was enabled
            assertThat(options.isDisplayHeaderFooter()).isTrue();
            assertThat(options.getFooterTemplate()).contains("pageNumber");
        }
    }

    @Test
    void testHeaderWithTitle() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Test Document Title</title></head>
            <body>
                <h1>Header Test</h1>
                <p>This document should have a header with the title.</p>
            </body>
            </html>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .headerWithTitle()
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

            assertThat(options.isDisplayHeaderFooter()).isTrue();
            assertThat(options.getHeaderTemplate()).contains("title");
        }
    }

    @Test
    void testFooterWithDate() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = "<html><body><h1>Footer with Date Test</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .footerWithDate()
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

            assertThat(options.isDisplayHeaderFooter()).isTrue();
            assertThat(options.getFooterTemplate()).contains("date");
        }
    }

    @Test
    void testStandardHeaderFooter() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Standard Header/Footer Document</title></head>
            <body>
                <h1>Page 1 Content</h1>
                <p>This is some content on the first page.</p>
                <div style="page-break-after: always;"></div>
                <h1>Page 2 Content</h1>
                <p>This is some content on the second page.</p>
            </body>
            </html>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .standardHeaderFooter()
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

            assertThat(options.isDisplayHeaderFooter()).isTrue();
            assertThat(options.getHeaderTemplate()).contains("title");
            assertThat(options.getFooterTemplate()).contains("pageNumber");
        }
    }

    @Test
    void testCustomHeaderTemplate() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = "<html><body><h1>Custom Header Test</h1></body></html>";

        String customHeader = """
            <div style="font-size: 12px; text-align: right; width: 100%; padding-right: 1cm;">
                <span class="title"></span> - Page <span class="pageNumber"></span>
            </div>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .displayHeaderFooter(true)
                .headerTemplate(customHeader)
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testCustomFooterTemplate() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Custom Footer Test</title></head>
            <body>
                <h1>Page 1</h1>
                <div style="page-break-after: always;"></div>
                <h1>Page 2</h1>
            </body>
            </html>
            """;

        String customFooter = """
            <div style="font-size: 10px; width: 100%; display: flex; justify-content: space-between; padding: 0 1cm;">
                <span><span class="date"></span></span>
                <span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span>
                <span><span class="url"></span></span>
            </div>
            """;

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .displayHeaderFooter(true)
                .footerTemplate(customFooter)
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testHeaderAndFooterTogether() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Combined Header and Footer Test</title></head>
            <body>
                <h1>Testing Both Header and Footer</h1>
                <p>This document has both a custom header and footer.</p>
                <div style="page-break-after: always;"></div>
                <h1>Second Page</h1>
                <p>More content here.</p>
            </body>
            </html>
            """;

        String header = "<div style=\"font-size: 10px; text-align: center; width: 100%;\"><span class=\"title\"></span></div>";
        String footer = "<div style=\"font-size: 10px; text-align: center; width: 100%;\">Page <span class=\"pageNumber\"></span></div>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            PdfOptions options = PdfOptions.builder()
                .displayHeaderFooter(true)
                .headerTemplate(header)
                .footerTemplate(footer)
                .printBackground(true)
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        }
    }

    @Test
    void testConvenienceMethodOverriddenByCustomTemplate() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        String html = "<html><body><h1>Override Test</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath.get())
            .build()) {

            // First use convenience method, then override with custom template
            PdfOptions options = PdfOptions.builder()
                .simplePageNumbers()
                .footerTemplate("<div style=\"text-align: center; width: 100%;\">Custom Footer</div>")
                .build();

            byte[] pdf = generator.fromHtml(html)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

            // Verify custom template was used
            assertThat(options.getFooterTemplate()).contains("Custom Footer");
            assertThat(options.getFooterTemplate()).doesNotContain("pageNumber");
        }
    }
}
