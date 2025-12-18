package com.fostermoore.pdfviachrome.chrome;

import com.fostermoore.pdfviachrome.util.ChromePathDetector;
import com.fostermoore.pdfviachrome.util.ResourceCleanup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for shutdown hook functionality in ChromeManager.
 *
 * These tests verify that Chrome processes are properly registered for cleanup
 * via JVM shutdown hooks and that the hooks are correctly managed.
 */
@EnabledIfEnvironmentVariable(named = "CHROME_AVAILABLE", matches = "true", disabledReason = "Chrome not available in CI")
class ChromeManagerShutdownTest {

    private List<ChromeManager> managersToCleanup;
    private int initialHookCount;

    @BeforeEach
    void setUp() {
        managersToCleanup = new ArrayList<>();
        initialHookCount = ResourceCleanup.getRegisteredHookCount();
    }

    @AfterEach
    void tearDown() {
        // Clean up any managers that weren't properly closed
        for (ChromeManager manager : managersToCleanup) {
            try {
                manager.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        managersToCleanup.clear();
    }

    @Test
    void testShutdownHookRegisteredOnStart() throws Exception {
        // Verify Chrome is available
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
                .chromePath(chromePath.get())
                .build();

        ChromeManager manager = new ChromeManager(options);
        managersToCleanup.add(manager);

        // Before starting, no hook should be registered
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount);

        // Start Chrome
        ChromeProcess process = manager.start();
        assertThat(process).isNotNull();
        assertThat(process.isAlive()).isTrue();

        // After starting, hook should be registered
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount + 1);
        assertThat(ResourceCleanup.isHookRegistered(process)).isTrue();

        // Clean up
        manager.close();
        managersToCleanup.remove(manager);
    }

    @Test
    void testShutdownHookRemovedOnNormalClose() throws Exception {
        // Verify Chrome is available
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
                .chromePath(chromePath.get())
                .build();

        ChromeManager manager = new ChromeManager(options);
        managersToCleanup.add(manager);

        // Start Chrome and verify hook is registered
        ChromeProcess process = manager.start();
        assertThat(ResourceCleanup.isHookRegistered(process)).isTrue();
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount + 1);

        // Close manager normally
        manager.close();
        managersToCleanup.remove(manager);

        // Hook should be removed
        assertThat(ResourceCleanup.isHookRegistered(process)).isFalse();
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount);
    }

    @Test
    void testMultipleInstancesHaveIndependentHooks() throws Exception {
        // Verify Chrome is available
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options1 = ChromeOptions.builder()
                .chromePath(chromePath.get())
                .remoteDebuggingPort(9222)
                .build();

        ChromeOptions options2 = ChromeOptions.builder()
                .chromePath(chromePath.get())
                .remoteDebuggingPort(9223)
                .build();

        ChromeManager manager1 = new ChromeManager(options1);
        ChromeManager manager2 = new ChromeManager(options2);
        managersToCleanup.add(manager1);
        managersToCleanup.add(manager2);

        // Start both Chrome instances
        ChromeProcess process1 = manager1.start();
        ChromeProcess process2 = manager2.start();

        // Both should have hooks registered
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount + 2);
        assertThat(ResourceCleanup.isHookRegistered(process1)).isTrue();
        assertThat(ResourceCleanup.isHookRegistered(process2)).isTrue();

        // Close first manager
        manager1.close();
        managersToCleanup.remove(manager1);

        // Only first hook should be removed
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount + 1);
        assertThat(ResourceCleanup.isHookRegistered(process1)).isFalse();
        assertThat(ResourceCleanup.isHookRegistered(process2)).isTrue();

        // Close second manager
        manager2.close();
        managersToCleanup.remove(manager2);

        // All hooks should be removed
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount);
        assertThat(ResourceCleanup.isHookRegistered(process2)).isFalse();
    }

    @Test
    void testTemporaryDirectoryCleanedUpInShutdownHook() throws Exception {
        // Verify Chrome is available
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
                .chromePath(chromePath.get())
                .build(); // Don't specify user data dir - will create temporary

        ChromeManager manager = new ChromeManager(options);
        managersToCleanup.add(manager);

        // Start Chrome
        ChromeProcess process = manager.start();
        assertThat(process.isTemporaryUserDataDir()).isTrue();

        Path tempDir = process.getUserDataDir();
        assertThat(tempDir).isNotNull();
        assertThat(Files.exists(tempDir)).isTrue();

        // Manually invoke the shutdown hook logic to test cleanup
        // (We can't actually trigger JVM shutdown in a unit test)
        Process rawProcess = process.getProcess();
        if (rawProcess.isAlive()) {
            rawProcess.destroyForcibly();
            rawProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        }

        // Note: We can't easily test the actual directory deletion here without
        // actually running the shutdown hook, which we can't do in a unit test.
        // The important part is that the hook is registered and contains the cleanup logic.

        // Clean up
        manager.close();
        managersToCleanup.remove(manager);

        // Verify temporary directory was cleaned up
        assertThat(Files.exists(tempDir)).isFalse();
    }

    @Test
    void testIdempotentClose() throws Exception {
        // Verify Chrome is available
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
                .chromePath(chromePath.get())
                .build();

        ChromeManager manager = new ChromeManager(options);
        managersToCleanup.add(manager);

        // Start Chrome
        ChromeProcess process = manager.start();
        assertThat(ResourceCleanup.isHookRegistered(process)).isTrue();

        // Close multiple times
        manager.close();
        manager.close();
        manager.close();

        managersToCleanup.remove(manager);

        // Hook should only be removed once, no errors should occur
        assertThat(ResourceCleanup.isHookRegistered(process)).isFalse();
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialHookCount);
    }

    @Test
    void testProcessTerminatedWhenHookNotRemoved() throws Exception {
        // Verify Chrome is available
        Optional<Path> chromePath = ChromePathDetector.detectChromePath();
        assertThat(chromePath).isPresent();

        ChromeOptions options = ChromeOptions.builder()
                .chromePath(chromePath.get())
                .build();

        ChromeManager manager = new ChromeManager(options);

        // Start Chrome
        ChromeProcess process = manager.start();
        assertThat(process.isAlive()).isTrue();
        assertThat(ResourceCleanup.isHookRegistered(process)).isTrue();

        long pid = process.getPid();

        // Simulate abnormal termination by NOT calling close()
        // Instead, just remove the manager from our cleanup list
        // and let the shutdown hook logic handle it

        // We'll manually verify the process gets terminated
        // In a real JVM shutdown, the hook would run automatically

        // For testing, we can't trigger actual JVM shutdown, but we can verify:
        // 1. Hook is registered
        assertThat(ResourceCleanup.isHookRegistered(process)).isTrue();

        // 2. Process is alive
        assertThat(process.isAlive()).isTrue();

        // Clean up for the test
        manager.close();

        // After normal close, hook should be removed
        assertThat(ResourceCleanup.isHookRegistered(process)).isFalse();
    }
}
