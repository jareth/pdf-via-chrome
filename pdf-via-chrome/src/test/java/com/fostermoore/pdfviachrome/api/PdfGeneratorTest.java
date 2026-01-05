package com.fostermoore.pdfviachrome.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PdfGenerator fluent API and builder.
 */
class PdfGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreate_returnsBuilder() {
        PdfGenerator.Builder builder = PdfGenerator.create();

        assertThat(builder).isNotNull();
    }

    @Test
    void testBuilder_withDefaults() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThat(generator).isNotNull();
        assertThat(generator.isInitialized()).isFalse();
    }

    @Test
    void testBuilder_withChromePath() throws IOException {
        Path chromePath = createExecutableFile("chromium");

        PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath)
            .build();

        assertThat(generator).isNotNull();
    }

    @Test
    void testBuilder_withTimeout() {
        PdfGenerator generator = PdfGenerator.create()
            .withTimeout(Duration.ofSeconds(60))
            .build();

        assertThat(generator).isNotNull();
    }

    @Test
    void testBuilder_withTimeout_nullThrowsException() {
        assertThatThrownBy(() -> PdfGenerator.create().withTimeout(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Timeout must be positive");
    }

    @Test
    void testBuilder_withTimeout_zeroThrowsException() {
        assertThatThrownBy(() -> PdfGenerator.create().withTimeout(Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Timeout must be positive");
    }

    @Test
    void testBuilder_withTimeout_negativeThrowsException() {
        assertThatThrownBy(() -> PdfGenerator.create().withTimeout(Duration.ofSeconds(-1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Timeout must be positive");
    }

    @Test
    void testBuilder_withHeadless() {
        PdfGenerator generator1 = PdfGenerator.create()
            .withHeadless(true)
            .build();

        PdfGenerator generator2 = PdfGenerator.create()
            .withHeadless(false)
            .build();

        assertThat(generator1).isNotNull();
        assertThat(generator2).isNotNull();
    }

    @Test
    void testBuilder_withRemoteDebuggingPort() {
        PdfGenerator generator = PdfGenerator.create()
            .withRemoteDebuggingPort(9222)
            .build();

        assertThat(generator).isNotNull();
    }

    @Test
    void testBuilder_withUserDataDir() {
        Path userDataDir = Paths.get("/tmp/chrome-data");

        PdfGenerator generator = PdfGenerator.create()
            .withUserDataDir(userDataDir)
            .build();

        assertThat(generator).isNotNull();
    }

    @Test
    void testBuilder_withDisableGpu() {
        PdfGenerator generator = PdfGenerator.create()
            .withDisableGpu(false)
            .build();

        assertThat(generator).isNotNull();
    }

    @Test
    void testBuilder_withDisableDevShmUsage() {
        PdfGenerator generator = PdfGenerator.create()
            .withDisableDevShmUsage(true)
            .build();

        assertThat(generator).isNotNull();
    }

    @Test
    void testBuilder_withNoSandbox() {
        PdfGenerator generator = PdfGenerator.create()
            .withNoSandbox(true)
            .build();

        assertThat(generator).isNotNull();
    }

    @Test
    void testBuilder_methodChaining() throws IOException {
        Path chromePath = createExecutableFile("chrome");

        PdfGenerator generator = PdfGenerator.create()
            .withChromePath(chromePath)
            .withTimeout(Duration.ofSeconds(45))
            .withHeadless(true)
            .withRemoteDebuggingPort(9222)
            .withDisableGpu(true)
            .withDisableDevShmUsage(true)
            .withNoSandbox(false)
            .build();

        assertThat(generator).isNotNull();
        assertThat(generator.isInitialized()).isFalse();
    }

    @Test
    void testFromHtml_withValidHtml_returnsGenerationBuilder() {
        PdfGenerator generator = PdfGenerator.create().build();

        PdfGenerator.GenerationBuilder builder = generator.fromHtml("<html><body>Test</body></html>");

        assertThat(builder).isNotNull();
    }

    @Test
    void testFromHtml_withNullHtml_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() -> generator.fromHtml(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("HTML content cannot be null or empty");
    }

    @Test
    void testFromHtml_withEmptyHtml_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() -> generator.fromHtml(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("HTML content cannot be null or empty");
    }

    @Test
    void testFromHtml_withWhitespaceHtml_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() -> generator.fromHtml("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("HTML content cannot be null or empty");
    }

    @Test
    void testFromUrl_withValidUrl_returnsGenerationBuilder() {
        PdfGenerator generator = PdfGenerator.create().build();

        PdfGenerator.GenerationBuilder builder = generator.fromUrl("https://example.com");

        assertThat(builder).isNotNull();
    }

    @Test
    void testFromUrl_withNullUrl_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() -> generator.fromUrl(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");
    }

    @Test
    void testFromUrl_withEmptyUrl_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() -> generator.fromUrl(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");
    }

    @Test
    void testFromUrl_withWhitespaceUrl_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() -> generator.fromUrl("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");
    }

    @Test
    void testFromHtml_afterClose_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();
        generator.close();

        assertThatThrownBy(() -> generator.fromHtml("<html></html>"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PdfGenerator has been closed");
    }

    @Test
    void testFromUrl_afterClose_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();
        generator.close();

        assertThatThrownBy(() -> generator.fromUrl("https://example.com"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PdfGenerator has been closed");
    }

    @Test
    void testGenerationBuilder_withOptions() {
        PdfGenerator generator = PdfGenerator.create().build();

        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .paperSize(PdfOptions.PaperFormat.A4)
            .build();

        PdfGenerator.GenerationBuilder builder = generator.fromHtml("<html></html>")
            .withOptions(options);

        assertThat(builder).isNotNull();
    }

    @Test
    void testGenerationBuilder_withOptions_nullThrowsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() ->
            generator.fromHtml("<html></html>").withOptions(null)
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("PdfOptions cannot be null");
    }

    @Test
    void testGenerationBuilder_methodChaining() {
        PdfGenerator generator = PdfGenerator.create().build();

        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .printBackground(true)
            .build();

        PdfGenerator.GenerationBuilder builder = generator.fromHtml("<html><body>Test</body></html>")
            .withOptions(options);

        assertThat(builder).isNotNull();
    }

    @Test
    void testClose_isIdempotent() {
        PdfGenerator generator = PdfGenerator.create().build();

        // Should not throw exceptions when called multiple times
        generator.close();
        generator.close();
        generator.close();
    }

    @Test
    void testTryWithResources_closesAutomatically() {
        PdfGenerator generator;

        try (PdfGenerator gen = PdfGenerator.create().build()) {
            generator = gen;
            assertThat(generator).isNotNull();
        }

        // After try-with-resources, generator should be closed
        assertThatThrownBy(() -> generator.fromHtml("<html></html>"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("has been closed");
    }

    @Test
    void testIsInitialized_falseByDefault() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThat(generator.isInitialized()).isFalse();
    }

    @Test
    void testFluentApi_completeExample() {
        // This test verifies the fluent API compiles and chains correctly
        try (PdfGenerator generator = PdfGenerator.create()
            .withTimeout(Duration.ofSeconds(30))
            .withHeadless(true)
            .build()) {

            PdfGenerator.GenerationBuilder builder = generator
                .fromHtml("<html><body><h1>Test</h1></body></html>")
                .withOptions(PdfOptions.builder()
                    .landscape(true)
                    .paperSize(PdfOptions.PaperFormat.A4)
                    .margins(0.5)
                    .printBackground(true)
                    .build());

            assertThat(builder).isNotNull();
        }
    }

    @Test
    void testFluentApi_urlExample() {
        // This test verifies the URL-based fluent API compiles correctly
        try (PdfGenerator generator = PdfGenerator.create()
            .withTimeout(Duration.ofSeconds(30))
            .build()) {

            PdfGenerator.GenerationBuilder builder = generator
                .fromUrl("https://example.com")
                .withOptions(PdfOptions.defaults());

            assertThat(builder).isNotNull();
        }
    }

    @Test
    void testFromDocument_withValidDocument_returnsGenerationBuilder() throws ParserConfigurationException {
        PdfGenerator generator = PdfGenerator.create().build();
        Document document = createSimpleDocument();

        PdfGenerator.GenerationBuilder builder = generator.fromDocument(document);

        assertThat(builder).isNotNull();
    }

    @Test
    void testFromDocument_withNullDocument_throwsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() -> generator.fromDocument(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Document cannot be null");
    }

    @Test
    void testFromDocument_afterClose_throwsException() throws ParserConfigurationException {
        PdfGenerator generator = PdfGenerator.create().build();
        generator.close();
        Document document = createSimpleDocument();

        assertThatThrownBy(() -> generator.fromDocument(document))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PdfGenerator has been closed");
    }

    @Test
    void testFluentApi_documentExample() throws ParserConfigurationException {
        // This test verifies the Document-based fluent API compiles correctly
        try (PdfGenerator generator = PdfGenerator.create()
            .withTimeout(Duration.ofSeconds(30))
            .build()) {

            Document document = createSimpleDocument();

            PdfGenerator.GenerationBuilder builder = generator
                .fromDocument(document)
                .withOptions(PdfOptions.builder()
                    .printBackground(true)
                    .build());

            assertThat(builder).isNotNull();
        }
    }

    @Test
    void testGenerationBuilder_withBaseUrl() {
        PdfGenerator generator = PdfGenerator.create().build();

        PdfGenerator.GenerationBuilder builder = generator
            .fromHtml("<html><body><img src='/images/logo.png'/></body></html>")
            .withBaseUrl("http://localhost:8080/");

        assertThat(builder).isNotNull();
    }

    @Test
    void testGenerationBuilder_withBaseUrl_nullThrowsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() ->
            generator.fromHtml("<html></html>").withBaseUrl(null)
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Base URL cannot be null or empty");
    }

    @Test
    void testGenerationBuilder_withBaseUrl_emptyThrowsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() ->
            generator.fromHtml("<html></html>").withBaseUrl("")
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Base URL cannot be null or empty");
    }

    @Test
    void testGenerationBuilder_withBaseUrl_whitespaceThrowsException() {
        PdfGenerator generator = PdfGenerator.create().build();

        assertThatThrownBy(() ->
            generator.fromHtml("<html></html>").withBaseUrl("   ")
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Base URL cannot be null or empty");
    }

    @Test
    void testGenerationBuilder_withBaseUrl_methodChaining() {
        PdfGenerator generator = PdfGenerator.create().build();

        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .printBackground(true)
            .build();

        PdfGenerator.GenerationBuilder builder = generator
            .fromHtml("<html><body><img src='/logo.png'/></body></html>")
            .withBaseUrl("http://localhost:8080/")
            .withOptions(options);

        assertThat(builder).isNotNull();
    }

    @Test
    void testFluentApi_completeExampleWithBaseUrl() {
        // This test verifies the fluent API with base URL compiles and chains correctly
        try (PdfGenerator generator = PdfGenerator.create()
            .withTimeout(Duration.ofSeconds(30))
            .withHeadless(true)
            .build()) {

            PdfGenerator.GenerationBuilder builder = generator
                .fromHtml("<html><body><img src='/images/logo.png'/></body></html>")
                .withBaseUrl("http://localhost:8080/")
                .withOptions(PdfOptions.builder()
                    .landscape(true)
                    .paperSize(PdfOptions.PaperFormat.A4)
                    .margins(0.5)
                    .printBackground(true)
                    .build());

            assertThat(builder).isNotNull();
        }
    }

    /**
     * Helper method to create a simple DOM Document for testing.
     */
    private Document createSimpleDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        // Create a simple HTML structure
        var html = document.createElement("html");
        var body = document.createElement("body");
        var h1 = document.createElement("h1");
        h1.setTextContent("Test Document");

        body.appendChild(h1);
        html.appendChild(body);
        document.appendChild(html);

        return document;
    }

    /**
     * Helper method to create an executable file for testing.
     * On Unix-like systems, sets the executable permission.
     * On Windows, files are executable by default.
     */
    private Path createExecutableFile(String filename) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.createFile(file);

        // On Unix-like systems, set executable permission
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            Files.setPosixFilePermissions(file, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            ));
        }

        return file;
    }
}
