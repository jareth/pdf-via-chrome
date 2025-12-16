package com.github.headlesschromepdf.cdp;

import com.github.headlesschromepdf.exception.BrowserTimeoutException;
import com.github.headlesschromepdf.exception.PageLoadException;
import com.github.headlesschromepdf.exception.PdfGenerationException;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.types.page.Navigate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controller class that orchestrates Chrome DevTools Protocol Page domain operations
 * for PDF generation.
 * <p>
 * This class provides high-level methods for:
 * <ul>
 *   <li>Navigating to URLs</li>
 *   <li>Loading HTML content directly</li>
 *   <li>Generating PDFs from loaded pages</li>
 *   <li>Handling page load events and timeouts</li>
 * </ul>
 * </p>
 * <p>
 * PageController handles the complexity of CDP event-driven operations and provides
 * synchronous methods that wait for page load completion before returning.
 * </p>
 */
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);
    private static final int DEFAULT_PAGE_LOAD_TIMEOUT_MS = 30000;
    private static final String ABOUT_BLANK = "about:blank";

    private final CdpSession session;
    private final int pageLoadTimeoutMs;

    /**
     * Creates a new PageController with the specified CDP session.
     *
     * @param session the CDP session to use for page operations
     */
    public PageController(CdpSession session) {
        this(session, DEFAULT_PAGE_LOAD_TIMEOUT_MS);
    }

    /**
     * Creates a new PageController with the specified CDP session and timeout.
     *
     * @param session the CDP session to use for page operations
     * @param pageLoadTimeoutMs the timeout for page load operations in milliseconds
     */
    public PageController(CdpSession session, int pageLoadTimeoutMs) {
        if (session == null) {
            throw new IllegalArgumentException("CdpSession cannot be null");
        }
        if (pageLoadTimeoutMs <= 0) {
            throw new IllegalArgumentException("Page load timeout must be positive");
        }

        this.session = session;
        this.pageLoadTimeoutMs = pageLoadTimeoutMs;
    }

    /**
     * Navigates to the specified URL and waits for the page to load.
     * <p>
     * This method enables the Page domain, navigates to the URL, and waits for
     * the DOMContentLoaded event before returning. It will throw an exception
     * if navigation fails or times out.
     * </p>
     *
     * @param url the URL to navigate to
     * @throws PageLoadException if navigation fails
     * @throws BrowserTimeoutException if page load times out
     * @throws IllegalArgumentException if url is null or empty
     */
    public void navigateToUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        logger.info("Navigating to URL: {}", url);

        Page page = session.getPage();

        // Enable Page domain to receive events
        page.enable();

        // Setup latch to wait for page load
        CountDownLatch loadLatch = new CountDownLatch(1);
        AtomicBoolean loadFailed = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        // Listen for DOMContentLoaded event
        page.onDomContentEventFired(event -> {
            logger.debug("DOMContentLoaded event fired for URL: {}", url);
            loadLatch.countDown();
        });

        // Listen for loading failures
        page.onLoadEventFired(event -> {
            logger.debug("Load event fired for URL: {}", url);
        });

        // Navigate to URL
        try {
            Navigate navigateResult = page.navigate(url);

            // Check if navigation encountered an error
            if (navigateResult != null && navigateResult.getErrorText() != null) {
                String error = navigateResult.getErrorText();
                logger.error("Navigation failed for URL {}: {}", url, error);
                throw new PageLoadException("Failed to navigate to URL: " + error);
            }

            logger.debug("Navigation initiated for URL: {}", url);

        } catch (Exception e) {
            if (e instanceof PageLoadException) {
                throw e;
            }
            logger.error("Exception during navigation to URL: {}", url, e);
            throw new PageLoadException("Failed to navigate to URL: " + url, e);
        }

        // Wait for page load or timeout
        try {
            boolean loaded = loadLatch.await(pageLoadTimeoutMs, TimeUnit.MILLISECONDS);

            if (!loaded) {
                logger.error("Page load timed out after {} ms for URL: {}", pageLoadTimeoutMs, url);
                throw new BrowserTimeoutException(
                    String.format("Page load timed out after %d ms for URL: %s",
                        pageLoadTimeoutMs, url));
            }

            if (loadFailed.get()) {
                String error = errorMessage.get();
                logger.error("Page load failed for URL {}: {}", url, error);
                throw new PageLoadException("Page load failed: " + error);
            }

            logger.info("Successfully loaded URL: {}", url);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Page load interrupted for URL: {}", url, e);
            throw new PageLoadException("Page load interrupted for URL: " + url, e);
        }
    }

    /**
     * Loads HTML content directly into the page using a data URI.
     * <p>
     * This method navigates to a data URI containing the HTML content and waits
     * for the page to load. This is useful for converting HTML strings to PDF
     * without needing a web server.
     * </p>
     *
     * @param htmlContent the HTML content to load
     * @throws PageLoadException if loading fails
     * @throws BrowserTimeoutException if page load times out
     * @throws IllegalArgumentException if htmlContent is null
     */
    public void loadHtmlContent(String htmlContent) {
        if (htmlContent == null) {
            throw new IllegalArgumentException("HTML content cannot be null");
        }

        logger.info("Loading HTML content ({} characters)", htmlContent.length());

        try {
            // Create data URI for HTML content
            // Using data:text/html,<content> format
            String dataUri = "data:text/html," + encodeForDataUri(htmlContent);

            // Navigate to the data URI
            navigateToUrl(dataUri);

            logger.debug("Successfully loaded HTML content");

        } catch (PageLoadException | BrowserTimeoutException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to load HTML content", e);
            throw new PageLoadException("Failed to load HTML content", e);
        }
    }

    /**
     * Loads HTML content using the Page.setDocumentContent() CDP method.
     * <p>
     * This is an alternative to the data URI approach. It first navigates to
     * about:blank and then sets the document content directly.
     * </p>
     *
     * @param htmlContent the HTML content to load
     * @throws PageLoadException if loading fails
     * @throws BrowserTimeoutException if page load times out
     * @throws IllegalArgumentException if htmlContent is null
     */
    public void setDocumentContent(String htmlContent) {
        if (htmlContent == null) {
            throw new IllegalArgumentException("HTML content cannot be null");
        }

        logger.info("Setting document content ({} characters)", htmlContent.length());

        Page page = session.getPage();

        try {
            // Enable Page domain
            page.enable();

            // Navigate to about:blank first
            page.navigate(ABOUT_BLANK);

            // Small delay to ensure navigation completes
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Get the frame tree to obtain the frame ID
            var frameTree = page.getFrameTree();
            String frameId = frameTree.getFrame().getId();

            // Setup latch to wait for page load
            CountDownLatch loadLatch = new CountDownLatch(1);

            // Listen for DOMContentLoaded event
            page.onDomContentEventFired(event -> {
                logger.debug("DOMContentLoaded event fired after setting content");
                loadLatch.countDown();
            });

            // Set the document content
            page.setDocumentContent(frameId, htmlContent);

            // Wait for page load or timeout
            boolean loaded = loadLatch.await(pageLoadTimeoutMs, TimeUnit.MILLISECONDS);

            if (!loaded) {
                logger.error("Page load timed out after {} ms when setting document content", pageLoadTimeoutMs);
                throw new BrowserTimeoutException(
                    String.format("Page load timed out after %d ms when setting document content",
                        pageLoadTimeoutMs));
            }

            logger.debug("Successfully set document content");

        } catch (PageLoadException | BrowserTimeoutException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Page load interrupted when setting document content", e);
            throw new PageLoadException("Page load interrupted when setting document content", e);
        } catch (Exception e) {
            logger.error("Failed to set document content", e);
            throw new PageLoadException("Failed to set document content", e);
        }
    }

    /**
     * Generates a PDF from the currently loaded page with default options.
     * <p>
     * This method calls the Page.printToPDF() CDP command and decodes the
     * base64-encoded PDF data into a byte array.
     * </p>
     *
     * @return the PDF as a byte array
     * @throws PdfGenerationException if PDF generation fails
     */
    public byte[] generatePdf() {
        logger.info("Generating PDF from current page with default options");

        Page page = session.getPage();

        try {
            // Generate PDF with default options
            var pdfResult = page.printToPDF(
                false,  // landscape
                false,  // displayHeaderFooter
                false,  // printBackground
                1.0,    // scale
                null,   // paperWidth
                null,   // paperHeight
                null,   // marginTop
                null,   // marginBottom
                null,   // marginLeft
                null,   // marginRight
                null,   // pageRanges
                false,  // ignoreInvalidPageRanges
                null,   // headerTemplate
                null,   // footerTemplate
                false,  // preferCSSPageSize
                null    // transferMode - null returns base64
            );

            if (pdfResult == null) {
                throw new PdfGenerationException("PDF generation returned null result");
            }

            String base64Pdf = pdfResult.getData();

            if (base64Pdf == null || base64Pdf.isEmpty()) {
                throw new PdfGenerationException("PDF generation returned empty data");
            }

            // Decode base64 to byte array
            byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

            logger.info("Successfully generated PDF ({} bytes)", pdfBytes.length);

            return pdfBytes;

        } catch (PdfGenerationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid base64 PDF data", e);
            throw new PdfGenerationException("Failed to decode PDF data", e);
        } catch (Exception e) {
            logger.error("Failed to generate PDF", e);
            throw new PdfGenerationException("Failed to generate PDF", e);
        }
    }

    /**
     * Encodes HTML content for use in a data URI.
     * <p>
     * This method URL-encodes the HTML content to make it safe for use in a data URI.
     * Special characters are encoded, but common HTML characters are preserved for readability.
     * </p>
     *
     * @param html the HTML content to encode
     * @return the encoded HTML suitable for a data URI
     */
    private String encodeForDataUri(String html) {
        try {
            // URL encode the HTML for data URI
            // We use UTF-8 encoding
            return URLEncoder.encode(html, StandardCharsets.UTF_8.toString())
                // Restore some characters that don't need encoding for better readability
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            // This should never happen with UTF-8
            throw new PdfGenerationException("Failed to encode HTML content", e);
        }
    }

    /**
     * Gets the page load timeout in milliseconds.
     *
     * @return the timeout value
     */
    public int getPageLoadTimeoutMs() {
        return pageLoadTimeoutMs;
    }
}
