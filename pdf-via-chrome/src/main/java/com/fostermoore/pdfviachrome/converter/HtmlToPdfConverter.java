package com.fostermoore.pdfviachrome.converter;

import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Converts HTML string content to PDF using Chrome's headless rendering engine.
 * <p>
 * This converter loads HTML content into a Chrome page using the Chrome DevTools Protocol,
 * waits for the page to be ready, and generates a PDF with the specified options.
 * </p>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * String html = "&lt;html&gt;&lt;body&gt;&lt;h1&gt;Hello World&lt;/h1&gt;&lt;/body&gt;&lt;/html&gt;";
 * PdfOptions options = PdfOptions.builder()
 *     .printBackground(true)
 *     .paperSize(PaperFormat.A4)
 *     .build();
 *
 * try (CdpSession session = ...) {
 *     HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
 *     byte[] pdfData = converter.convert(html, options);
 *     // Save or process PDF data
 * }
 * </pre>
 */
public class HtmlToPdfConverter {

    private static final Logger logger = LoggerFactory.getLogger(HtmlToPdfConverter.class);
    private static final int DEFAULT_LOAD_TIMEOUT_MS = 30000;
    private static final String ABOUT_BLANK = "about:blank";

    private final CdpSession session;
    private final int loadTimeoutMs;

    /**
     * Creates a new HTML to PDF converter with the specified CDP session.
     *
     * @param session the CDP session connected to Chrome
     * @throws IllegalArgumentException if session is null
     */
    public HtmlToPdfConverter(CdpSession session) {
        this(session, DEFAULT_LOAD_TIMEOUT_MS);
    }

    /**
     * Creates a new HTML to PDF converter with the specified CDP session and timeout.
     *
     * @param session the CDP session connected to Chrome
     * @param loadTimeoutMs the timeout for page loading in milliseconds
     * @throws IllegalArgumentException if session is null or timeout is not positive
     */
    public HtmlToPdfConverter(CdpSession session, int loadTimeoutMs) {
        if (session == null) {
            throw new IllegalArgumentException("CdpSession cannot be null");
        }
        if (loadTimeoutMs <= 0) {
            throw new IllegalArgumentException("Load timeout must be positive");
        }

        this.session = session;
        this.loadTimeoutMs = loadTimeoutMs;
    }

    /**
     * Converts HTML string content to PDF.
     *
     * @param html the HTML content to convert
     * @param options the PDF generation options
     * @return the PDF data as a byte array
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if html or options are null
     */
    public byte[] convert(String html, PdfOptions options) {
        return convert(html, options, null, null);
    }

    /**
     * Converts HTML string content to PDF with optional custom CSS injection.
     *
     * @param html the HTML content to convert
     * @param options the PDF generation options
     * @param customCss optional custom CSS to inject (can be null)
     * @return the PDF data as a byte array
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if html or options are null
     */
    public byte[] convert(String html, PdfOptions options, String customCss) {
        return convert(html, options, customCss, null);
    }

    /**
     * Converts HTML string content to PDF with optional custom CSS and JavaScript.
     *
     * @param html the HTML content to convert
     * @param options the PDF generation options
     * @param customCss optional custom CSS to inject (can be null)
     * @param customJavaScript optional custom JavaScript to execute (can be null)
     * @return the PDF data as a byte array
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if html or options are null
     */
    public byte[] convert(String html, PdfOptions options, String customCss, String customJavaScript) {
        return convert(html, options, customCss, customJavaScript, null);
    }

