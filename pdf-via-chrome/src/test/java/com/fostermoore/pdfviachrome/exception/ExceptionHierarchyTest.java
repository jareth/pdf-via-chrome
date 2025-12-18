package com.fostermoore.pdfviachrome.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the exception hierarchy.
 * Tests construction, inheritance, and proper message/cause handling.
 */
class ExceptionHierarchyTest {

    // PdfGenerationException Tests

    @Test
    void pdfGenerationException_withMessage_shouldSetMessage() {
        String message = "Test error message";
        PdfGenerationException exception = new PdfGenerationException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void pdfGenerationException_withMessageAndCause_shouldSetBoth() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        PdfGenerationException exception = new PdfGenerationException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void pdfGenerationException_withCause_shouldSetCause() {
        Throwable cause = new RuntimeException("Root cause");
        PdfGenerationException exception = new PdfGenerationException(cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getMessage()).contains("Root cause");
    }

    @Test
    void pdfGenerationException_extendsRuntimeException() {
        PdfGenerationException exception = new PdfGenerationException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    // ChromeNotFoundException Tests

    @Test
    void chromeNotFoundException_withMessage_shouldSetMessage() {
        String message = "Chrome not found";
        ChromeNotFoundException exception = new ChromeNotFoundException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void chromeNotFoundException_withMessageAndCause_shouldSetBoth() {
        String message = "Chrome not found";
        Throwable cause = new RuntimeException("Root cause");
        ChromeNotFoundException exception = new ChromeNotFoundException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void chromeNotFoundException_withCause_shouldSetCause() {
        Throwable cause = new RuntimeException("Root cause");
        ChromeNotFoundException exception = new ChromeNotFoundException(cause);

        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void chromeNotFoundException_extendsPdfGenerationException() {
        ChromeNotFoundException exception = new ChromeNotFoundException("test");

        assertThat(exception).isInstanceOf(PdfGenerationException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    // BrowserTimeoutException Tests

    @Test
    void browserTimeoutException_withMessage_shouldSetMessage() {
        String message = "Browser timeout";
        BrowserTimeoutException exception = new BrowserTimeoutException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void browserTimeoutException_withMessageAndCause_shouldSetBoth() {
        String message = "Browser timeout";
        Throwable cause = new RuntimeException("Root cause");
        BrowserTimeoutException exception = new BrowserTimeoutException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void browserTimeoutException_withCause_shouldSetCause() {
        Throwable cause = new RuntimeException("Root cause");
        BrowserTimeoutException exception = new BrowserTimeoutException(cause);

        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void browserTimeoutException_extendsPdfGenerationException() {
        BrowserTimeoutException exception = new BrowserTimeoutException("test");

        assertThat(exception).isInstanceOf(PdfGenerationException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    // PageLoadException Tests

    @Test
    void pageLoadException_withMessage_shouldSetMessage() {
        String message = "Page load failed";
        PageLoadException exception = new PageLoadException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void pageLoadException_withMessageAndCause_shouldSetBoth() {
        String message = "Page load failed";
        Throwable cause = new RuntimeException("Root cause");
        PageLoadException exception = new PageLoadException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void pageLoadException_withCause_shouldSetCause() {
        Throwable cause = new RuntimeException("Root cause");
        PageLoadException exception = new PageLoadException(cause);

        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void pageLoadException_extendsPdfGenerationException() {
        PageLoadException exception = new PageLoadException("test");

        assertThat(exception).isInstanceOf(PdfGenerationException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    // CdpConnectionException Tests

    @Test
    void cdpConnectionException_withMessage_shouldSetMessage() {
        String message = "CDP connection failed";
        CdpConnectionException exception = new CdpConnectionException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void cdpConnectionException_withMessageAndCause_shouldSetBoth() {
        String message = "CDP connection failed";
        Throwable cause = new RuntimeException("Root cause");
        CdpConnectionException exception = new CdpConnectionException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void cdpConnectionException_withCause_shouldSetCause() {
        Throwable cause = new RuntimeException("Root cause");
        CdpConnectionException exception = new CdpConnectionException(cause);

        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void cdpConnectionException_extendsPdfGenerationException() {
        CdpConnectionException exception = new CdpConnectionException("test");

        assertThat(exception).isInstanceOf(PdfGenerationException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    // Hierarchy Tests

    @Test
    void catchPdfGenerationException_shouldCatchAllSubclasses() {
        // This test verifies that all specific exceptions can be caught by the base exception

        try {
            throw new ChromeNotFoundException("test");
        } catch (PdfGenerationException e) {
            assertThat(e).isInstanceOf(ChromeNotFoundException.class);
        }

        try {
            throw new BrowserTimeoutException("test");
        } catch (PdfGenerationException e) {
            assertThat(e).isInstanceOf(BrowserTimeoutException.class);
        }

        try {
            throw new PageLoadException("test");
        } catch (PdfGenerationException e) {
            assertThat(e).isInstanceOf(PageLoadException.class);
        }

        try {
            throw new CdpConnectionException("test");
        } catch (PdfGenerationException e) {
            assertThat(e).isInstanceOf(CdpConnectionException.class);
        }
    }

    @Test
    void exceptionTypes_areDistinct() {
        ChromeNotFoundException chrome = new ChromeNotFoundException("test");
        BrowserTimeoutException timeout = new BrowserTimeoutException("test");
        PageLoadException pageLoad = new PageLoadException("test");
        CdpConnectionException cdp = new CdpConnectionException("test");

        // Verify they're not instances of each other
        assertThat(chrome).isNotInstanceOf(BrowserTimeoutException.class);
        assertThat(chrome).isNotInstanceOf(PageLoadException.class);
        assertThat(chrome).isNotInstanceOf(CdpConnectionException.class);

        assertThat(timeout).isNotInstanceOf(ChromeNotFoundException.class);
        assertThat(timeout).isNotInstanceOf(PageLoadException.class);
        assertThat(timeout).isNotInstanceOf(CdpConnectionException.class);

        assertThat(pageLoad).isNotInstanceOf(ChromeNotFoundException.class);
        assertThat(pageLoad).isNotInstanceOf(BrowserTimeoutException.class);
        assertThat(pageLoad).isNotInstanceOf(CdpConnectionException.class);

        assertThat(cdp).isNotInstanceOf(ChromeNotFoundException.class);
        assertThat(cdp).isNotInstanceOf(BrowserTimeoutException.class);
        assertThat(cdp).isNotInstanceOf(PageLoadException.class);
    }
}
