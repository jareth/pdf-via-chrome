package com.fostermoore.pdfviachrome.converter;

import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.github.kklisura.cdt.protocol.commands.Page;
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

            // Navigate to about:blank first
            logger.trace("Navigating to {}", ABOUT_BLANK);
            page.navigate(ABOUT_BLANK);

            // Set up a latch to wait for DOMContentLoaded event
            CountDownLatch domLoadedLatch = new CountDownLatch(1);
            AtomicBoolean loadError = new AtomicBoolean(false);

            // Listen for DOMContentLoaded event
            page.onDomContentEventFired(event -> {
                logger.trace("DOMContentLoaded event received");
                domLoadedLatch.countDown();
            });

            // Listen for load error events
            page.onLoadEventFired(event -> {
                logger.trace("Load event fired");
            });

            // Set the document content
            logger.trace("Setting document content (HTML length: {} bytes)", html.length());
            page.setDocumentContent(page.getFrameTree().getFrame().getId(), html);

            // Wait for DOMContentLoaded event with timeout
            logger.trace("Waiting for DOMContentLoaded event (timeout: {}ms)", loadTimeoutMs);
            boolean loaded = domLoadedLatch.await(loadTimeoutMs, TimeUnit.MILLISECONDS);

            if (!loaded) {
                throw new PdfGenerationException(
                    "Timeout waiting for page to load (timeout: " + loadTimeoutMs + "ms)"
                );
            }

            if (loadError.get()) {
                throw new PdfGenerationException("Error occurred while loading HTML content");
            }

            logger.debug("Page loaded successfully, generating PDF");

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
}