    /**
     * Converts HTML string content to PDF with optional custom CSS, JavaScript, and base URL.
     *
     * @param html the HTML content to convert
     * @param options the PDF generation options
     * @param customCss optional custom CSS to inject (can be null)
     * @param customJavaScript optional custom JavaScript to execute (can be null)
     * @param baseUrl optional base URL for resolving relative URLs (can be null)
     * @return the PDF data as a byte array
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if html or options are null
     */
    public byte[] convert(String html, PdfOptions options, String customCss, String customJavaScript, String baseUrl) {
        if (html == null) {
            throw new IllegalArgumentException("HTML content cannot be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("PdfOptions cannot be null");
        }

        if (!session.isConnected()) {
            throw new PdfGenerationException("CDP session is not connected");
        }

        logger.debug("Starting HTML to PDF conversion (HTML length: {} bytes)", html.length());

        try {
            Page page = session.getPage();

            // Enable Page domain to receive events
            page.enable();

            // Enable Network domain to monitor resource loading
            var network = session.getNetwork();
            network.enable();

            // Log network requests to help debug resource loading issues
            network.onRequestWillBeSent(event -> {
                logger.trace("Chrome requesting: {} ({})", event.getRequest().getUrl(), event.getType());
            });

            network.onResponseReceived(event -> {
                var response = event.getResponse();
                logger.trace("Chrome received response: {} - Status {} ({})",
                    response.getUrl(), response.getStatus(), event.getType());
            });

            network.onLoadingFailed(event -> {
                logger.warn("Chrome failed to load resource: {} - {}",
                    event.getErrorText(), event.getType());
                if (event.getType() != null && event.getType().toString().equals("Image")) {
                    logger.error("Image loading failed - PDF may be missing images");
                }
            });

            // Navigate to base URL first if provided (to establish security context/origin)
            // Otherwise navigate to about:blank
            // This is critical: navigating to the base URL allows setDocumentContent to fetch resources from that origin
            String navigationUrl = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl : ABOUT_BLANK;
            logger.trace("Navigating to {} to establish security context", navigationUrl);
            page.navigate(navigationUrl);

            // Set up a latch to wait for load event (fires after all resources are loaded)
            CountDownLatch loadEventLatch = new CountDownLatch(1);
            AtomicBoolean loadError = new AtomicBoolean(false);

            // Listen for DOMContentLoaded event (for logging only)
            page.onDomContentEventFired(event -> {
                logger.trace("DOMContentLoaded event received");
            });

            // Listen for load event (fires after all resources including images are loaded)
            page.onLoadEventFired(event -> {
                logger.trace("Load event fired - all resources loaded");
                loadEventLatch.countDown();
            });

            // Inject base URL if provided (note: we still inject it for cases where base URL isn't navigable)
            String htmlContent = html;
            if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                htmlContent = injectBaseUrl(html, baseUrl);
                logger.debug("Injected base URL into HTML: {}", baseUrl);
            }

            // Set the document content
            // Because we navigated to baseUrl first, the page now has that origin and can fetch resources
            logger.trace("Setting document content (HTML length: {} bytes)", htmlContent.length());
            page.setDocumentContent(page.getFrameTree().getFrame().getId(), htmlContent);

            // Wait for load event (all resources loaded) with timeout
            logger.trace("Waiting for load event - all resources including images (timeout: {}ms)", loadTimeoutMs);
            boolean loaded = loadEventLatch.await(loadTimeoutMs, TimeUnit.MILLISECONDS);

            if (!loaded) {
                throw new PdfGenerationException(
                    "Timeout waiting for page and resources to load (timeout: " + loadTimeoutMs + "ms)"
                );
            }

            if (loadError.get()) {
                throw new PdfGenerationException("Error occurred while loading HTML content");
            }

            logger.debug("Page and all resources (including images) loaded successfully");

            // Inject custom CSS if provided
            if (customCss != null && !customCss.trim().isEmpty()) {
                injectCss(customCss);
            }

            // Execute custom JavaScript if provided
            if (customJavaScript != null && !customJavaScript.trim().isEmpty()) {
                doExecuteJavaScript(customJavaScript);
            }

            logger.debug("Generating PDF");

            // Generate PDF with the specified options
            byte[] pdfData = generatePdf(page, options);

            logger.info("PDF generated successfully (size: {} bytes)", pdfData.length);
            return pdfData;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PdfGenerationException("PDF generation interrupted", e);
        } catch (PdfGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to convert HTML to PDF", e);
        }
    }

