package com.fostermoore.pdfviachrome.api;

import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import com.fostermoore.pdfviachrome.converter.HtmlToPdfConverter;
import com.fostermoore.pdfviachrome.converter.UrlToPdfConverter;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main entry point for PDF generation from HTML content, URLs, and DOM Documents.
 *
 * This class provides a fluent API for generating PDFs using headless Chrome.
 * It implements AutoCloseable to ensure proper cleanup of Chrome and CDP resources.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a PDF generator
 * try (PdfGenerator generator = PdfGenerator.create().build()) {
 *     // Generate PDF from HTML
 *     byte[] pdf = generator.fromHtml("<html><body><h1>Hello World</h1></body></html>")
 *         .withOptions(PdfOptions.builder().landscape(true).build())
 *         .generate();
 *
 *     // Generate PDF from URL
 *     byte[] pdf2 = generator.fromUrl("https://example.com")
 *         .withOptions(PdfOptions.defaults())
 *         .generate();
 *
 *     // Generate PDF from DOM Document
 *     Document doc = ... // create or parse a DOM Document
 *     byte[] pdf3 = generator.fromDocument(doc)
 *         .withOptions(PdfOptions.builder().printBackground(true).build())
 *         .generate();
 * }
 * }</pre>
 *
 * <p>Thread-safety: A single PdfGenerator instance can be used to generate multiple PDFs
 * sequentially. Concurrent PDF generation from the same instance is thread-safe.</p>
 */
