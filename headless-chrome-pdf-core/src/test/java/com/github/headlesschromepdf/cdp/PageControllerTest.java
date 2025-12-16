package com.github.headlesschromepdf.cdp;

import com.github.headlesschromepdf.exception.BrowserTimeoutException;
import com.github.headlesschromepdf.exception.PageLoadException;
import com.github.headlesschromepdf.exception.PdfGenerationException;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.events.page.DomContentEventFired;
import com.github.kklisura.cdt.protocol.events.page.LoadEventFired;
import com.github.kklisura.cdt.protocol.types.page.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PageController.
 */
@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    @Mock
    private CdpSession mockSession;

    @Mock
    private Page mockPage;

    private PageController pageController;

    @BeforeEach
    void setUp() {
        lenient().when(mockSession.getPage()).thenReturn(mockPage);
        pageController = new PageController(mockSession);
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

    @Test
    void constructor_withValidSession_shouldSucceed() {
        PageController controller = new PageController(mockSession);
        assertThat(controller).isNotNull();
        assertThat(controller.getPageLoadTimeoutMs()).isEqualTo(30000);
    }

    @Test
    void constructor_withCustomTimeout_shouldSetTimeout() {
        PageController controller = new PageController(mockSession, 60000);
        assertThat(controller).isNotNull();
        assertThat(controller.getPageLoadTimeoutMs()).isEqualTo(60000);
    }

    @Test
    void constructor_withNullSession_shouldThrowException() {
        assertThatThrownBy(() -> new PageController(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("CdpSession cannot be null");
    }

    @Test
    void constructor_withZeroTimeout_shouldThrowException() {
        assertThatThrownBy(() -> new PageController(mockSession, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Page load timeout must be positive");
    }

    @Test
    void constructor_withNegativeTimeout_shouldThrowException() {
        assertThatThrownBy(() -> new PageController(mockSession, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Page load timeout must be positive");
    }

    @Test
    void navigateToUrl_withValidUrl_shouldSucceed() {
        // Setup
        String url = "https://example.com";
        Navigate navigateResult = new Navigate();
        navigateResult.setErrorText(null);

        when(mockPage.navigate(url)).thenReturn(navigateResult);
        setupDomContentLoadedHandler();

        // Execute
        pageController.navigateToUrl(url);

        // Verify
        verify(mockPage).enable();
        verify(mockPage).navigate(url);
        verify(mockPage).onDomContentEventFired(any());
    }

    @Test
    void navigateToUrl_withNullUrl_shouldThrowException() {
        assertThatThrownBy(() -> pageController.navigateToUrl(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("URL cannot be null or empty");
    }

    @Test
    void navigateToUrl_withEmptyUrl_shouldThrowException() {
        assertThatThrownBy(() -> pageController.navigateToUrl(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("URL cannot be null or empty");
    }

    @Test
    void navigateToUrl_withNavigationError_shouldThrowPageLoadException() {
        // Setup
        String url = "https://invalid.example.com";
        Navigate navigateResult = new Navigate();
        navigateResult.setErrorText("net::ERR_NAME_NOT_RESOLVED");

        when(mockPage.navigate(url)).thenReturn(navigateResult);

        // Execute & Verify
        assertThatThrownBy(() -> pageController.navigateToUrl(url))
            .isInstanceOf(PageLoadException.class)
            .hasMessageContaining("Failed to navigate to URL")
            .hasMessageContaining("net::ERR_NAME_NOT_RESOLVED");

        verify(mockPage).enable();
        verify(mockPage).navigate(url);
    }

    @Test
    void navigateToUrl_withTimeout_shouldThrowBrowserTimeoutException() {
        // Setup - create controller with very short timeout
        PageController shortTimeoutController = new PageController(mockSession, 100);
        String url = "https://example.com";

        Navigate navigateResult = new Navigate();
        navigateResult.setErrorText(null);

        when(mockPage.navigate(url)).thenReturn(navigateResult);

        // Don't trigger the DOMContentLoaded event - let it timeout

        // Execute & Verify
        assertThatThrownBy(() -> shortTimeoutController.navigateToUrl(url))
            .isInstanceOf(BrowserTimeoutException.class)
            .hasMessageContaining("Page load timed out after 100 ms");

        verify(mockPage).enable();
        verify(mockPage).navigate(url);
    }

    @Test
    void navigateToUrl_withException_shouldThrowPageLoadException() {
        // Setup
        String url = "https://example.com";
        when(mockPage.navigate(url)).thenThrow(new RuntimeException("CDP error"));

        // Execute & Verify
        assertThatThrownBy(() -> pageController.navigateToUrl(url))
            .isInstanceOf(PageLoadException.class)
            .hasMessageContaining("Failed to navigate to URL")
            .hasCauseInstanceOf(RuntimeException.class);

        verify(mockPage).enable();
        verify(mockPage).navigate(url);
    }

    @Test
    void loadHtmlContent_withValidHtml_shouldSucceed() {
        // Setup
        String html = "<html><body><h1>Test</h1></body></html>";

        Navigate navigateResult = new Navigate();
        navigateResult.setErrorText(null);

        when(mockPage.navigate(anyString())).thenReturn(navigateResult);
        setupDomContentLoadedHandler();

        // Execute
        pageController.loadHtmlContent(html);

        // Verify
        verify(mockPage).enable();
        verify(mockPage).navigate(contains("data:text/html,"));
    }

    @Test
    void loadHtmlContent_withNullHtml_shouldThrowException() {
        assertThatThrownBy(() -> pageController.loadHtmlContent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("HTML content cannot be null");
    }

    @Test
    void setDocumentContent_withValidHtml_shouldSucceed() {
        // Setup
        String html = "<html><body><h1>Test</h1></body></html>";

        Frame frame = new Frame();
        frame.setId("frame-id-123");

        FrameTree frameTree = new FrameTree();
        frameTree.setFrame(frame);

        when(mockPage.getFrameTree()).thenReturn(frameTree);
        setupDomContentLoadedHandler();

        // Execute
        pageController.setDocumentContent(html);

        // Verify
        verify(mockPage).enable();
        verify(mockPage).navigate("about:blank");
        verify(mockPage).getFrameTree();
        verify(mockPage).setDocumentContent("frame-id-123", html);
    }

    @Test
    void setDocumentContent_withNullHtml_shouldThrowException() {
        assertThatThrownBy(() -> pageController.setDocumentContent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("HTML content cannot be null");
    }

    @Test
    void setDocumentContent_withTimeout_shouldThrowBrowserTimeoutException() {
        // Setup - create controller with very short timeout
        PageController shortTimeoutController = new PageController(mockSession, 100);
        String html = "<html><body>Test</body></html>";

        Frame frame = new Frame();
        frame.setId("frame-id-123");

        FrameTree frameTree = new FrameTree();
        frameTree.setFrame(frame);

        when(mockPage.getFrameTree()).thenReturn(frameTree);

        // Don't trigger the DOMContentLoaded event - let it timeout

        // Execute & Verify
        assertThatThrownBy(() -> shortTimeoutController.setDocumentContent(html))
            .isInstanceOf(BrowserTimeoutException.class)
            .hasMessageContaining("Page load timed out after 100 ms when setting document content");

        verify(mockPage).enable();
        verify(mockPage).navigate("about:blank");
        verify(mockPage).setDocumentContent("frame-id-123", html);
    }

    @Test
    void generatePdf_withDefaults_shouldReturnPdfBytes() {
        // Setup
        String testPdfContent = "PDF content";
        String base64Pdf = Base64.getEncoder().encodeToString(testPdfContent.getBytes());

        PrintToPDF mockResult = mock(PrintToPDF.class);
        when(mockResult.getData()).thenReturn(base64Pdf);

        when(mockPage.printToPDF(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockResult);

        // Execute
        byte[] result = pageController.generatePdf();

        // Verify
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testPdfContent.getBytes());
        verify(mockPage).printToPDF(
            eq(false), // landscape
            eq(false), // displayHeaderFooter
            eq(false), // printBackground
            eq(1.0),   // scale
            eq(null),  // paperWidth
            eq(null),  // paperHeight
            eq(null),  // marginTop
            eq(null),  // marginBottom
            eq(null),  // marginLeft
            eq(null),  // marginRight
            eq(null),  // pageRanges
            eq(false), // ignoreInvalidPageRanges
            eq(null),  // headerTemplate
            eq(null),  // footerTemplate
            eq(false), // preferCSSPageSize
            eq(null)   // transferMode
        );
    }

    @Test
    void generatePdf_withEmptyData_shouldThrowException() {
        // Setup
        PrintToPDF mockResult = mock(PrintToPDF.class);
        when(mockResult.getData()).thenReturn("");

        when(mockPage.printToPDF(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockResult);

        // Execute & Verify
        assertThatThrownBy(() -> pageController.generatePdf())
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("PDF generation returned empty data");
    }

    @Test
    void generatePdf_withNullData_shouldThrowException() {
        // Setup
        PrintToPDF mockResult = mock(PrintToPDF.class);
        when(mockResult.getData()).thenReturn(null);

        when(mockPage.printToPDF(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockResult);

        // Execute & Verify
        assertThatThrownBy(() -> pageController.generatePdf())
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("PDF generation returned empty data");
    }

    @Test
    void generatePdf_withNullResult_shouldThrowException() {
        // Setup
        when(mockPage.printToPDF(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(null);

        // Execute & Verify
        assertThatThrownBy(() -> pageController.generatePdf())
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("PDF generation returned null result");
    }

    @Test
    void generatePdf_withInvalidBase64_shouldThrowException() {
        // Setup
        PrintToPDF mockResult = mock(PrintToPDF.class);
        when(mockResult.getData()).thenReturn("invalid-base64!!!");

        when(mockPage.printToPDF(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(mockResult);

        // Execute & Verify
        assertThatThrownBy(() -> pageController.generatePdf())
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Failed to decode PDF data")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatePdf_withException_shouldThrowPdfGenerationException() {
        // Setup
        when(mockPage.printToPDF(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any(), any(), any(), any()
        )).thenThrow(new RuntimeException("CDP error"));

        // Execute & Verify
        assertThatThrownBy(() -> pageController.generatePdf())
            .isInstanceOf(PdfGenerationException.class)
            .hasMessageContaining("Failed to generate PDF")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void getPageLoadTimeoutMs_shouldReturnConfiguredTimeout() {
        PageController controller = new PageController(mockSession, 45000);
        assertThat(controller.getPageLoadTimeoutMs()).isEqualTo(45000);
    }
}