    /**
     * Converts HTML string content to PDF using default options.
     *
     * @param html the HTML content to convert
     * @return the PDF data as a byte array
     * @throws PdfGenerationException if PDF generation fails
     */
    public byte[] convert(String html) {
        return convert(html, PdfOptions.defaults());
    }

    /**
     * Generates PDF from the current page using CDP's printToPDF command.
     *
     * @param page the CDP Page domain
     * @param options the PDF generation options
     * @return the PDF data as a byte array
     * @throws PdfGenerationException if PDF generation fails
     */
    private byte[] generatePdf(Page page, PdfOptions options) {
        try {
            logger.trace("Calling Page.printToPDF with options: landscape={}, printBackground={}, scale={}",
                options.isLandscape(), options.isPrintBackground(), options.getScale());

            // Call printToPDF and get result
            var pdfResult = page.printToPDF(
                options.isLandscape(),
                options.isDisplayHeaderFooter(),
                options.isPrintBackground(),
                options.getScale(),
                options.getPaperWidth(),
                options.getPaperHeight(),
                options.getMarginTop(),
                options.getMarginBottom(),
                options.getMarginLeft(),
                options.getMarginRight(),
                options.getPageRanges().isEmpty() ? null : options.getPageRanges(),
                false, // ignoreInvalidPageRanges
                options.getHeaderTemplate().isEmpty() ? null : options.getHeaderTemplate(),
                options.getFooterTemplate().isEmpty() ? null : options.getFooterTemplate(),
                options.isPreferCssPageSize(),
                null // transferMode - null returns base64 data
            );

            // Check if result is null
            if (pdfResult == null) {
                throw new PdfGenerationException("Chrome returned null PDF result");
            }

            // Get base64-encoded PDF data from result
            String base64Pdf = pdfResult.getData();

            if (base64Pdf == null || base64Pdf.isEmpty()) {
                throw new PdfGenerationException("Chrome returned empty PDF data");
            }

            logger.trace("Received base64-encoded PDF (length: {} chars)", base64Pdf.length());

            // Decode base64 to byte array
            byte[] pdfData = Base64.getDecoder().decode(base64Pdf);

            // Validate PDF data starts with PDF magic number
            if (pdfData.length < 5 || !isPdfMagicNumber(pdfData)) {
                throw new PdfGenerationException("Generated data does not appear to be a valid PDF");
            }

            return pdfData;

        } catch (PdfGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to generate PDF from page", e);
        }
    }

    /**
     * Checks if the byte array starts with PDF magic number (%PDF-).
     *
     * @param data the byte array to check
     * @return true if it starts with PDF magic number
     */
    private boolean isPdfMagicNumber(byte[] data) {
        if (data.length < 5) {
            return false;
        }
        String header = new String(data, 0, 5, StandardCharsets.US_ASCII);
        return header.equals("%PDF-");
    }

    /**
     * Injects CSS into the page by creating a style element in the document head.
     *
     * @param css the CSS to inject
     * @throws PdfGenerationException if CSS injection fails
     */
    private void injectCss(String css) {
        try {
            logger.debug("Injecting custom CSS (length: {} chars)", css.length());

            // Get the Runtime domain
            Runtime runtime = session.getRuntime();

            // Enable Runtime domain
            runtime.enable();

            // Escape the CSS for JavaScript string literal
            String escapedCss = css
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

            // JavaScript code to inject CSS
            String jsCode = String.format(
                "(function() {" +
                "  var style = document.createElement('style');" +
                "  style.textContent = '%s';" +
                "  document.head.appendChild(style);" +
                "})()",
                escapedCss
            );

            // Execute the JavaScript
            var result = runtime.evaluate(jsCode);

            // Check for JavaScript execution errors
            if (result.getExceptionDetails() != null) {
                String errorMessage = result.getExceptionDetails().getText();
                logger.error("CSS injection failed: {}", errorMessage);
                throw new PdfGenerationException("Failed to inject CSS: " + errorMessage);
            }

            logger.debug("Custom CSS injected successfully");

        } catch (PdfGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to inject custom CSS", e);
        }
    }

