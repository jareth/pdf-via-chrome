package com.fostermoore.pdfviachrome.cdp;

import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import com.fostermoore.pdfviachrome.exception.CdpConnectionException;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for CdpSession that connect to a real Chrome instance.
 *
 * These tests require Chrome to be installed and accessible on the system.
 * They can be skipped by not setting the RUN_INTEGRATION_TESTS environment variable.
 */
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true", disabledReason = "Integration tests disabled")
class CdpSessionIntegrationTest {

    @Test
    void connect_toRealChrome_shouldEstablishConnection() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                assertThat(session.isConnected()).isTrue();
                assertThat(session.getWebSocketUrl()).isNotEmpty();
            }
        }
    }

    @Test
    void getDomains_whenConnected_shouldReturnWorkingDomains() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                // Test that domains are accessible
                Page page = session.getPage();
                assertThat(page).isNotNull();

                com.github.kklisura.cdt.protocol.commands.Runtime runtime = session.getRuntime();
                assertThat(runtime).isNotNull();

                Network network = session.getNetwork();
                assertThat(network).isNotNull();

                // Enable domains to verify they work
                page.enable();
                runtime.enable();
                network.enable();
            }
        }
    }

    @Test
    void connect_withInvalidUrl_shouldThrowException() {
        CdpSession session = new CdpSession("ws://localhost:99999/invalid");

        assertThatThrownBy(() -> session.connect())
            .isInstanceOf(CdpConnectionException.class);
    }

    @Test
    void connect_withNonWebSocketUrl_shouldThrowException() {
        CdpSession session = new CdpSession("http://localhost:9222/devtools/page/123");

        assertThatThrownBy(() -> session.connect())
            .isInstanceOf(CdpConnectionException.class)
            .hasMessageContaining("Invalid WebSocket URL scheme");
    }

    @Test
    void connect_withMalformedUrl_shouldThrowException() {
        CdpSession session = new CdpSession("not-a-valid-url");

        assertThatThrownBy(() -> session.connect())
            .isInstanceOf(CdpConnectionException.class)
            .hasMessageContaining("Invalid WebSocket URL format");
    }

    @Test
    void close_shouldCleanupResources() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl());
            session.connect();

            assertThat(session.isConnected()).isTrue();

            session.close();

            assertThat(session.isConnected()).isFalse();

            // Verify that accessing domains after close throws exception
            assertThatThrownBy(() -> session.getPage())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has been closed");
        }
    }

    @Test
    void tryWithResources_shouldAutoCloseSession() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        CdpSession session;

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession s = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                s.connect();
                session = s;
                assertThat(session.isConnected()).isTrue();
            }

            // After try-with-resources, session should be closed
            assertThat(session.isConnected()).isFalse();
        }
    }

    @Test
    void cdpClient_createSession_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = CdpClient.createSession(chromeProcess)) {
                assertThat(session.isConnected()).isTrue();
                assertThat(session.getPage()).isNotNull();
            }
        }
    }

    @Test
    void cdpClient_builder_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = CdpClient.builder()
                    .chromeProcess(chromeProcess)
                    .build()) {
                assertThat(session.isConnected()).isTrue();
                assertThat(session.getPage()).isNotNull();
            }
        }
    }

    @Test
    void concurrentSessions_shouldWorkIndependently() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            int sessionCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(sessionCount);
            CountDownLatch latch = new CountDownLatch(sessionCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < sessionCount; i++) {
                executor.submit(() -> {
                    try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                        session.connect();

                        // Perform some operations
                        session.getPage().enable();
                        session.getRuntime().enable();

                        assertThat(session.isConnected()).isTrue();

                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(sessionCount);
        }
    }

    @Test
    void navigationAndEvaluation_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                com.github.kklisura.cdt.protocol.commands.Runtime runtime = session.getRuntime();

                page.enable();
                runtime.enable();

                // Navigate to a simple page
                page.navigate("data:text/html,<html><body><h1>Test Page</h1></body></html>");

                // Wait a bit for navigation
                Thread.sleep(500);

                // Evaluate some JavaScript
                var result = runtime.evaluate("document.querySelector('h1').textContent");
                assertThat(result).isNotNull();
                assertThat(result.getResult()).isNotNull();
            }
        }
    }

    @Test
    void reconnect_afterClose_shouldNotBeAllowed() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl());
            session.connect();
            session.close();

            // Attempting to connect again should fail
            assertThatThrownBy(() -> session.connect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has been closed");
        }
    }

    @Test
    void doubleConnect_shouldThrowException() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                // Attempting to connect again should fail
                assertThatThrownBy(() -> session.connect())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Already connected");
            }
        }
    }
}
