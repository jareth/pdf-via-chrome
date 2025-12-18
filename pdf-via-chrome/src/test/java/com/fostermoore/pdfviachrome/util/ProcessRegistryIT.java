package com.fostermoore.pdfviachrome.util;

import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ProcessRegistry with actual Chrome processes.
 *
 * These tests require Chrome to be installed on the system.
 * They are disabled by default and can be enabled by setting the
 * CHROME_INTEGRATION_TESTS environment variable to "true".
 *
 * To run: mvn verify -DCHROME_INTEGRATION_TESTS=true
 */
@EnabledIfEnvironmentVariable(named = "CHROME_INTEGRATION_TESTS", matches = "true")
class ProcessRegistryIT {

    private ProcessRegistry registry;
    private List<ChromeManager> managers;

    @BeforeEach
    void setUp() {
        registry = ProcessRegistry.getInstance();
        registry.clearForTesting();
        managers = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // Clean up all Chrome managers
        for (ChromeManager manager : managers) {
            try {
                manager.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        registry.clearForTesting();
    }

    @Test
    void testProcessTracking_withRealChromeProcess() throws Exception {
        // Auto-detect Chrome
        Optional<java.nio.file.Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent()
            .withFailMessage("Chrome not found on system. Please install Chrome to run integration tests.");

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        ChromeManager manager = new ChromeManager(options);
        managers.add(manager);

        // Initially no processes
        assertThat(registry.getActiveProcessCount()).isZero();

        // Start Chrome
        ChromeProcess chromeProcess = manager.start();

        // Process should be registered
        assertThat(registry.getActiveProcessCount()).isEqualTo(1);
        assertThat(registry.isRegistered(chromeProcess.getProcess())).isTrue();

        // Verify metadata
        ProcessRegistry.ProcessMetadata metadata = registry.getMetadata(chromeProcess.getProcess());
        assertThat(metadata).isNotNull();
        assertThat(metadata.getPid()).isEqualTo(chromeProcess.getPid());
        assertThat(metadata.wasAliveAtRegistration()).isTrue();
        assertThat(metadata.getStartTime()).isNotNull();

        // Close Chrome
        manager.close();

        // Wait a bit for cleanup
        Thread.sleep(500);

        // Process should be unregistered
        assertThat(registry.getActiveProcessCount()).isZero();
        assertThat(registry.isRegistered(chromeProcess.getProcess())).isFalse();
    }

    @Test
    void testProcessTracking_withMultipleChromeProcesses() throws Exception {
        Optional<java.nio.file.Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        // Start multiple Chrome instances
        ChromeOptions options1 = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .remoteDebuggingPort(9222)
            .build();

        ChromeOptions options2 = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .remoteDebuggingPort(9223)
            .build();

        ChromeOptions options3 = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .remoteDebuggingPort(9224)
            .build();

        ChromeManager manager1 = new ChromeManager(options1);
        ChromeManager manager2 = new ChromeManager(options2);
        ChromeManager manager3 = new ChromeManager(options3);
        managers.add(manager1);
        managers.add(manager2);
        managers.add(manager3);

        // Initially no processes
        assertThat(registry.getActiveProcessCount()).isZero();

        // Start all three
        ChromeProcess process1 = manager1.start();
        assertThat(registry.getActiveProcessCount()).isEqualTo(1);

        ChromeProcess process2 = manager2.start();
        assertThat(registry.getActiveProcessCount()).isEqualTo(2);

        ChromeProcess process3 = manager3.start();
        assertThat(registry.getActiveProcessCount()).isEqualTo(3);

        // All should be registered
        assertThat(registry.isRegistered(process1.getProcess())).isTrue();
        assertThat(registry.isRegistered(process2.getProcess())).isTrue();
        assertThat(registry.isRegistered(process3.getProcess())).isTrue();

        // Close one
        manager2.close();
        Thread.sleep(500);

        assertThat(registry.getActiveProcessCount()).isEqualTo(2);
        assertThat(registry.isRegistered(process1.getProcess())).isTrue();
        assertThat(registry.isRegistered(process2.getProcess())).isFalse();
        assertThat(registry.isRegistered(process3.getProcess())).isTrue();

        // Close remaining
        manager1.close();
        manager3.close();
        Thread.sleep(500);

        assertThat(registry.getActiveProcessCount()).isZero();
    }

    @Test
    void testHealthCheck_withRealProcesses() throws Exception {
        Optional<java.nio.file.Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        ChromeManager manager = new ChromeManager(options);
        managers.add(manager);

        ChromeProcess chromeProcess = manager.start();

        // Process is alive, health check should find nothing
        int cleaned = registry.performHealthCheck();
        assertThat(cleaned).isZero();
        assertThat(registry.getActiveProcessCount()).isEqualTo(1);

        // Kill the process directly (simulating crash)
        chromeProcess.getProcess().destroyForcibly();
        Thread.sleep(1000);

        // Health check should detect and remove dead process
        cleaned = registry.performHealthCheck();
        assertThat(cleaned).isEqualTo(1);
        assertThat(registry.getActiveProcessCount()).isZero();
    }

    @Test
    void testCleanupAll_withRealProcesses() throws Exception {
        Optional<java.nio.file.Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        // Start two Chrome instances
        ChromeOptions options1 = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .remoteDebuggingPort(9225)
            .build();

        ChromeOptions options2 = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .remoteDebuggingPort(9226)
            .build();

        ChromeManager manager1 = new ChromeManager(options1);
        ChromeManager manager2 = new ChromeManager(options2);
        managers.add(manager1);
        managers.add(manager2);

        ChromeProcess process1 = manager1.start();
        ChromeProcess process2 = manager2.start();

        assertThat(registry.getActiveProcessCount()).isEqualTo(2);
        assertThat(process1.isAlive()).isTrue();
        assertThat(process2.isAlive()).isTrue();

        // Emergency cleanup
        int terminated = registry.cleanupAll();

        assertThat(terminated).isEqualTo(2);
        assertThat(registry.getActiveProcessCount()).isZero();

        // Wait for processes to fully terminate
        Thread.sleep(1000);

        assertThat(process1.isAlive()).isFalse();
        assertThat(process2.isAlive()).isFalse();
    }

    @Test
    void testGetActiveProcesses_withRealProcesses() throws Exception {
        Optional<java.nio.file.Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        ChromeManager manager = new ChromeManager(options);
        managers.add(manager);

        ChromeProcess chromeProcess = manager.start();

        List<ProcessRegistry.ProcessMetadata> activeProcesses = registry.getActiveProcesses();

        assertThat(activeProcesses).hasSize(1);
        ProcessRegistry.ProcessMetadata metadata = activeProcesses.get(0);
        assertThat(metadata.getPid()).isEqualTo(chromeProcess.getPid());
        assertThat(metadata.wasAliveAtRegistration()).isTrue();
        assertThat(metadata.getAgeMillis()).isGreaterThanOrEqualTo(0);

        manager.close();
        Thread.sleep(500);

        assertThat(registry.getActiveProcesses()).isEmpty();
    }

    @Test
    void testProcessMetadata_ageTracking() throws Exception {
        Optional<java.nio.file.Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        ChromeManager manager = new ChromeManager(options);
        managers.add(manager);

        ChromeProcess chromeProcess = manager.start();

        ProcessRegistry.ProcessMetadata metadata = registry.getMetadata(chromeProcess.getProcess());
        long age1 = metadata.getAgeMillis();

        // Wait a bit
        Thread.sleep(200);

        long age2 = metadata.getAgeMillis();

        // Age should have increased
        assertThat(age2).isGreaterThan(age1);
        assertThat(age2 - age1).isGreaterThanOrEqualTo(200);

        manager.close();
    }

    @Test
    void testTryWithResources_automaticallyUnregisters() throws Exception {
        Optional<java.nio.file.Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
            .chromePath(chromePath.get())
            .headless(true)
            .build();

        ChromeProcess chromeProcess;

        try (ChromeManager manager = new ChromeManager(options)) {
            chromeProcess = manager.start();
            assertThat(registry.getActiveProcessCount()).isEqualTo(1);
            assertThat(registry.isRegistered(chromeProcess.getProcess())).isTrue();
        }

        // After try-with-resources, should be unregistered
        Thread.sleep(500);
        assertThat(registry.getActiveProcessCount()).isZero();
        assertThat(registry.isRegistered(chromeProcess.getProcess())).isFalse();
    }
}