    /**
     * Executes JavaScript in the page context.
     * Supports both synchronous code and Promise-based asynchronous code.
     *
     * @param jsCode the JavaScript code to execute
     * @throws PdfGenerationException if JavaScript execution fails
     */
    private void doExecuteJavaScript(String jsCode) {
        try {
            logger.debug("Executing custom JavaScript (length: {} chars)", jsCode.length());

            // Get the Runtime domain
            Runtime runtime = session.getRuntime();

            // Enable Runtime domain
            runtime.enable();

            // Wrap the user's code to handle both sync and async execution
            // If the code returns a Promise, we'll await it
            String wrappedCode = String.format(
                "(async function() { %s })()",
                jsCode
            );

            // Execute the JavaScript with await promise support
            var result = runtime.evaluate(
                wrappedCode,
                null,    // objectGroup
                false,   // includeCommandLineAPI
                false,   // silent
                null,    // contextId
                false,   // returnByValue
                false,   // generatePreview
                false,   // userGesture
                true,    // awaitPromise - wait for Promise to resolve
                false,   // throwOnSideEffect
                null,    // timeout
                false,   // disableBreaks
                false,   // replMode
                false,   // allowUnsafeEvalBlockedByCSP
                null     // uniqueContextId
            );

            // Check for JavaScript execution errors
            if (result.getExceptionDetails() != null) {
                String errorMessage = result.getExceptionDetails().getText();
                logger.error("JavaScript execution failed: {}", errorMessage);

                // Log the exception details for debugging
                var exception = result.getExceptionDetails().getException();
                if (exception != null && exception.getDescription() != null) {
                    logger.error("Exception details: {}", exception.getDescription());
                }

                throw new PdfGenerationException("Failed to execute JavaScript: " + errorMessage);
            }

            logger.debug("Custom JavaScript executed successfully");

            // Log the result if it's meaningful (not undefined)
            if (result.getResult() != null && result.getResult().getValue() != null) {
                logger.trace("JavaScript execution result: {}", result.getResult().getValue());
            }

        } catch (PdfGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to execute custom JavaScript", e);
        }
    }

    /**
     * Injects a base URL into HTML content by adding a {@code <base>} tag.
     * The base tag is inserted as the first element in the {@code <head>} section.
     * If no {@code <head>} tag exists, one is created.
     *
     * @param html the HTML content
     * @param baseUrl the base URL to inject
     * @return the modified HTML with the base tag
     */
    private String injectBaseUrl(String html, String baseUrl) {
        // Create the base tag
        String baseTag = String.format("<base href=\"%s\">",
            baseUrl.replace("\"", "&quot;")); // Escape quotes in URL

        // Try to find <head> tag (case-insensitive)
        java.util.regex.Pattern headPattern = java.util.regex.Pattern.compile(
            "(<head[^>]*>)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher headMatcher = headPattern.matcher(html);

        if (headMatcher.find()) {
            // Insert base tag right after opening <head> tag
            int insertPosition = headMatcher.end();
            return html.substring(0, insertPosition) + baseTag + html.substring(insertPosition);
        } else {
            // No <head> tag found - try to insert after <html> tag
            java.util.regex.Pattern htmlPattern = java.util.regex.Pattern.compile(
                "(<html[^>]*>)",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher htmlMatcher = htmlPattern.matcher(html);

            if (htmlMatcher.find()) {
                // Insert <head> with base tag after <html>
                int insertPosition = htmlMatcher.end();
                return html.substring(0, insertPosition) +
                       "<head>" + baseTag + "</head>" +
                       html.substring(insertPosition);
            } else {
                // No <html> or <head> tag - prepend a complete head section
                return "<head>" + baseTag + "</head>" + html;
            }
        }
    }
}
