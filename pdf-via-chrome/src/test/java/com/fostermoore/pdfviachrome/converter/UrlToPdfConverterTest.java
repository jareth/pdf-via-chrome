package com.fostermoore.pdfviachrome.converter;

import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.exception.BrowserTimeoutException;
import com.fostermoore.pdfviachrome.exception.PageLoadException;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.types.page.Navigate;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.events.page.LoadEventFired;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlToPdfConverterTest {

    private static final String TEST_URL = "https://example.com";
    private static final String VALID_PDF_BASE64 = Base64.getEncoder().encodeToString("%PDF-1.4\n%Test PDF".getBytes());

    @Mock
    private CdpSession mockSession;

    @Mock
    private Page mockPage;

    @Mock
    private Navigate mockNavigateResult;

    private UrlToPdfConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UrlToPdfConverter(mockSession);

        // Setup common mock behaviors (lenient to avoid unnecessary stubbing warnings)
        lenient().when(mockSession.isConnected()).thenReturn(true);
        lenient().when(mockSession.getPage()).thenReturn(mockPage);
        lenient().when(mockPage.navigate(anyString())).thenReturn(mockNavigateResult);
        lenient().when(mockNavigateResult.getErrorText()).thenReturn(null);
    }

    /**
     * Helper method to setup printToPDF mock with valid PDF data.
     */
    private void setupValidPrintToPdfMock() {
        PrintToPDF mockResult = mock(PrintToPDF.class);
        when(mockResult.getData()).thenReturn(VALID_PDF_BASE64);
        when(mockPage.printToPDF(
            anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(),
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(), any(), anyBoolean(), any(), any(),
            anyBoolean(), any()
        )).thenReturn(mockResult);
    }

    /**
     * Helper method to setup printToPDF mock that returns null result.
     */
    private void setupNullPrintToPdfMock() {
        when(mockPage.printToPDF(
            anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(),
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(), any(), anyBoolean(), any(), any(),
            anyBoolean(), any()
        )).thenReturn(null);
    }

    /**
     * Helper method to setup printToPDF mock that returns empty data.
     */
    private void setupEmptyPrintToPdfMock() {
        PrintToPDF mockResult = mock(PrintToPDF.class);
        when(mockResult.getData()).thenReturn("");
        when(mockPage.printToPDF(
            anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(),
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(), any(), anyBoolean(), any(), any(),
            anyBoolean(), any()
        )).thenReturn(mockResult);
    }

    /**
     * Helper method to setup printToPDF mock with invalid PDF data.
     */
    private void setupInvalidPrintToPdfMock() {
        String invalidData = Base64.getEncoder().encodeToString("Not a PDF".getBytes());
        PrintToPDF mockResult = mock(PrintToPDF.class);
        when(mockResult.getData()).thenReturn(invalidData);
        when(mockPage.printToPDF(
            anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(),
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(), any(), anyBoolean(), any(), any(),
            anyBoolean(), any()
        )).thenReturn(mockResult);
    }

    /**
     * Helper method to setup load event handler.
     */
    private void setupLoadEventHandler() {
        doAnswer(invocation -> {
            // Get the event handler that was registered
            Object handler = invocation.getArgument(0);
            // Immediately invoke it using reflection to trigger the countDown
            try {
                Method method = handler.getClass().getMethod("onEvent", Object.class);
                method.invoke(handler, new LoadEventFired());
            } catch (Exception e) {
                // Log or ignore - the test might fail with timeout
                System.err.println("Failed to invoke event handler: " + e.getMessage());
            }
            return null;
        }).when(mockPage).onLoadEventFired(any());
    }

    @AfterEach
    void tearDown() {
        converter = null;
    }

    @Test
    void constructor_withValidSession_shouldCreateConverter() {
        assertThat(converter).isNotNull();
    }

    @Test
    void constructor_withNullSession_shouldThrowException() {
        assertThatThrownBy(() -> new UrlToPdfConverter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CdpSession cannot be null");
    }

    @Test
    void constructor_withNegativeTimeout_shouldThrowException() {
        assertThatThrownBy(() -> new UrlToPdfConverter(mockSession, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Load timeout must be positive");
    }

    @Test
    void constructor_withZeroTimeout_shouldThrowException() {
        assertThatThrownBy(() -> new UrlToPdfConverter(mockSession, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Load timeout must be positive");
    }

    @Test
    void convert_withNullUrl_shouldThrowException() {
        PdfOptions options = PdfOptions.defaults();

        assertThatThrownBy(() -> converter.convert(null, options))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");
    }

    @Test
    void convert_withEmptyUrl_shouldThrowException() {
        PdfOptions options = PdfOptions.defaults();

        assertThatThrownBy(() -> converter.convert("", options))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");
    }

    @Test
    void convert_withWhitespaceUrl_shouldThrowException() {
        PdfOptions options = PdfOptions.defaults();

        assertThatThrownBy(() -> converter.convert("   ", options))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URL cannot be null or empty");
    }

    @Test
    void convert_withNullOptions_shouldThrowException() {
        assertThatThrownBy(() -> converter.convert(TEST_URL, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PdfOptions cannot be null");
    }

    @Test
    void convert_withDisconnectedSession_shouldThrowException() {
        when(mockSession.isConnected()).thenReturn(false);
        PdfOptions options = PdfOptions.defaults();

        assertThatThrownBy(() -> converter.convert(TEST_URL, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("CDP session is not connected");
    }

    @Test
    void convert_withValidUrl_shouldGeneratePdf() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();
        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(TEST_URL, options);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(new String(result)).startsWith("%PDF-");

        verify(mockPage).enable();
        verify(mockPage).navigate(TEST_URL);
        verify(mockPage).printToPDF(
            eq(false), // landscape
            eq(false), // displayHeaderFooter
            eq(false), // printBackground
            eq(1.0), // scale
            eq(8.5), // paperWidth
            eq(11.0), // paperHeight
            eq(0.4), // marginTop
            eq(0.4), // marginBottom
            eq(0.4), // marginLeft
            eq(0.4), // marginRight
            isNull(), // pageRanges (empty string becomes null)
            eq(false), // ignoreInvalidPageRanges
            isNull(), // headerTemplate (empty string becomes null)
            isNull(), // footerTemplate (empty string becomes null)
            eq(false), // preferCssPageSize
            isNull() // transferMode
        );
    }

    @Test
    //@Disabled("Test passes individually but fails when run with other tests due to Mockito mock state issue - needs investigation")
    void convert_withCustomOptions_shouldUseThem() {
        // Arrange
        PdfOptions options = PdfOptions.builder()
            .landscape(true)
            .printBackground(true)
            .scale(0.8)
            .paperWidth(8.27)
            .paperHeight(11.7)
            .margins(0.5)
            .build();

        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(TEST_URL, options);

        // Assert
        assertThat(result).isNotNull();

        verify(mockPage).printToPDF(
            eq(true), // landscape
            eq(false), // displayHeaderFooter
            eq(true), // printBackground
            eq(0.8), // scale
            eq(8.27), // paperWidth (A4)
            eq(11.7), // paperHeight (A4)
            eq(0.5), // marginTop
            eq(0.5), // marginBottom
            eq(0.5), // marginLeft
            eq(0.5), // marginRight
            isNull(), // pageRanges
            eq(false), // ignoreInvalidPageRanges
            isNull(), // headerTemplate
            isNull(), // footerTemplate
            eq(false), // preferCssPageSize
            isNull() // transferMode
        );
    }

    @Test
    //@Disabled("Test passes individually but fails when run with other tests due to Mockito mock state issue - needs investigation")
    void convert_withDefaultOptions_shouldUseDefaultPdfOptions() {
        // Arrange
        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(TEST_URL);

        // Assert
        assertThat(result).isNotNull();

        verify(mockPage).printToPDF(
            eq(false), // landscape (default)
            eq(false), // displayHeaderFooter (default)
            eq(false), // printBackground (default)
            eq(1.0), // scale (default)
            eq(8.5), // paperWidth (default US Letter)
            eq(11.0), // paperHeight (default US Letter)
            eq(0.4), // marginTop (default)
            eq(0.4), // marginBottom (default)
            eq(0.4), // marginLeft (default)
            eq(0.4), // marginRight (default)
            isNull(), // pageRanges (default empty)
            eq(false), // ignoreInvalidPageRanges
            isNull(), // headerTemplate (default empty)
            isNull(), // footerTemplate (default empty)
            eq(false), // preferCssPageSize (default)
            isNull() // transferMode
        );
    }

    @Test
    void convert_whenNavigationReturnsError_shouldThrowPageLoadException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();
        when(mockNavigateResult.getErrorText()).thenReturn("net::ERR_NAME_NOT_RESOLVED");

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(TEST_URL, options))
            .isInstanceOf(PageLoadException.class)
            .hasMessageContaining("Failed to navigate to URL")
            .hasMessageContaining("net::ERR_NAME_NOT_RESOLVED");
    }

    @Test
    void convert_whenNavigationTimesOut_shouldThrowBrowserTimeoutException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();
        UrlToPdfConverter shortTimeoutConverter = new UrlToPdfConverter(mockSession, 100);

        // Don't setup load event handler - this will cause timeout

        // Act & Assert
        assertThatThrownBy(() -> shortTimeoutConverter.convert(TEST_URL, options))
            .isInstanceOf(BrowserTimeoutException.class)
            .hasMessageContaining("Timeout waiting for page to load")
            .hasMessageContaining(TEST_URL);
    }

    @Test
    void convert_whenPrintToPdfReturnsNull_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        setupNullPrintToPdfMock();
        setupLoadEventHandler();

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(TEST_URL, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Chrome returned null PDF result");
    }

    @Test
    void convert_whenPrintToPdfReturnsEmpty_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        setupEmptyPrintToPdfMock();
        setupLoadEventHandler();

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(TEST_URL, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Chrome returned empty PDF data");
    }

    @Test
    void convert_whenPrintToPdfReturnsInvalidData_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        setupInvalidPrintToPdfMock();
        setupLoadEventHandler();

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(TEST_URL, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("does not appear to be a valid PDF");
    }

    @Test
    void convert_whenPageEnableFails_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        doThrow(new RuntimeException("Page enable failed"))
            .when(mockPage).enable();

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(TEST_URL, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Failed to convert URL to PDF");
    }

    @Test
    void convert_whenNavigationFails_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        when(mockPage.navigate(anyString()))
            .thenThrow(new RuntimeException("Navigation failed"));

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(TEST_URL, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Failed to convert URL to PDF");
    }

    @Test
    //@Disabled("Test passes individually but fails when run with other tests due to Mockito mock state issue - needs investigation")
    void convert_withHttpsUrl_shouldHandleCorrectly() {
        // Arrange
        String httpsUrl = "https://secure.example.com";
        PdfOptions options = PdfOptions.defaults();

        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(httpsUrl, options);

        // Assert
        assertThat(result).isNotNull();
        verify(mockPage).navigate(httpsUrl);
    }

    @Test
    void convert_withHttpUrl_shouldHandleCorrectly() {
        // Arrange
        String httpUrl = "http://example.com";
        PdfOptions options = PdfOptions.defaults();

        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(httpUrl, options);

        // Assert
        assertThat(result).isNotNull();
        verify(mockPage).navigate(httpUrl);
    }

    @Test
    void convert_withHeaderAndFooter_shouldPassTemplates() {
        // Arrange
        PdfOptions options = PdfOptions.builder()
            .displayHeaderFooter(true)
            .headerTemplate("<div>Header</div>")
            .footerTemplate("<div>Footer</div>")
            .build();

        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(TEST_URL, options);

        // Assert
        assertThat(result).isNotNull();

        verify(mockPage).printToPDF(
            anyBoolean(),
            eq(true), // displayHeaderFooter
            anyBoolean(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            any(), anyBoolean(),
            eq("<div>Header</div>"), // headerTemplate
            eq("<div>Footer</div>"), // footerTemplate
            anyBoolean(), any()
        );
    }

    @Test
    void convert_withPageRanges_shouldPassToPrintToPdf() {
        // Arrange
        PdfOptions options = PdfOptions.builder()
            .pageRanges("1-3, 5")
            .build();

        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(TEST_URL, options);

        // Assert
        assertThat(result).isNotNull();

        verify(mockPage).printToPDF(
            anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(),
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), anyDouble(),
            eq("1-3, 5"), // pageRanges
            anyBoolean(), any(), any(), anyBoolean(), any()
        );
    }

    @Test
    void convert_withUrlPath_shouldHandleCorrectly() {
        // Arrange
        String urlWithPath = "https://example.com/path/to/page";
        PdfOptions options = PdfOptions.defaults();

        setupValidPrintToPdfMock();
        setupLoadEventHandler();

        // Act
        byte[] result = converter.convert(urlWithPath, options);

        // Assert
        assertThat(result).isNotNull();
        verify(mockPage).navigate(urlWithPath);
    }
}
