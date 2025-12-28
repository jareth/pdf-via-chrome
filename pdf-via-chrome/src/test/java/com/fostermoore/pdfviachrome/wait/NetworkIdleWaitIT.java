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
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for NetworkIdleWait with real Chrome instances.
 * <p>
 * These tests require Chrome to be installed and accessible on the system.
 * They can be skipped by not setting the RUN_INTEGRATION_TESTS environment variable.
 * </p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true", disabledReason = "Integration tests disabled")
class NetworkIdleWaitIT {

    @Test
    void networkIdleWait_withNoNetworkActivity_shouldCompleteQuickly() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Navigate to a simple static page
                page.navigate("data:text/html,<html><body><h1>Static Page</h1></body></html>");

                // Wait for network to be idle
                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(200))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should complete after quiet period + small overhead
                assertThat(elapsedTime).isGreaterThanOrEqualTo(200);
                assertThat(elapsedTime).isLessThan(1000);
            }
        }
    }

    @Test
    void networkIdleWait_withDelayedAjaxRequest_shouldWaitForCompletion() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with delayed AJAX request
                String html = """
                    <html>
                    <head>
                        <title>AJAX Test Page</title>
                    </head>
                    <body>
                        <h1 id="title">Loading...</h1>
                        <div id="content">Waiting for data...</div>
                        <script>
                            // Simulate AJAX request after 500ms
                            setTimeout(() => {
                                fetch('data:text/plain,test data')
                                    .then(response => response.text())
                                    .then(data => {
                                        document.getElementById('title').textContent = 'Loaded';
                                        document.getElementById('content').textContent = data;
                                    });
                            }, 500);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);

                // Wait for network to be idle
                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(300))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should wait for AJAX request to complete (500ms delay + request time + quiet period)
                assertThat(elapsedTime).isGreaterThanOrEqualTo(800);
                assertThat(elapsedTime).isLessThan(2000);
            }
        }
    }

    @Test
    void networkIdleWait_withMultipleAjaxRequests_shouldWaitForAllToComplete() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with multiple AJAX requests
                String html = """
                    <html>
                    <head>
                        <title>Multiple AJAX Test</title>
                    </head>
                    <body>
                        <h1>Multiple AJAX Requests</h1>
                        <div id="result1">Loading 1...</div>
                        <div id="result2">Loading 2...</div>
                        <div id="result3">Loading 3...</div>
                        <script>
                            // First request - immediate
                            fetch('data:text/plain,data1')
                                .then(r => r.text())
                                .then(d => document.getElementById('result1').textContent = d);

                            // Second request - after 200ms
                            setTimeout(() => {
                                fetch('data:text/plain,data2')
                                    .then(r => r.text())
                                    .then(d => document.getElementById('result2').textContent = d);
                            }, 200);

                            // Third request - after 400ms
                            setTimeout(() => {
                                fetch('data:text/plain,data3')
                                    .then(r => r.text())
                                    .then(d => document.getElementById('result3').textContent = d);
                            }, 400);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);

                // Wait for network to be idle
                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(300))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should wait for all requests to complete (400ms + request time + quiet period)
                assertThat(elapsedTime).isGreaterThanOrEqualTo(700);
                assertThat(elapsedTime).isLessThan(2000);
            }
        }
    }

    @Test
    void networkIdleWait_withMaxInflightRequests_shouldCompleteWhenThresholdMet() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with a long-polling request (simulated with delayed fetch)
                String html = """
                    <html>
                    <head>
                        <title>Long Polling Test</title>
                    </head>
                    <body>
                        <h1>Long Polling Simulation</h1>
                        <div id="status">Active</div>
                        <script>
                            // Quick request that completes
                            fetch('data:text/plain,initial')
                                .then(r => r.text())
                                .then(d => document.getElementById('status').textContent = d);

                            // Simulate a long-polling request that takes a while
                            // In reality this would be a server endpoint that keeps connection open
                            setTimeout(() => {
                                fetch('data:text/plain,polling')
                                    .then(r => r.text())
                                    .then(d => console.log(d));
                            }, 200);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);

                // Allow 1 inflight request (the long-polling one)
                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(300))
                        .maxInflightRequests(1)
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should complete after initial requests complete (200ms + request time + quiet period)
                assertThat(elapsedTime).isGreaterThanOrEqualTo(300);
                assertThat(elapsedTime).isLessThan(2000);
            }
        }
    }

    @Test
    void networkIdleWait_withTimeout_shouldThrowTimeoutException() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with continuous AJAX requests (every 100ms)
                String html = """
                    <html>
                    <head>
                        <title>Continuous AJAX Test</title>
                    </head>
                    <body>
                        <h1>Continuous Requests</h1>
                        <div id="counter">0</div>
                        <script>
                            let count = 0;
                            setInterval(() => {
                                fetch('data:text/plain,request' + count)
                                    .then(r => r.text())
                                    .then(d => {
                                        count++;
                                        document.getElementById('counter').textContent = count;
                                    });
                            }, 100);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);

                // Network will never be idle due to continuous requests
                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(500))
                        .build();

                assertThatThrownBy(() -> wait.await(getDevToolsService(session), Duration.ofSeconds(1)))
                        .isInstanceOf(TimeoutException.class)
                        .hasMessageContaining("Network did not become idle");
            }
        }
    }

    @Test
    void networkIdleWait_withPageContainingImages_shouldWaitForImageLoads() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with data URI images (loads quickly)
                String html = """
                    <html>
                    <head>
                        <title>Image Load Test</title>
                    </head>
                    <body>
                        <h1>Images</h1>
                        <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" alt="red dot">
                        <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==" alt="blue dot">
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);

                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(200))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should complete after images load + quiet period
                assertThat(elapsedTime).isGreaterThanOrEqualTo(200);
                assertThat(elapsedTime).isLessThan(2000);
            }
        }
    }

    @Test
    void networkIdleWait_whenInterrupted_shouldThrowInterruptedException() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with continuous requests
                String html = """
                    <html>
                    <head>
                        <title>Interrupt Test</title>
                    </head>
                    <body>
                        <h1>Continuous Activity</h1>
                        <script>
                            setInterval(() => {
                                fetch('data:text/plain,test');
                            }, 100);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);

                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofSeconds(5))
                        .build();

                ChromeDevToolsService devTools = getDevToolsService(session);

                CountDownLatch latch = new CountDownLatch(1);
                Thread testThread = new Thread(() -> {
                    try {
                        wait.await(devTools, Duration.ofSeconds(30));
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
                boolean completed = latch.await(3, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
                assertThat(testThread.isAlive()).isFalse();
            }
        }
    }

    @Test
    void networkIdleWait_withDefaultSettings_shouldWorkCorrectly() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Simple page
                page.navigate("data:text/html,<html><body><h1>Default Settings Test</h1></body></html>");

                // Use default builder settings (500ms quiet period, 0 max inflight)
                WaitStrategy wait = NetworkIdleWait.builder().build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should complete after default 500ms quiet period
                assertThat(elapsedTime).isGreaterThanOrEqualTo(500);
                assertThat(elapsedTime).isLessThan(1500);
            }
        }
    }

    @Test
    void networkIdleWait_withVeryShortQuietPeriod_shouldWorkCorrectly() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><p>Short quiet period test</p></body></html>");

                // Very short quiet period (50ms)
                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should complete quickly
                assertThat(elapsedTime).isGreaterThanOrEqualTo(50);
                assertThat(elapsedTime).isLessThan(500);
            }
        }
    }

    @Test
    void networkIdleWait_multipleInvocations_shouldWorkCorrectly() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                WaitStrategy wait = NetworkIdleWait.builder()
                        .quietPeriod(Duration.ofMillis(200))
                        .build();

                ChromeDevToolsService devTools = getDevToolsService(session);

                // First page
                page.navigate("data:text/html,<html><body><h1>Page 1</h1></body></html>");
                long start1 = System.currentTimeMillis();
                wait.await(devTools, Duration.ofSeconds(10));
                long elapsed1 = System.currentTimeMillis() - start1;

                // Second page
                page.navigate("data:text/html,<html><body><h1>Page 2</h1></body></html>");
                long start2 = System.currentTimeMillis();
                wait.await(devTools, Duration.ofSeconds(10));
                long elapsed2 = System.currentTimeMillis() - start2;

                // Third page with AJAX
                String html = """
                    <html>
                    <body>
                        <h1>Page 3 with AJAX</h1>
                        <script>
                            setTimeout(() => {
                                fetch('data:text/plain,data');
                            }, 100);
                        </script>
                    </body>
                    </html>
                    """;
                page.navigate("data:text/html," + html);
                long start3 = System.currentTimeMillis();
                wait.await(devTools, Duration.ofSeconds(10));
                long elapsed3 = System.currentTimeMillis() - start3;

                // All should complete successfully
                assertThat(elapsed1).isGreaterThanOrEqualTo(200);
                assertThat(elapsed2).isGreaterThanOrEqualTo(200);
                assertThat(elapsed3).isGreaterThanOrEqualTo(300); // Has AJAX delay
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
