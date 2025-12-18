package com.fostermoore.pdfviachrome.wait;

import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import com.fostermoore.pdfviachrome.cdp.CdpSession;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for WaitStrategy implementations with real Chrome instances.
 * <p>
 * These tests require Chrome to be installed and accessible on the system.
 * They can be skipped by not setting the RUN_INTEGRATION_TESTS environment variable.
 * </p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true", disabledReason = "Integration tests disabled")
class WaitStrategyIntegrationTest {

    @Test
    void timeoutWait_withValidDuration_shouldWaitCorrectly() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Navigate to a simple page
                page.navigate("data:text/html,<html><body><h1>Test Page</h1></body></html>");

                // Wait using TimeoutWait strategy
                WaitStrategy wait = new TimeoutWait(Duration.ofMillis(500));

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Verify wait duration (allow 200ms tolerance)
                assertThat(elapsedTime).isGreaterThanOrEqualTo(500);
                assertThat(elapsedTime).isLessThan(800);
            }
        }
    }

    @Test
    void timeoutWait_withStaticFactory_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><p>Static factory test</p></body></html>");

                // Use static factory method
                WaitStrategy wait = WaitStrategy.timeout(Duration.ofMillis(300));

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                assertThat(elapsedTime).isGreaterThanOrEqualTo(300);
                assertThat(elapsedTime).isLessThan(600);
            }
        }
    }

    @Test
    void timeoutWait_withDefaultFactory_shouldUse2Seconds() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><p>Default timeout test</p></body></html>");

                // Use default factory method (2 seconds)
                WaitStrategy wait = WaitStrategy.timeout();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should wait approximately 2 seconds
                assertThat(elapsedTime).isGreaterThanOrEqualTo(2000);
                assertThat(elapsedTime).isLessThan(2500);
            }
        }
    }

    @Test
    void timeoutWait_withMultipleInvocations_shouldWaitEachTime() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><p>Multiple invocations test</p></body></html>");

                WaitStrategy wait = new TimeoutWait(Duration.ofMillis(200));
                ChromeDevToolsService devTools = getDevToolsService(session);

                // First wait
                long start1 = System.currentTimeMillis();
                wait.await(devTools, Duration.ofSeconds(10));
                long elapsed1 = System.currentTimeMillis() - start1;

                // Second wait
                long start2 = System.currentTimeMillis();
                wait.await(devTools, Duration.ofSeconds(10));
                long elapsed2 = System.currentTimeMillis() - start2;

                // Third wait
                long start3 = System.currentTimeMillis();
                wait.await(devTools, Duration.ofSeconds(10));
                long elapsed3 = System.currentTimeMillis() - start3;

                // All should wait approximately the same amount
                assertThat(elapsed1).isGreaterThanOrEqualTo(200);
                assertThat(elapsed2).isGreaterThanOrEqualTo(200);
                assertThat(elapsed3).isGreaterThanOrEqualTo(200);
            }
        }
    }

    @Test
    void timeoutWait_withComplexPage_shouldWaitRegardlessOfPageState() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Navigate to a more complex page with some JavaScript
                String html = """
                    <html>
                    <head>
                        <title>Complex Page</title>
                        <style>
                            body { font-family: Arial; }
                            .content { margin: 20px; }
                        </style>
                    </head>
                    <body>
                        <div class="content">
                            <h1 id="title">Loading...</h1>
                            <p id="message">Please wait...</p>
                        </div>
                        <script>
                            setTimeout(() => {
                                document.getElementById('title').textContent = 'Complex Page';
                                document.getElementById('message').textContent = 'Content loaded!';
                            }, 100);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);

                // TimeoutWait should wait the specified duration regardless of page state
                WaitStrategy wait = new TimeoutWait(Duration.ofMillis(400));

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                assertThat(elapsedTime).isGreaterThanOrEqualTo(400);
                assertThat(elapsedTime).isLessThan(700);
            }
        }
    }

    @Test
    void timeoutWait_whenThreadInterrupted_shouldThrowInterruptedException() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><p>Interrupt test</p></body></html>");

                WaitStrategy wait = new TimeoutWait(Duration.ofSeconds(5));
                ChromeDevToolsService devTools = getDevToolsService(session);

                CountDownLatch latch = new CountDownLatch(1);
                Thread testThread = new Thread(() -> {
                    try {
                        wait.await(devTools, Duration.ofSeconds(10));
                        fail("Expected InterruptedException to be thrown");
                    } catch (InterruptedException e) {
                        // Expected - verify interrupt status is restored
                        assertThat(Thread.currentThread().isInterrupted()).isTrue();
                        latch.countDown();
                    } catch (Exception e) {
                        fail("Expected InterruptedException, got: " + e.getClass().getSimpleName());
                    }
                });

                testThread.start();

                // Wait a bit then interrupt
                Thread.sleep(500);
                testThread.interrupt();

                // Wait for test thread to finish
                boolean completed = latch.await(2, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
                assertThat(testThread.isAlive()).isFalse();
            }
        }
    }

    @Test
    void timeoutWait_withVeryShortDuration_shouldStillWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><p>Short wait test</p></body></html>");

                // Very short wait (1ms)
                WaitStrategy wait = new TimeoutWait(Duration.ofMillis(1));

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should complete very quickly (within 100ms)
                assertThat(elapsedTime).isLessThan(100);
            }
        }
    }

    @Test
    void timeoutWait_withLongerDuration_shouldWaitFully() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><p>Long wait test</p></body></html>");

                // Longer wait (3 seconds)
                WaitStrategy wait = new TimeoutWait(Duration.ofSeconds(3));

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should wait approximately 3 seconds
                assertThat(elapsedTime).isGreaterThanOrEqualTo(3000);
                assertThat(elapsedTime).isLessThan(3500);
            }
        }
    }

    /**
     * Helper method to get the ChromeDevToolsService from a CdpSession.
     * This uses reflection to access the internal devToolsService field.
     */
    private ChromeDevToolsService getDevToolsService(CdpSession session) {
        try {
            java.lang.reflect.Field field = CdpSession.class.getDeclaredField("devToolsService");
            field.setAccessible(true);
            return (ChromeDevToolsService) field.get(session);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get ChromeDevToolsService from CdpSession", e);
        }
    }
}
