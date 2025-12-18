package com.fostermoore.pdfviachrome.util;

import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResourceCleanup utility class.
 */
class ResourceCleanupTest {

    @Test
    void testCannotInstantiate() {
        assertThatThrownBy(() -> {
            // Use reflection to try to instantiate
            var constructor = ResourceCleanup.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testRegisterShutdownHook() {
        // Create a mock Chrome process
        Process mockProcess = mock(Process.class);
        when(mockProcess.pid()).thenReturn(12345L);
        when(mockProcess.isAlive()).thenReturn(true);

        Path tempDir = Paths.get("/tmp/chrome-test");
        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, "ws://localhost:9222", tempDir, true);

        int initialCount = ResourceCleanup.getRegisteredHookCount();

        // Register shutdown hook
        Thread hook = ResourceCleanup.registerShutdownHook(chromeProcess);

        assertThat(hook).isNotNull();
        assertThat(hook.getName()).contains("ChromeCleanup-PID-12345");
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount + 1);
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess)).isTrue();

        // Clean up
        ResourceCleanup.removeShutdownHook(chromeProcess);
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount);
    }

    @Test
    void testRegisterShutdownHookWithNullProcess() {
        assertThatThrownBy(() -> ResourceCleanup.registerShutdownHook(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ChromeProcess cannot be null");
    }

    @Test
    void testRemoveShutdownHook() {
        Process mockProcess = mock(Process.class);
        when(mockProcess.pid()).thenReturn(12345L);
        when(mockProcess.isAlive()).thenReturn(true);

        Path tempDir = Paths.get("/tmp/chrome-test");
        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, "ws://localhost:9222", tempDir, false);

        int initialCount = ResourceCleanup.getRegisteredHookCount();

        // Register and then remove
        ResourceCleanup.registerShutdownHook(chromeProcess);
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount + 1);

        boolean removed = ResourceCleanup.removeShutdownHook(chromeProcess);
        assertThat(removed).isTrue();
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount);
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess)).isFalse();
    }

    @Test
    void testRemoveShutdownHookWithNullProcess() {
        boolean removed = ResourceCleanup.removeShutdownHook(null);
        assertThat(removed).isFalse();
    }

    @Test
    void testRemoveNonExistentHook() {
        Process mockProcess = mock(Process.class);
        when(mockProcess.pid()).thenReturn(99999L);

        Path tempDir = Paths.get("/tmp/chrome-test");
        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, "ws://localhost:9222", tempDir, false);

        // Try to remove a hook that was never registered
        boolean removed = ResourceCleanup.removeShutdownHook(chromeProcess);
        assertThat(removed).isFalse();
    }

    @Test
    void testIsHookRegistered() {
        Process mockProcess = mock(Process.class);
        when(mockProcess.pid()).thenReturn(12345L);

        Path tempDir = Paths.get("/tmp/chrome-test");
        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, "ws://localhost:9222", tempDir, false);

        // Initially not registered
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess)).isFalse();

        // Register
        ResourceCleanup.registerShutdownHook(chromeProcess);
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess)).isTrue();

        // Remove
        ResourceCleanup.removeShutdownHook(chromeProcess);
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess)).isFalse();
    }

    @Test
    void testIsHookRegisteredWithNull() {
        assertThat(ResourceCleanup.isHookRegistered(null)).isFalse();
    }

    @Test
    void testMultipleProcesses() {
        Process mockProcess1 = mock(Process.class);
        when(mockProcess1.pid()).thenReturn(11111L);
        ChromeProcess chromeProcess1 = new ChromeProcess(mockProcess1, "ws://localhost:9222",
                                                          Paths.get("/tmp/chrome-1"), false);

        Process mockProcess2 = mock(Process.class);
        when(mockProcess2.pid()).thenReturn(22222L);
        ChromeProcess chromeProcess2 = new ChromeProcess(mockProcess2, "ws://localhost:9223",
                                                          Paths.get("/tmp/chrome-2"), false);

        int initialCount = ResourceCleanup.getRegisteredHookCount();

        // Register both
        ResourceCleanup.registerShutdownHook(chromeProcess1);
        ResourceCleanup.registerShutdownHook(chromeProcess2);

        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount + 2);
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess1)).isTrue();
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess2)).isTrue();

        // Remove first
        ResourceCleanup.removeShutdownHook(chromeProcess1);
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount + 1);
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess1)).isFalse();
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess2)).isTrue();

        // Remove second
        ResourceCleanup.removeShutdownHook(chromeProcess2);
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount);
        assertThat(ResourceCleanup.isHookRegistered(chromeProcess2)).isFalse();
    }

    @Test
    void testShutdownHookTerminatesProcess() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.pid()).thenReturn(12345L);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);

        Path tempDir = Paths.get("/tmp/chrome-test");
        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, "ws://localhost:9222", tempDir, false);

        // Register shutdown hook
        Thread hook = ResourceCleanup.registerShutdownHook(chromeProcess);

        // Manually run the shutdown hook to test its behavior
        hook.start();

        // Verify the process was forcibly destroyed
        verify(mockProcess).destroyForcibly();
        verify(mockProcess).waitFor(5, TimeUnit.SECONDS);

        // Clean up
        ResourceCleanup.removeShutdownHook(chromeProcess);
    }

    @Test
    void testShutdownHookSkipsDeadProcess() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.pid()).thenReturn(12345L);
        when(mockProcess.isAlive()).thenReturn(false); // Process already dead

        Path tempDir = Paths.get("/tmp/chrome-test");
        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, "ws://localhost:9222", tempDir, false);

        // Register shutdown hook
        Thread hook = ResourceCleanup.registerShutdownHook(chromeProcess);

        // Manually run the shutdown hook
        hook.start();

        // Verify destroyForcibly was NOT called since process is already dead
        verify(mockProcess, never()).destroyForcibly();

        // Clean up
        ResourceCleanup.removeShutdownHook(chromeProcess);
    }

    @Test
    void testIdempotentRemoval() {
        Process mockProcess = mock(Process.class);
        when(mockProcess.pid()).thenReturn(12345L);

        Path tempDir = Paths.get("/tmp/chrome-test");
        ChromeProcess chromeProcess = new ChromeProcess(mockProcess, "ws://localhost:9222", tempDir, false);

        int initialCount = ResourceCleanup.getRegisteredHookCount();

        // Register
        ResourceCleanup.registerShutdownHook(chromeProcess);
        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount + 1);

        // Remove multiple times
        boolean removed1 = ResourceCleanup.removeShutdownHook(chromeProcess);
        boolean removed2 = ResourceCleanup.removeShutdownHook(chromeProcess);
        boolean removed3 = ResourceCleanup.removeShutdownHook(chromeProcess);

        assertThat(removed1).isTrue();
        assertThat(removed2).isFalse(); // Already removed
        assertThat(removed3).isFalse(); // Already removed

        assertThat(ResourceCleanup.getRegisteredHookCount()).isEqualTo(initialCount);
    }
}
