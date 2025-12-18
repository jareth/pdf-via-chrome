package com.fostermoore.pdfviachrome.cdp;

import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Emulation;
import com.github.kklisura.cdt.protocol.commands.DOM;
import com.github.kklisura.cdt.protocol.commands.Performance;
import com.github.kklisura.cdt.protocol.commands.Security;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdpSessionTest {

    private static final String VALID_WS_URL = "ws://localhost:9222/devtools/page/12345";

    @Mock
    private ChromeDevToolsService mockDevToolsService;

    @Mock
    private Page mockPage;

    @Mock
    private com.github.kklisura.cdt.protocol.commands.Runtime mockRuntime;

    @Mock
    private Network mockNetwork;

    @Mock
    private Emulation mockEmulation;

    @Mock
    private DOM mockDOM;

    @Mock
    private Performance mockPerformance;

    @Mock
    private Security mockSecurity;

    private CdpSession session;

    @BeforeEach
    void setUp() {
        session = new CdpSession(VALID_WS_URL);
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    void constructor_withValidUrl_shouldCreateSession() {
        assertThat(session).isNotNull();
        assertThat(session.getWebSocketUrl()).isEqualTo(VALID_WS_URL);
    }

    @Test
    void constructor_withNullUrl_shouldThrowException() {
        assertThatThrownBy(() -> new CdpSession(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WebSocket URL cannot be null");
    }

    @Test
    void constructor_withEmptyUrl_shouldThrowException() {
        assertThatThrownBy(() -> new CdpSession(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WebSocket URL cannot be null or empty");
    }

    @Test
    void isConnected_beforeConnect_shouldReturnFalse() {
        assertThat(session.isConnected()).isFalse();
    }

    @Test
    void getPage_beforeConnect_shouldThrowException() {
        assertThatThrownBy(() -> session.getPage())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected");
    }

    @Test
    void getRuntime_beforeConnect_shouldThrowException() {
        assertThatThrownBy(() -> session.getRuntime())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected");
    }

    @Test
    void getNetwork_beforeConnect_shouldThrowException() {
        assertThatThrownBy(() -> session.getNetwork())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected");
    }

    @Test
    void getEmulation_beforeConnect_shouldThrowException() {
        assertThatThrownBy(() -> session.getEmulation())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected");
    }

    @Test
    void getDOM_beforeConnect_shouldThrowException() {
        assertThatThrownBy(() -> session.getDOM())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected");
    }

    @Test
    void getPerformance_beforeConnect_shouldThrowException() {
        assertThatThrownBy(() -> session.getPerformance())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected");
    }

    @Test
    void getSecurity_beforeConnect_shouldThrowException() {
        assertThatThrownBy(() -> session.getSecurity())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected");
    }

    @Test
    void close_withoutConnect_shouldNotThrowException() {
        assertThatCode(() -> session.close()).doesNotThrowAnyException();
    }

    @Test
    void close_multipleTimes_shouldBeIdempotent() {
        mockConnectedSession();

        assertThatCode(() -> {
            session.close();
            session.close();
            session.close();
        }).doesNotThrowAnyException();

        verify(mockDevToolsService, times(1)).close();
    }

    @Test
    void isConnected_afterClose_shouldReturnFalse() {
        mockConnectedSession();
        session.close();

        assertThat(session.isConnected()).isFalse();
    }

    @Test
    void getPage_afterClose_shouldThrowException() {
        mockConnectedSession();
        session.close();

        assertThatThrownBy(() -> session.getPage())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("has been closed");
    }

    @Test
    void tryWithResources_shouldAutoClose() throws Exception {
        ChromeDevToolsService devTools = mockDevToolsService;

        CdpSession autoCloseSession = new CdpSession(VALID_WS_URL);
        injectMockDevToolsService(autoCloseSession, devTools);

        try (CdpSession s = autoCloseSession) {
            assertThat(s.isConnected()).isTrue();
        }

        verify(devTools).close();
    }

    @Test
    void getDomains_whenConnected_shouldReturnDomains() {
        mockConnectedSession();

        assertThat(session.getPage()).isEqualTo(mockPage);
        assertThat(session.getRuntime()).isEqualTo(mockRuntime);
        assertThat(session.getNetwork()).isEqualTo(mockNetwork);
        assertThat(session.getEmulation()).isEqualTo(mockEmulation);
        assertThat(session.getDOM()).isEqualTo(mockDOM);
        assertThat(session.getPerformance()).isEqualTo(mockPerformance);
        assertThat(session.getSecurity()).isEqualTo(mockSecurity);
    }

    @Test
    void concurrentAccess_shouldBeThreadSafe() throws InterruptedException {
        mockConnectedSession();

        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        session.getPage();
                        session.getRuntime();
                        session.getNetwork();
                        session.isConnected();
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Thread-safety issues would cause exceptions
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    void concurrentClose_shouldBeThreadSafe() throws InterruptedException {
        mockConnectedSession();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    session.close();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Close should only be called once on the underlying service
        verify(mockDevToolsService, times(1)).close();
    }

    @Test
    void close_withExceptionInDevToolsClose_shouldHandleGracefully() {
        mockConnectedSession();
        doThrow(new RuntimeException("Test exception")).when(mockDevToolsService).close();

        assertThatCode(() -> session.close()).doesNotThrowAnyException();
        assertThat(session.isConnected()).isFalse();
    }

    @Test
    void connect_afterClose_shouldThrowException() {
        session.close();

        assertThatThrownBy(() -> session.connect())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("has been closed");
    }

    /**
     * Helper method to inject a mock ChromeDevToolsService into a session.
     * This simulates a connected session for testing.
     */
    private void injectMockDevToolsService(CdpSession session, ChromeDevToolsService service) {
        try {
            Field field = CdpSession.class.getDeclaredField("devToolsService");
            field.setAccessible(true);
            field.set(session, service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock service", e);
        }
    }

    /**
     * Helper method to mock a connected session with all domains.
     */
    private void mockConnectedSession() {
        lenient().when(mockDevToolsService.getPage()).thenReturn(mockPage);
        lenient().when(mockDevToolsService.getRuntime()).thenReturn(mockRuntime);
        lenient().when(mockDevToolsService.getNetwork()).thenReturn(mockNetwork);
        lenient().when(mockDevToolsService.getEmulation()).thenReturn(mockEmulation);
        lenient().when(mockDevToolsService.getDOM()).thenReturn(mockDOM);
        lenient().when(mockDevToolsService.getPerformance()).thenReturn(mockPerformance);
        lenient().when(mockDevToolsService.getSecurity()).thenReturn(mockSecurity);

        injectMockDevToolsService(session, mockDevToolsService);
    }
}
