package com.fostermoore.pdfviachrome.converter;

import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpClient;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import com.fostermoore.pdfviachrome.exception.BrowserTimeoutException;
import com.fostermoore.pdfviachrome.exception.PageLoadException;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.fostermoore.pdfviachrome.util.ChromePathDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UrlToPdfConverter that use actual Chrome instances.
 * <p>
 * These tests require Chrome to be installed on the system and internet connectivity.
 * They are disabled by default and can be enabled by setting the
 * CHROME_INTEGRATION_TESTS environment variable to "true".
 * </p>
 * <p>
 * To run: mvn verify -DCHROME_INTEGRATION_TESTS=true
 * </p>
 */
@EnabledIfEnvironmentVariable(named = "CHROME_INTEGRATION_TESTS", matches = "true")
class UrlToPdfConverterIT {

    private static final String EXAMPLE_URL = "https://example.com";
    private static final String HTTPBIN_URL = "https://httpbin.org/html";
    private static final String INVALID_URL = "https://this-domain-definitely-does-not-exist-12345.com";
    private static final String NOT_FOUND_URL = "https://example.com/this-page-definitely-does-not-exist-404";

    @Test
    void testConvertSimpleUrlToPdf() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent()
            .withFailMessage("Chrome not found. Please install Chrome to run integration tests.");

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                byte[] pdfData = converter.convert(EXAMPLE_URL, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(pdfData.length).isGreaterThan(100);

                // Verify PDF magic number
                String header = new String(pdfData, 0, 5, StandardCharsets.US_ASCII);
                assertThat(header).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertUrlWithCustomPdfOptions() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.builder()
                    .landscape(true)
                    .paperSize(PdfOptions.PaperFormat.A4)
                    .margins("1cm")
                    .scale(0.9)
                    .printBackground(true)
                    .build();

                byte[] pdfData = converter.convert(EXAMPLE_URL, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertMultipleUrls() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // Convert first URL
                byte[] pdf1 = converter.convert(EXAMPLE_URL, pdfOptions);
                assertThat(pdf1).isNotNull().isNotEmpty();

                // Convert second URL (same as first)
                byte[] pdf2 = converter.convert(EXAMPLE_URL, pdfOptions);
                assertThat(pdf2).isNotNull().isNotEmpty();

                // All PDFs should be valid
                assertThat(new String(pdf1, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
                assertThat(new String(pdf2, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertUrlWithHeaderAndFooter() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.builder()
                    .displayHeaderFooter(true)
                    .headerTemplate("<div style='font-size:10px; text-align:center; width:100%;'>Generated PDF</div>")
                    .footerTemplate("<div style='font-size:10px; text-align:center; width:100%;'>Page <span class='pageNumber'></span> of <span class='totalPages'></span></div>")
                    .marginTop("1cm")
                    .marginBottom("1cm")
                    .build();

                byte[] pdfData = converter.convert(EXAMPLE_URL, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertWithDefaultMethod() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);

                // Test the convenience method that uses default options
                byte[] pdfData = converter.convert(EXAMPLE_URL);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertWithDisconnectedSession() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            CdpSession session = CdpClient.createSession(process);
            session.connect();

            UrlToPdfConverter converter = new UrlToPdfConverter(session);

            // Close the session
            session.close();

            // Attempting to convert should fail
            PdfOptions pdfOptions = PdfOptions.defaults();
            assertThatThrownBy(() -> converter.convert(EXAMPLE_URL, pdfOptions))
                .isInstanceOf(PdfGenerationException.class)
                .hasMessageContaining("not connected");
        }
    }

    @Test
    void testConvertWithShortTimeout() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                // Use a very short timeout (1ms) - this will likely timeout
                UrlToPdfConverter converter = new UrlToPdfConverter(session, 1);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // This should throw a timeout exception
                assertThatThrownBy(() -> converter.convert(EXAMPLE_URL, pdfOptions))
                    .isInstanceOfAny(BrowserTimeoutException.class, PdfGenerationException.class);
            }
        }
    }

    @Test
    void testConvertInvalidDomain() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session, 10000);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // This should throw a PageLoadException due to DNS resolution failure
                assertThatThrownBy(() -> converter.convert(INVALID_URL, pdfOptions))
                    .isInstanceOf(PageLoadException.class)
                    .hasMessageContaining("Failed to navigate to URL");
            }
        }
    }

    @Test
    void testConvert404Page() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // 404 pages typically load successfully (the server returns a 404 page)
                // So this should succeed, not throw an exception
                byte[] pdfData = converter.convert(NOT_FOUND_URL, pdfOptions);

                // The PDF should still be valid (it's a PDF of the 404 error page)
                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertHttpUrl() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // Test with HTTP (not HTTPS)
                byte[] pdfData = converter.convert("http://example.com", pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertUrlWithQueryParameters() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // Test with query parameters
                byte[] pdfData = converter.convert("https://example.com?param1=value1&param2=value2", pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertUrlWithFragment() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.defaults();

                // Test with fragment
                byte[] pdfData = converter.convert("https://example.com#section", pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void testConvertWithPrintBackground() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions chromeOptions = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
            ChromeProcess process = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(process)) {
                session.connect();

                UrlToPdfConverter converter = new UrlToPdfConverter(session);
                PdfOptions pdfOptions = PdfOptions.builder()
                    .printBackground(true)
                    .build();

                byte[] pdfData = converter.convert(EXAMPLE_URL, pdfOptions);

                assertThat(pdfData).isNotNull();
                assertThat(pdfData).isNotEmpty();
                assertThat(new String(pdfData, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
            }
        }
    }
}
