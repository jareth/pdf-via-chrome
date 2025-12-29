package com.fostermoore.pdfviachrome.converter;

import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.exception.BrowserTimeoutException;
import com.fostermoore.pdfviachrome.exception.PageLoadException;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.page.Navigate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Converts web pages from URLs to PDF using Chrome's headless rendering engine.
 * <p>
 * This converter navigates to a URL using the Chrome DevTools Protocol,
 * waits for the page to load completely, and generates a PDF with the specified options.
 * </p>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * PdfOptions options = PdfOptions.builder()
 *     .printBackground(true)
 *     .paperSize(PaperFormat.A4)
 *     .build();
 *
 * try (CdpSession session = ...) {
 *     UrlToPdfConverter converter = new UrlToPdfConverter(session);
 *     byte[] pdfData = converter.convert("https://example.com", options);
 *     // Save or process PDF data
 * }
 * </pre>
 */
public class UrlToPdfConverter {

    private static final Logger logger = LoggerFactory.getLogger(UrlToPdfConverter.class);
    private static final int DEFAULT_LOAD_TIMEOUT_MS = 30000;

    private final CdpSession session;
    private final int loadTimeoutMs;

    /**
     * Creates a new URL to PDF converter with the specified CDP session.
     *
     * @param session the CDP session connected to Chrome
     * @throws IllegalArgumentException if session is null
     */
    public UrlToPdfConverter(CdpSession session) {
        this(session, DEFAULT_LOAD_TIMEOUT_MS);
    }

    /**
     * Creates a new URL to PDF converter with the specified CDP session and timeout.
     *
     * @param session the CDP session connected to Chrome
     * @param loadTimeoutMs the timeout for page loading in milliseconds
     * @throws IllegalArgumentException if session is null or timeout is not positive
     */
    public UrlToPdfConverter(CdpSession session, int loadTimeoutMs) {
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
     * Converts a web page from a URL to PDF.
     *
     * @param url the URL to navigate to and convert
     * @param options the PDF generation options
     * @return the PDF data as a byte array
     * @throws PageLoadException if the page fails to load (404, network errors, etc.)
     * @throws BrowserTimeoutException if the page load exceeds the configured timeout
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if url or options are null
     */
    public byte[] convert(String url, PdfOptions options) {
        return convert(url, options, null);
    }

    /**
     * Converts a web page from a URL to PDF with optional custom CSS injection.
     *
     * @param url the URL to navigate to and convert
     * @param options the PDF generation options
     * @param customCss optional custom CSS to inject (can be null)
     * @return the PDF data as a byte array
     * @throws PageLoadException if the page fails to load (404, network errors, etc.)
     * @throws BrowserTimeoutException if the page load exceeds the configured timeout
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if url or options are null
     */
    public byte[] convert(String url, PdfOptions options, String customCss) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (options == null) {
            throw new IllegalArgumentException("PdfOptions cannot be null");
        }

        if (!session.isConnected()) {
            throw new PdfGenerationException("CDP session is not connected");
        }

        logger.debug("Starting URL to PDF conversion for: {}", url);

        try {
            Page page = session.getPage();

            // Enable Page domain to receive events
            page.enable();

            // Set up latches to wait for page load events
            CountDownLatch loadEventLatch = new CountDownLatch(1);
            AtomicBoolean navigationError = new AtomicBoolean(false);
            StringBuilder errorMessage = new StringBuilder();

            // Listen for load event
            page.onLoadEventFired(event -> {
                logger.trace("Load event fired for URL: {}", url);
                loadEventLatch.countDown();
            });

            // Listen for frame stopped loading (backup for load event)
            page.onFrameStoppedLoading(event -> {
                logger.trace("Frame stopped loading for URL: {}", url);
            });

            // Navigate to URL
            logger.debug("Navigating to URL: {}", url);
            Navigate navigateResult = page.navigate(url);

            // Check for navigation errors
            if (navigateResult != null && navigateResult.getErrorText() != null) {
                String error = navigateResult.getErrorText();
                logger.error("Navigation error for URL {}: {}", url, error);
                throw new PageLoadException("Failed to navigate to URL: " + error);
            }

            // Wait for load event with timeout
            logger.trace("Waiting for load event (timeout: {}ms)", loadTimeoutMs);
            boolean loaded = loadEventLatch.await(loadTimeoutMs, TimeUnit.MILLISECONDS);

            if (!loaded) {
                throw new BrowserTimeoutException(
                    "Timeout waiting for page to load: " + url + " (timeout: " + loadTimeoutMs + "ms)"
                );
            }

            if (navigationError.get()) {
                throw new PageLoadException("Error occurred while navigating to URL: " + errorMessage.toString());
            }

            logger.debug("Page loaded successfully");

            // Inject custom CSS if provided
            if (customCss != null && !customCss.trim().isEmpty()) {
                injectCss(customCss);
            }

            logger.debug("Generating PDF");

            // Generate PDF with the specified options
            byte[] pdfData = generatePdf(page, options);

            logger.info("PDF generated successfully from URL: {} (size: {} bytes)", url, pdfData.length);
            return pdfData;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PdfGenerationException("PDF generation interrupted", e);
        } catch (PdfGenerationException e) {
            // Re-throw PdfGenerationException and its subclasses (PageLoadException, BrowserTimeoutException)
            // to preserve specific error messages
            throw e;
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to convert URL to PDF: " + url, e);
        }
    }

    /**
     * Converts a web page from a URL to PDF using default options.
     *
     * @param url the URL to navigate to and convert
     * @return the PDF data as a byte array
     * @throws PageLoadException if the page fails to load
     * @throws BrowserTimeoutException if the page load exceeds the configured timeout
     * @throws PdfGenerationException if PDF generation fails
     */
    public byte[] convert(String url) {
        return convert(url, PdfOptions.defaults());
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
}