public class PdfGenerator implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerator.class);

    private final ChromeOptions chromeOptions;
    private ChromeManager chromeManager;
    private CdpSession cdpSession;
    private volatile boolean closed = false;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean initialized = false;

    /**
     * Private constructor - use create() to obtain a builder.
     *
     * @param builder the builder containing configuration
     */
    private PdfGenerator(Builder builder) {
        this.chromeOptions = builder.buildChromeOptions();
    }

    /**
     * Creates a new builder for configuring the PDF generator.
     *
     * @return a new Builder instance
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Starts building a PDF generation request from HTML content.
     *
     * @param html the HTML content to convert to PDF
     * @return a GenerationBuilder for configuring the PDF generation
     * @throws IllegalArgumentException if html is null or empty
     * @throws IllegalStateException if the generator has been closed
     */
    public GenerationBuilder fromHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            throw new IllegalArgumentException("HTML content cannot be null or empty");
        }
        if (closed) {
            throw new IllegalStateException("PdfGenerator has been closed");
        }

        return new GenerationBuilder(ContentSource.html(html));
    }

    /**
     * Starts building a PDF generation request from a URL.
     *
     * @param url the URL to convert to PDF
     * @return a GenerationBuilder for configuring the PDF generation
     * @throws IllegalArgumentException if url is null or empty
     * @throws IllegalStateException if the generator has been closed
     */
    public GenerationBuilder fromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (closed) {
            throw new IllegalStateException("PdfGenerator has been closed");
        }

        return new GenerationBuilder(ContentSource.url(url));
    }

    /**
     * Starts building a PDF generation request from a DOM Document.
     *
     * @param document the DOM Document to convert to PDF
     * @return a GenerationBuilder for configuring the PDF generation
     * @throws IllegalArgumentException if document is null
     * @throws IllegalStateException if the generator has been closed
     * @throws PdfGenerationException if document serialization fails
     */
    public GenerationBuilder fromDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (closed) {
            throw new IllegalStateException("PdfGenerator has been closed");
        }

        String html = documentToHtml(document);
        return fromHtml(html);
    }

    /**
     * Converts a DOM Document to an HTML string.
     *
     * @param document the DOM Document to convert
     * @return the HTML string representation
     * @throws PdfGenerationException if serialization fails
     */
    private String documentToHtml(Document document) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new PdfGenerationException("Failed to convert Document to HTML", e);
        }
    }

    /**
     * Checks if the generator is initialized (Chrome is running).
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Closes the PDF generator and releases all resources.
     * This method is idempotent and can be called multiple times safely.
     */
    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }

            closed = true;
            logger.info("Closing PdfGenerator");

            // Close CDP session first
            if (cdpSession != null) {
                try {
                    cdpSession.close();
                } catch (Exception e) {
                    logger.warn("Error closing CDP session", e);
                } finally {
                    cdpSession = null;
                }
            }

            // Close Chrome manager
            if (chromeManager != null) {
                try {
                    chromeManager.close();
                } catch (Exception e) {
                    logger.warn("Error closing Chrome manager", e);
                } finally {
                    chromeManager = null;
                }
            }

            initialized = false;
            logger.info("PdfGenerator closed successfully");

        } finally {
            lock.unlock();
        }
    }

    /**
     * Initializes Chrome and CDP session lazily (on first generate() call).
     *
     * @throws PdfGenerationException if initialization fails
     */
    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        lock.lock();
        try {
            if (initialized) {
                return; // Double-check after acquiring lock
            }

            if (closed) {
                throw new IllegalStateException("Cannot initialize: PdfGenerator has been closed");
            }

            logger.info("Initializing PdfGenerator (lazy initialization)");

            // Start Chrome
            chromeManager = new ChromeManager(chromeOptions);
            ChromeProcess chromeProcess = chromeManager.start();

            // Create and connect CDP session
            cdpSession = new CdpSession(chromeProcess.getWebSocketDebuggerUrl());
            cdpSession.connect();

            initialized = true;
            logger.info("PdfGenerator initialized successfully");

        } catch (Exception e) {
            // Clean up on failure
            if (cdpSession != null) {
                try {
                    cdpSession.close();
                } catch (Exception ignored) {
                }
                cdpSession = null;
            }
            if (chromeManager != null) {
                try {
                    chromeManager.close();
                } catch (Exception ignored) {
                }
                chromeManager = null;
            }
            throw new PdfGenerationException("Failed to initialize PdfGenerator", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Builder for configuring PdfGenerator instances.
     */
    public static class Builder {
        private Path chromePath;
        private Duration timeout = Duration.ofSeconds(30);
        private boolean headless = true;
        private int remoteDebuggingPort = 0;
        private Path userDataDir;
        private boolean disableGpu = true;
        private boolean disableDevShmUsage = false;
        private boolean noSandbox = false;

        /**
         * Sets the path to the Chrome executable.
         * If not set, Chrome will be auto-detected.
         *
         * @param chromePath the path to Chrome executable
         * @return this builder
         */
        public Builder withChromePath(Path chromePath) {
            this.chromePath = chromePath;
            return this;
        }

        /**
         * Sets the timeout for Chrome startup and page operations.
         *
         * @param timeout the timeout duration
         * @return this builder
         * @throws IllegalArgumentException if timeout is null or non-positive
         */
        public Builder withTimeout(Duration timeout) {
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets whether to run Chrome in headless mode.
         * Default is true.
         *
         * @param headless true for headless mode, false otherwise
         * @return this builder
         */
        public Builder withHeadless(boolean headless) {
            this.headless = headless;
            return this;
        }

        /**
         * Sets the remote debugging port.
         * Use 0 (default) for a random available port.
         *
         * @param port the port number, or 0 for random
         * @return this builder
         */
        public Builder withRemoteDebuggingPort(int port) {
            this.remoteDebuggingPort = port;
            return this;
        }

        /**
         * Sets the user data directory for Chrome.
         * If not set, a temporary directory will be created.
         *
         * @param userDataDir the user data directory path
         * @return this builder
         */
        public Builder withUserDataDir(Path userDataDir) {
            this.userDataDir = userDataDir;
            return this;
        }

        /**
         * Sets whether to disable GPU acceleration.
         * Default is true (recommended for headless mode).
         *
         * @param disableGpu true to disable GPU, false otherwise
         * @return this builder
         */
        public Builder withDisableGpu(boolean disableGpu) {
            this.disableGpu = disableGpu;
            return this;
        }

        /**
         * Sets whether to disable /dev/shm usage.
         * Useful in Docker environments with limited /dev/shm.
         * Default is false.
         *
         * @param disableDevShmUsage true to disable, false otherwise
         * @return this builder
         */
        public Builder withDisableDevShmUsage(boolean disableDevShmUsage) {
            this.disableDevShmUsage = disableDevShmUsage;
            return this;
        }

        /**
         * Sets whether to run Chrome without sandboxing.
         * WARNING: Reduces security. Only use in trusted environments like Docker.
         * Default is false.
         *
         * @param noSandbox true to disable sandboxing, false otherwise
         * @return this builder
         */
        public Builder withNoSandbox(boolean noSandbox) {
            this.noSandbox = noSandbox;
            return this;
        }

        /**
         * Builds the PdfGenerator instance.
         *
         * @return a new PdfGenerator instance
         */
        public PdfGenerator build() {
            return new PdfGenerator(this);
        }

        /**
         * Builds ChromeOptions from the builder configuration.
         *
         * @return ChromeOptions instance
         */
        private ChromeOptions buildChromeOptions() {
            return ChromeOptions.builder()
                .chromePath(chromePath)
                .headless(headless)
                .remoteDebuggingPort(remoteDebuggingPort)
                .userDataDir(userDataDir)
                .disableGpu(disableGpu)
                .disableDevShmUsage(disableDevShmUsage)
                .noSandbox(noSandbox)
                .startupTimeout((int) timeout.getSeconds())
                .shutdownTimeout(5)
                .build();
        }
    }

    /**
     * Builder for configuring individual PDF generation requests.
     */
    public class GenerationBuilder {
        private final ContentSource contentSource;
        private PdfOptions pdfOptions = PdfOptions.defaults();
        private String customCss;
        private String customJavaScript;
        private String baseUrl;

        private GenerationBuilder(ContentSource contentSource) {
            this.contentSource = contentSource;
        }

        /**
         * Sets the PDF options for this generation request.
         *
         * @param options the PDF options
         * @return this builder
         * @throws IllegalArgumentException if options is null
         */
        public GenerationBuilder withOptions(PdfOptions options) {
            if (options == null) {
                throw new IllegalArgumentException("PdfOptions cannot be null");
            }
            this.pdfOptions = options;
            return this;
        }

        /**
         * Injects custom CSS into the page before PDF generation.
         * The CSS is applied after the page loads but before PDF generation.
         * This allows you to override styles specifically for printing, such as
         * hiding navigation, adjusting layouts, or applying print-specific styles.
         *
         * @param css the CSS string to inject
         * @return this builder
         * @throws IllegalArgumentException if css is null or empty
         */
        public GenerationBuilder withCustomCss(String css) {
            if (css == null || css.trim().isEmpty()) {
                throw new IllegalArgumentException("CSS cannot be null or empty");
            }
            this.customCss = css;
            return this;
        }

        /**
         * Injects custom CSS from a file into the page before PDF generation.
         * The CSS is applied after the page loads but before PDF generation.
         * This allows you to override styles specifically for printing, such as
         * hiding navigation, adjusting layouts, or applying print-specific styles.
         *
         * @param cssFile the path to the CSS file
         * @return this builder
         * @throws IllegalArgumentException if cssFile is null or doesn't exist
         * @throws PdfGenerationException if the file cannot be read
         */
        public GenerationBuilder withCustomCssFromFile(Path cssFile) {
            if (cssFile == null) {
                throw new IllegalArgumentException("CSS file path cannot be null");
            }
            if (!Files.exists(cssFile)) {
                throw new IllegalArgumentException("CSS file does not exist: " + cssFile);
            }
            if (!Files.isRegularFile(cssFile)) {
                throw new IllegalArgumentException("CSS file path is not a regular file: " + cssFile);
            }

            try {
                String css = Files.readString(cssFile);
                return withCustomCss(css);
            } catch (IOException e) {
                throw new PdfGenerationException("Failed to read CSS file: " + cssFile, e);
            }
        }

        /**
         * Executes custom JavaScript in the page before PDF generation.
         * The JavaScript is executed after the page loads (and after CSS injection if any)
         * but before PDF generation. This allows you to manipulate the page content,
         * trigger actions, or wait for dynamic content before capturing the PDF.
         *
         * <p>Example use cases:</p>
         * <ul>
         *   <li>Remove elements: {@code document.querySelector('.ads').remove()}</li>
         *   <li>Trigger rendering: {@code window.renderChart()}</li>
         *   <li>Modify content: {@code document.title = 'PDF Export'}</li>
         *   <li>Wait for async operations: {@code await loadData()}</li>
         * </ul>
         *
         * @param jsCode the JavaScript code to execute
         * @return this builder
         * @throws IllegalArgumentException if jsCode is null or empty
         */
        public GenerationBuilder executeJavaScript(String jsCode) {
            if (jsCode == null || jsCode.trim().isEmpty()) {
                throw new IllegalArgumentException("JavaScript code cannot be null or empty");
            }
            this.customJavaScript = jsCode;
            return this;
        }

        /**
         * Executes custom JavaScript from a file in the page before PDF generation.
         * The JavaScript is executed after the page loads (and after CSS injection if any)
         * but before PDF generation. This allows you to manipulate the page content,
         * trigger actions, or wait for dynamic content before capturing the PDF.
         *
         * @param jsFile the path to the JavaScript file
         * @return this builder
         * @throws IllegalArgumentException if jsFile is null or doesn't exist
         * @throws PdfGenerationException if the file cannot be read
         */
        public GenerationBuilder executeJavaScriptFromFile(Path jsFile) {
            if (jsFile == null) {
                throw new IllegalArgumentException("JavaScript file path cannot be null");
            }
            if (!Files.exists(jsFile)) {
                throw new IllegalArgumentException("JavaScript file does not exist: " + jsFile);
            }
            if (!Files.isRegularFile(jsFile)) {
                throw new IllegalArgumentException("JavaScript file path is not a regular file: " + jsFile);
            }

            try {
                String jsCode = Files.readString(jsFile);
                return executeJavaScript(jsCode);
            } catch (IOException e) {
                throw new PdfGenerationException("Failed to read JavaScript file: " + jsFile, e);
            }
        }

        /**
         * Sets the base URL for resolving relative URLs in HTML content.
         * This is particularly useful when HTML content contains relative paths to images,
         * stylesheets, or other resources that need to be loaded from a web server.
         *
         * <p>The base URL is injected into the HTML as a {@code <base>} tag, which tells
         * the browser how to resolve relative URLs. For example, if you set the base URL
         * to "http://localhost:8080/", then an image tag like {@code <img src="/images/logo.png"/>}
         * will be resolved to "http://localhost:8080/images/logo.png".</p>
         *
         * <p>Example usage:</p>
         * <pre>{@code
         * try (PdfGenerator generator = PdfGenerator.create().build()) {
         *     String html = "<html><body><img src='/images/logo.png'/></body></html>";
         *     byte[] pdf = generator.fromHtml(html)
         *         .withBaseUrl("http://localhost:8080/")
         *         .generate();
         * }
         * }</pre>
         *
         * <p><strong>Important:</strong> This method only works with HTML content (fromHtml/fromDocument).
         * It has no effect when generating PDFs from URLs (fromUrl), as URLs already have
         * their own base context.</p>
         *
         * @param baseUrl the base URL to use for resolving relative URLs (e.g., "http://localhost:8080/")
         * @return this builder
         * @throws IllegalArgumentException if baseUrl is null or empty
         */
        public GenerationBuilder withBaseUrl(String baseUrl) {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Base URL cannot be null or empty");
            }
            this.baseUrl = baseUrl.trim();
            return this;
        }

        /**
         * Generates the PDF.
         *
         * @return the PDF content as a byte array
         * @throws PdfGenerationException if PDF generation fails
         */
        public byte[] generate() {
            lock.lock();
            try {
                // Ensure Chrome is initialized
                ensureInitialized();

                logger.debug("Generating PDF from {}", contentSource.getType());

                // Delegate to appropriate converter based on content source type
                if (contentSource.isHtml()) {
                    // Use HtmlToPdfConverter for HTML content
                    HtmlToPdfConverter converter = new HtmlToPdfConverter(cdpSession);
                    return converter.convert(
                        contentSource.getContent(),
                        pdfOptions,
                        customCss,
                        customJavaScript,
                        baseUrl
                    );
                } else {
                    // Use UrlToPdfConverter for URL content
                    if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                        logger.warn("baseUrl is ignored when generating PDF from URL (URL already has base context)");
                    }
                    if (customJavaScript != null && !customJavaScript.trim().isEmpty()) {
                        logger.warn("JavaScript execution is not supported for URL-based PDF generation");
                    }
                    UrlToPdfConverter converter = new UrlToPdfConverter(cdpSession);
                    return converter.convert(
                        contentSource.getContent(),
                        pdfOptions,
                        customCss
                    );
                }

            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Represents the source of content for PDF generation.
     */
    private static class ContentSource {
        private final String type;
        private final String content;

        private ContentSource(String type, String content) {
            this.type = type;
            this.content = content;
        }

        static ContentSource html(String html) {
            return new ContentSource("HTML", html);
        }

        static ContentSource url(String url) {
            return new ContentSource("URL", url);
        }

        String getType() {
            return type;
        }

        String getContent() {
            return content;
        }

        boolean isHtml() {
            return "HTML".equals(type);
        }
    }
}
