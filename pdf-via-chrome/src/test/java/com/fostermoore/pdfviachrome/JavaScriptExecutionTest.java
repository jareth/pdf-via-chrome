package com.fostermoore.pdfviachrome;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for JavaScript execution functionality in PDF generation.
 * These tests verify that JavaScript can be executed before PDF generation
 * to manipulate the page content, trigger actions, or wait for dynamic content.
 */
class JavaScriptExecutionTest {

    @TempDir
    Path tempDir;

    /**
     * Tests basic JavaScript execution that modifies the DOM.
     * The JavaScript should execute successfully and the changes should be visible in the PDF.
     */
    @Test
    void testBasicJavaScriptExecution() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Page</title>
            </head>
            <body>
                <h1 id="title">Original Title</h1>
                <p id="content">Original content</p>
            </body>
            </html>
            """;

        String jsCode = """
            document.getElementById('title').textContent = 'Modified Title';
            document.getElementById('content').textContent = 'Modified content';
            """;

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScript(jsCode)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests JavaScript execution that removes elements from the DOM.
     * This is a common use case for removing ads or unwanted elements before PDF generation.
     */
    @Test
    void testJavaScriptRemovesElements() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Page with Ads</title>
            </head>
            <body>
                <h1>Main Content</h1>
                <div class="ad">Advertisement Banner</div>
                <p>Important content</p>
                <div class="ad">Another Ad</div>
            </body>
            </html>
            """;

        String jsCode = """
            document.querySelectorAll('.ad').forEach(ad => ad.remove());
            """;

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScript(jsCode)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests JavaScript execution from a file.
     * The JavaScript file should be read and executed successfully.
     */
    @Test
    void testJavaScriptExecutionFromFile() throws IOException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Page</title>
            </head>
            <body>
                <div id="target">Original text</div>
            </body>
            </html>
            """;

        // Create a temporary JavaScript file
        Path jsFile = tempDir.resolve("modify.js");
        String jsCode = "document.getElementById('target').textContent = 'Modified from file';";
        Files.writeString(jsFile, jsCode);

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScriptFromFile(jsFile)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests that JavaScript execution throws an exception when the file doesn't exist.
     */
    @Test
    void testJavaScriptFromNonExistentFile() {
        String html = "<html><body><h1>Test</h1></body></html>";
        Path nonExistentFile = tempDir.resolve("nonexistent.js");

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            assertThatThrownBy(() ->
                generator.fromHtml(html)
                    .executeJavaScriptFromFile(nonExistentFile)
                    .generate()
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JavaScript file does not exist");
        }
    }

    /**
     * Tests that JavaScript execution validates the JavaScript code is not null.
     */
    @Test
    void testJavaScriptExecutionWithNullCode() {
        String html = "<html><body><h1>Test</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            assertThatThrownBy(() ->
                generator.fromHtml(html)
                    .executeJavaScript(null)
                    .generate()
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JavaScript code cannot be null or empty");
        }
    }

    /**
     * Tests that JavaScript execution validates the JavaScript code is not empty.
     */
    @Test
    void testJavaScriptExecutionWithEmptyCode() {
        String html = "<html><body><h1>Test</h1></body></html>";

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            assertThatThrownBy(() ->
                generator.fromHtml(html)
                    .executeJavaScript("   ")
                    .generate()
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JavaScript code cannot be null or empty");
        }
    }

    /**
     * Tests JavaScript execution that uses async/await (Promise-based code).
     * The library should wait for the Promise to resolve before generating the PDF.
     */
    @Test
    void testAsyncJavaScriptExecution() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Async Test</title>
            </head>
            <body>
                <div id="result">Loading...</div>
            </body>
            </html>
            """;

        String jsCode = """
            await new Promise(resolve => setTimeout(resolve, 100));
            document.getElementById('result').textContent = 'Async operation completed';
            """;

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScript(jsCode)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests that JavaScript errors are properly caught and reported.
     */
    @Test
    void testJavaScriptExecutionWithError() {
        String html = "<html><body><h1>Test</h1></body></html>";

        // JavaScript code that will throw an error
        String jsCode = "throw new Error('Test error from JavaScript');";

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            assertThatThrownBy(() ->
                generator.fromHtml(html)
                    .executeJavaScript(jsCode)
                    .generate()
            )
                .isInstanceOf(PdfGenerationException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage()).contains("Failed to execute JavaScript");
                });
        }
    }

    /**
     * Tests that JavaScript execution works with CSS injection.
     * Both CSS and JavaScript should be applied in the correct order.
     */
    @Test
    void testJavaScriptWithCssInjection() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>CSS + JS Test</title>
            </head>
            <body>
                <h1 id="title">Test Title</h1>
            </body>
            </html>
            """;

        String css = "h1 { color: red; }";
        String jsCode = "document.getElementById('title').textContent = 'Modified Title';";

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .withCustomCss(css)
                .executeJavaScript(jsCode)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests that JavaScript execution works with PDF options.
     */
    @Test
    void testJavaScriptWithPdfOptions() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Options Test</title>
            </head>
            <body>
                <div id="content">Original</div>
            </body>
            </html>
            """;

        String jsCode = "document.getElementById('content').textContent = 'Modified';";

        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .printBackground(true)
            .build();

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScript(jsCode)
                .withOptions(options)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests that JavaScript can add new elements to the DOM.
     */
    @Test
    void testJavaScriptAddsElements() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Add Elements Test</title>
            </head>
            <body>
                <div id="container"></div>
            </body>
            </html>
            """;

        String jsCode = """
            const container = document.getElementById('container');
            for (let i = 1; i <= 5; i++) {
                const p = document.createElement('p');
                p.textContent = 'Paragraph ' + i;
                container.appendChild(p);
            }
            """;

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScript(jsCode)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests that JavaScript can modify document properties.
     */
    @Test
    void testJavaScriptModifiesDocumentProperties() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Original Title</title>
            </head>
            <body>
                <h1>Test Page</h1>
            </body>
            </html>
            """;

        String jsCode = "document.title = 'Modified Title by JavaScript';";

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScript(jsCode)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Tests that multiple JavaScript statements execute in sequence.
     */
    @Test
    void testMultipleJavaScriptStatements() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Multiple Statements Test</title>
            </head>
            <body>
                <div id="first">First</div>
                <div id="second">Second</div>
                <div id="third">Third</div>
            </body>
            </html>
            """;

        String jsCode = """
            document.getElementById('first').textContent = 'Modified First';
            document.getElementById('second').textContent = 'Modified Second';
            document.getElementById('third').textContent = 'Modified Third';
            """;

        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromHtml(html)
                .executeJavaScript(jsCode)
                .generate();

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(isPdfValid(pdf)).isTrue();
        }
    }

    /**
     * Helper method to validate that the byte array is a valid PDF.
     *
     * @param pdfData the PDF data to validate
     * @return true if the data starts with the PDF magic number (%PDF-)
     */
    private boolean isPdfValid(byte[] pdfData) {
        if (pdfData == null || pdfData.length < 5) {
            return false;
        }
        return pdfData[0] == '%' &&
               pdfData[1] == 'P' &&
               pdfData[2] == 'D' &&
               pdfData[3] == 'F' &&
               pdfData[4] == '-';
    }
}
