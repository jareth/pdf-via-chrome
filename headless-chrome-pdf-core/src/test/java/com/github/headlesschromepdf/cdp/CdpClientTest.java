package com.github.headlesschromepdf.cdp;

import com.github.headlesschromepdf.chrome.ChromeProcess;
import com.github.headlesschromepdf.exception.CdpConnectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdpClientTest {

    private static final String VALID_WS_URL = "ws://localhost:9222/devtools/page/12345";

    @Mock
    private ChromeProcess mockChromeProcess;

    @Test
    void createSession_withNullChromeProcess_shouldThrowException() {
        assertThatThrownBy(() -> CdpClient.createSession((ChromeProcess) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ChromeProcess cannot be null");
    }

    @Test
    void createSession_withNotAliveChromeProcess_shouldThrowException() {
        when(mockChromeProcess.isAlive()).thenReturn(false);

        assertThatThrownBy(() -> CdpClient.createSession(mockChromeProcess))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chrome process is not running");
    }

    @Test
    void createSession_withNullWebSocketUrl_shouldThrowException() {
        assertThatThrownBy(() -> CdpClient.createSession((String) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WebSocket URL cannot be null");
    }

    @Test
    void createSession_withEmptyWebSocketUrl_shouldThrowException() {
        assertThatThrownBy(() -> CdpClient.createSession(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WebSocket URL cannot be null or empty");
    }

    @Test
    void createSession_withInvalidUrl_shouldThrowException() {
        assertThatThrownBy(() -> CdpClient.createSession("invalid-url"))
            .isInstanceOf(CdpConnectionException.class);
    }

    @Test
    void createSession_withNonWebSocketUrl_shouldThrowException() {
        assertThatThrownBy(() -> CdpClient.createSession("http://localhost:9222/devtools/page/123"))
            .isInstanceOf(CdpConnectionException.class);
    }

    @Test
    void builder_withoutUrlOrProcess_shouldThrowException() {
        CdpClient.Builder builder = CdpClient.builder();

        assertThatThrownBy(() -> builder.build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Either webSocketUrl or chromeProcess must be set");
    }

    @Test
    void builder_withWebSocketUrl_shouldCreateSession() {
        CdpClient.Builder builder = CdpClient.builder()
            .webSocketUrl(VALID_WS_URL);

        assertThatThrownBy(() -> builder.build())
            .isInstanceOf(CdpConnectionException.class);
    }

    @Test
    void builder_withChromeProcess_shouldCreateSession() {
        when(mockChromeProcess.isAlive()).thenReturn(true);
        when(mockChromeProcess.getWebSocketDebuggerUrl()).thenReturn(VALID_WS_URL);

        CdpClient.Builder builder = CdpClient.builder()
            .chromeProcess(mockChromeProcess);

        assertThatThrownBy(() -> builder.build())
            .isInstanceOf(CdpConnectionException.class);
    }

    @Test
    void builder_fluentApi_shouldWork() {
        CdpClient.Builder builder = CdpClient.builder()
            .webSocketUrl(VALID_WS_URL);

        assertThat(builder).isNotNull();
    }

    @Test
    void builder_chromeProcessTakesPrecedence_shouldUseProcessUrl() {
        when(mockChromeProcess.isAlive()).thenReturn(true);
        when(mockChromeProcess.getWebSocketDebuggerUrl()).thenReturn(VALID_WS_URL);

        CdpClient.Builder builder = CdpClient.builder()
            .webSocketUrl("ws://localhost:9999/different")
            .chromeProcess(mockChromeProcess);

        assertThatThrownBy(() -> builder.build())
            .isInstanceOf(CdpConnectionException.class);

        verify(mockChromeProcess).getWebSocketDebuggerUrl();
    }
}
