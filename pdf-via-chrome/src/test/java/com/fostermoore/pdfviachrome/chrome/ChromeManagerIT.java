package com.fostermoore.pdfviachrome.chrome;

import com.fostermoore.pdfviachrome.util.ChromePathDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ChromeManager that actually launch Chrome.
 *
 * These tests require Chrome to be installed on the system.
 * They are disabled by default and can be enabled by setting the
 * CHROME_INTEGRATION_TESTS system property to "true".
 *
 * To run: mvn verify -DCHROME_INTEGRATION_TESTS=true
 */
@EnabledIfSystemProperty(named = "CHROME_INTEGRATION_TESTS", matches = "true")
class ChromeManagerIT {

    @Test
    void testStartAndStopChrome() throws Exception {
        // Auto-detect Chrome
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent()
            .withFailMessage("Chrome not found on system. Please install Chrome to run integration tests.");

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager manager = new ChromeManager(options)) {
            ChromeProcess process = manager.start();

            assertThat(process).isNotNull();
            assertThat(process.isAlive()).isTrue();
            assertThat(process.getWebSocketDebuggerUrl()).isNotEmpty();
            assertThat(process.getWebSocketDebuggerUrl()).startsWith("ws://");
            assertThat(manager.isRunning()).isTrue();

            // Chrome should be running
            assertThat(process.getPid()).isGreaterThan(0);
        }
        // Chrome should be stopped after try-with-resources
    }

    @Test
    void testTryWithResourcesClosesChrome() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        ChromeProcess process;
        try (ChromeManager manager = new ChromeManager(options)) {
            process = manager.start();
            assertThat(process.isAlive()).isTrue();
        }

        // After try-with-resources, Chrome should be stopped
        // Wait a bit for the process to terminate
        Thread.sleep(1000);
        assertThat(process.isAlive()).isFalse();
    }

    @Test
    void testStartChromeWithCustomPort() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .remoteDebuggingPort(9222)
            .build();

        try (ChromeManager manager = new ChromeManager(options)) {
            ChromeProcess process = manager.start();

            assertThat(process.getWebSocketDebuggerUrl()).contains("9222");
        }
    }

    @Test
    void testStartChromeWithDockerFlags() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .noSandbox(true)
            .disableDevShmUsage(true)
            .build();

        try (ChromeManager manager = new ChromeManager(options)) {
            ChromeProcess process = manager.start();

            assertThat(process.isAlive()).isTrue();
            assertThat(process.getWebSocketDebuggerUrl()).isNotEmpty();
        }
    }

    @Test
    void testCannotStartTwice() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        try (ChromeManager manager = new ChromeManager(options)) {
            manager.start();

            // Trying to start again should fail
            assertThatThrownBy(manager::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chrome is already running");
        }
    }

    @Test
    void testGracefulShutdown() throws Exception {
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .shutdownTimeout(10)
            .build();

        ChromeManager manager = new ChromeManager(options);
        ChromeProcess process = manager.start();

        assertThat(process.isAlive()).isTrue();

        manager.close();

        // Process should be terminated
        Thread.sleep(500);
        assertThat(process.isAlive()).isFalse();
    }

    @Test
    void testAutoDetectChrome() throws Exception {
        // Test that Chrome can be auto-detected without specifying path
        ChromeOptions options = ChromeOptions.builder()
            .headless(true)
            .build();

        try (ChromeManager manager = new ChromeManager(options)) {
            ChromeProcess process = manager.start();

            assertThat(process).isNotNull();
            assertThat(process.isAlive()).isTrue();
        }
    }
}
