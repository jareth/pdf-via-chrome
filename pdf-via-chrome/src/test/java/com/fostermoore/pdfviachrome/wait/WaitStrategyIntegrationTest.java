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

    // ========== ElementWait Integration Tests ==========

    @Test
    void elementWait_whenElementPresentImmediately_shouldReturnQuickly() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Navigate to a page with the element already present
                page.navigate("data:text/html,<html><body><div id=\"target\">Element is here</div></body></html>");
                Thread.sleep(100); // Allow navigation to complete

                // Wait for the element
                WaitStrategy wait = ElementWait.builder()
                        .selector("#target")
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(10));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should return very quickly since element is already present
                assertThat(elapsedTime).isLessThan(500);
            }
        }
    }

    @Test
    void elementWait_whenElementAppearsAfterDelay_shouldWaitUntilFound() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page that adds element after 500ms
                String html = """
                    <html>
                    <body>
                        <div id="initial">Initial content</div>
                        <script>
                            setTimeout(() => {
                                const div = document.createElement('div');
                                div.id = 'delayed-element';
                                div.textContent = 'Delayed content';
                                document.body.appendChild(div);
                            }, 500);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);
                Thread.sleep(100); // Allow navigation to complete

                // Wait for the delayed element
                WaitStrategy wait = ElementWait.builder()
                        .selector("#delayed-element")
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(5));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should wait at least 500ms (the delay time)
                assertThat(elapsedTime).isGreaterThanOrEqualTo(500);
                assertThat(elapsedTime).isLessThan(1500);
            }
        }
    }

    @Test
    void elementWait_withVisibleCheck_shouldWaitForVisibility() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with hidden element that becomes visible after delay
                String html = """
                    <html>
                    <head>
                        <style>
                            #hidden-element { display: none; }
                        </style>
                    </head>
                    <body>
                        <div id="hidden-element">Hidden element</div>
                        <script>
                            setTimeout(() => {
                                document.getElementById('hidden-element').style.display = 'block';
                            }, 400);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);
                Thread.sleep(100); // Allow navigation to complete

                // Wait for element to be visible (not just present)
                WaitStrategy wait = ElementWait.builder()
                        .selector("#hidden-element")
                        .waitForVisible(true)
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(5));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should wait at least 400ms for visibility
                assertThat(elapsedTime).isGreaterThanOrEqualTo(400);
                assertThat(elapsedTime).isLessThan(1000);
            }
        }
    }

    @Test
    void elementWait_withPresentCheck_shouldFindHiddenElement() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with hidden element
                String html = """
                    <html>
                    <head>
                        <style>
                            #hidden-element { display: none; }
                        </style>
                    </head>
                    <body>
                        <div id="hidden-element">Hidden but present</div>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);
                Thread.sleep(100); // Allow navigation to complete

                // Wait for element presence (ignore visibility)
                WaitStrategy wait = ElementWait.builder()
                        .selector("#hidden-element")
                        .waitForVisible(false)
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(5));
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Should find element immediately even though it's hidden
                assertThat(elapsedTime).isLessThan(500);
            }
        }
    }

    @Test
    void elementWait_whenElementNeverAppears_shouldTimeout() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page without the target element
                page.navigate("data:text/html,<html><body><div id=\"other\">Other content</div></body></html>");
                Thread.sleep(100); // Allow navigation to complete

                // Wait for non-existent element
                WaitStrategy wait = ElementWait.builder()
                        .selector("#nonexistent")
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                assertThatThrownBy(() ->
                    wait.await(getDevToolsService(session), Duration.ofSeconds(1))
                ).isInstanceOf(java.util.concurrent.TimeoutException.class)
                 .hasMessageContaining("#nonexistent")
                 .hasMessageContaining("not present");
            }
        }
    }

    @Test
    void elementWait_withClassSelector_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with class-based element
                String html = """
                    <html>
                    <body>
                        <div class="content">Content</div>
                        <script>
                            setTimeout(() => {
                                const div = document.createElement('div');
                                div.className = 'target-class';
                                div.textContent = 'Target';
                                document.body.appendChild(div);
                            }, 300);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);
                Thread.sleep(100);

                // Wait for element with class selector
                WaitStrategy wait = ElementWait.builder()
                        .selector(".target-class")
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(5));
                long elapsedTime = System.currentTimeMillis() - startTime;

                assertThat(elapsedTime).isGreaterThanOrEqualTo(300);
                assertThat(elapsedTime).isLessThan(1000);
            }
        }
    }

    @Test
    void elementWait_withAttributeSelector_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with attribute-based element
                String html = """
                    <html>
                    <body>
                        <div>Content</div>
                        <script>
                            setTimeout(() => {
                                const div = document.createElement('div');
                                div.setAttribute('data-loaded', 'true');
                                div.textContent = 'Loaded';
                                document.body.appendChild(div);
                            }, 300);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);
                Thread.sleep(100);

                // Wait for element with attribute selector
                WaitStrategy wait = ElementWait.builder()
                        .selector("[data-loaded='true']")
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(5));
                long elapsedTime = System.currentTimeMillis() - startTime;

                assertThat(elapsedTime).isGreaterThanOrEqualTo(300);
                assertThat(elapsedTime).isLessThan(1000);
            }
        }
    }

    @Test
    void elementWait_withStaticFactoryPresent_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                page.navigate("data:text/html,<html><body><div id=\"test\">Test</div></body></html>");
                Thread.sleep(100);

                // Use static factory method
                WaitStrategy wait = WaitStrategy.elementPresent("#test");

                assertThatNoException().isThrownBy(() ->
                    wait.await(getDevToolsService(session), Duration.ofSeconds(5))
                );
            }
        }
    }

    @Test
    void elementWait_withStaticFactoryVisible_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with visible element
                String html = """
                    <html>
                    <body>
                        <div id="visible-test" style="display: block; width: 100px; height: 100px;">Visible</div>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);
                Thread.sleep(100);

                // Use static factory method for visibility
                WaitStrategy wait = WaitStrategy.elementVisible("#visible-test");

                assertThatNoException().isThrownBy(() ->
                    wait.await(getDevToolsService(session), Duration.ofSeconds(5))
                );
            }
        }
    }

    @Test
    void elementWait_withComplexSelector_shouldWork() throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager chromeManager = new ChromeManager(options)) {
            ChromeProcess chromeProcess = chromeManager.start();

            try (CdpSession session = new CdpSession(chromeProcess.getWebSocketDebuggerUrl())) {
                session.connect();

                Page page = session.getPage();
                page.enable();

                // Page with nested elements
                String html = """
                    <html>
                    <body>
                        <div class="container">
                            <div class="content">
                                <p class="text">Initial</p>
                            </div>
                        </div>
                        <script>
                            setTimeout(() => {
                                const p = document.createElement('p');
                                p.className = 'text target';
                                p.textContent = 'Target text';
                                document.querySelector('.content').appendChild(p);
                            }, 300);
                        </script>
                    </body>
                    </html>
                    """;

                page.navigate("data:text/html," + html);
                Thread.sleep(100);

                // Wait for element with complex selector
                WaitStrategy wait = ElementWait.builder()
                        .selector(".container .content p.target")
                        .pollInterval(Duration.ofMillis(50))
                        .build();

                long startTime = System.currentTimeMillis();
                wait.await(getDevToolsService(session), Duration.ofSeconds(5));
                long elapsedTime = System.currentTimeMillis() - startTime;

                assertThat(elapsedTime).isGreaterThanOrEqualTo(300);
                assertThat(elapsedTime).isLessThan(1000);
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
