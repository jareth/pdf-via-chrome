package com.fostermoore.pdfviachrome.converter;

import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.types.page.FrameTree;
import com.github.kklisura.cdt.protocol.types.page.Frame;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.events.page.DomContentEventFired;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HtmlToPdfConverterTest {

    private static final String SIMPLE_HTML = "<html><body><h1>Test</h1></body></html>";
    private static final String VALID_PDF_BASE64 = Base64.getEncoder().encodeToString("%PDF-1.4\n%Test PDF".getBytes());

    @Mock
    private CdpSession mockSession;

    @Mock
    private Page mockPage;

    @Mock
    private FrameTree mockFrameTree;

    @Mock
    private Frame mockFrame;

    private HtmlToPdfConverter converter;

    @BeforeEach
    void setUp() {
        converter = new HtmlToPdfConverter(mockSession);

        // Setup common mock behaviors (lenient to avoid unnecessary stubbing warnings)
        lenient().when(mockSession.isConnected()).thenReturn(true);
        lenient().when(mockSession.getPage()).thenReturn(mockPage);
        lenient().when(mockPage.getFrameTree()).thenReturn(mockFrameTree);
        lenient().when(mockFrameTree.getFrame()).thenReturn(mockFrame);
        lenient().when(mockFrame.getId()).thenReturn("frame123");
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
     * Helper method to setup DOM content loaded event handler.
     */
    private void setupDomContentLoadedHandler() {
        doAnswer(invocation -> {
            // Get the event handler that was registered
            Object handler = invocation.getArgument(0);
            // Immediately invoke it using reflection to trigger the countDown
            try {
                Method method = handler.getClass().getMethod("onEvent", Object.class);
                method.invoke(handler, new DomContentEventFired());
            } catch (Exception e) {
                // Log or ignore - the test might fail with timeout
                System.err.println("Failed to invoke event handler: " + e.getMessage());
            }
            return null;
        }).when(mockPage).onDomContentEventFired(any());
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
        assertThatThrownBy(() -> new HtmlToPdfConverter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CdpSession cannot be null");
    }

    @Test
    void constructor_withNegativeTimeout_shouldThrowException() {
        assertThatThrownBy(() -> new HtmlToPdfConverter(mockSession, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Load timeout must be positive");
    }

    @Test
    void constructor_withZeroTimeout_shouldThrowException() {
        assertThatThrownBy(() -> new HtmlToPdfConverter(mockSession, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Load timeout must be positive");
    }

    @Test
    void convert_withNullHtml_shouldThrowException() {
        PdfOptions options = PdfOptions.defaults();

        assertThatThrownBy(() -> converter.convert(null, options))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("HTML content cannot be null");
    }

    @Test
    void convert_withNullOptions_shouldThrowException() {
        assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PdfOptions cannot be null");
    }

    @Test
    void convert_withDisconnectedSession_shouldThrowException() {
        when(mockSession.isConnected()).thenReturn(false);
        PdfOptions options = PdfOptions.defaults();

        assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("CDP session is not connected");
    }

    @Test
    void convert_withValidHtml_shouldGeneratePdf() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();
        setupValidPrintToPdfMock();
        setupDomContentLoadedHandler();

        // Act
        byte[] result = converter.convert(SIMPLE_HTML, options);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(new String(result)).startsWith("%PDF-");

        verify(mockPage).enable();
        verify(mockPage).navigate("about:blank");
        verify(mockPage).setDocumentContent("frame123", SIMPLE_HTML);
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
        setupDomContentLoadedHandler();

        // Act
        byte[] result = converter.convert(SIMPLE_HTML, options);

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
    void convert_withDefaultOptions_shouldUseDefaultPdfOptions() {
        // Arrange
        setupValidPrintToPdfMock();
        setupDomContentLoadedHandler();

        // Act
        byte[] result = converter.convert(SIMPLE_HTML);

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
    void convert_withEmptyHtml_shouldGeneratePdf() {
        // Arrange
        String emptyHtml = "";
        PdfOptions options = PdfOptions.defaults();

        setupValidPrintToPdfMock();
        setupDomContentLoadedHandler();

        // Act
        byte[] result = converter.convert(emptyHtml, options);

        // Assert
        assertThat(result).isNotNull();
        verify(mockPage).setDocumentContent("frame123", emptyHtml);
    }

    @Test
    void convert_whenPrintToPdfReturnsNull_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        setupNullPrintToPdfMock();
        setupDomContentLoadedHandler();

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Chrome returned null PDF result");
    }

    @Test
    void convert_whenPrintToPdfReturnsEmpty_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        setupEmptyPrintToPdfMock();
        setupDomContentLoadedHandler();

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Chrome returned empty PDF data");
    }

    @Test
    void convert_whenPrintToPdfReturnsInvalidData_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        setupInvalidPrintToPdfMock();
        setupDomContentLoadedHandler();

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, options))
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
        assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Failed to convert HTML to PDF");
    }

    @Test
    void convert_whenNavigationFails_shouldThrowException() {
        // Arrange
        PdfOptions options = PdfOptions.defaults();

        doThrow(new RuntimeException("Navigation failed"))
            .when(mockPage).navigate(anyString());

        // Act & Assert
        assertThatThrownBy(() -> converter.convert(SIMPLE_HTML, options))
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Failed to convert HTML to PDF");
    }

    @Test
    void convert_withLargeHtml_shouldHandleCorrectly() {
        // Arrange
        StringBuilder largeHtml = new StringBuilder("<html><body>");
        for (int i = 0; i < 10000; i++) {
            largeHtml.append("<p>Line ").append(i).append("</p>");
        }
        largeHtml.append("</body></html>");

        PdfOptions options = PdfOptions.defaults();

        setupValidPrintToPdfMock();
        setupDomContentLoadedHandler();

        // Act
        byte[] result = converter.convert(largeHtml.toString(), options);

        // Assert
        assertThat(result).isNotNull();
        verify(mockPage).setDocumentContent(eq("frame123"), eq(largeHtml.toString()));
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
        setupDomContentLoadedHandler();

        // Act
        byte[] result = converter.convert(SIMPLE_HTML, options);

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
        setupDomContentLoadedHandler();

        // Act
        byte[] result = converter.convert(SIMPLE_HTML, options);

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
}
