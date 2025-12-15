package com.github.headlesschromepdf.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProcessRegistry.
 */
class ProcessRegistryTest {

    private ProcessRegistry registry;
    private List<Process> testProcesses;

    @BeforeEach
    void setUp() {
        registry = ProcessRegistry.getInstance();
        registry.clearForTesting();
        testProcesses = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // Clean up any test processes
        for (Process process : testProcesses) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        registry.clearForTesting();
    }

    @Test
    void testGetInstance_returnsSameInstance() {
        ProcessRegistry instance1 = ProcessRegistry.getInstance();
        ProcessRegistry instance2 = ProcessRegistry.getInstance();

        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    void testRegister_withValidProcess() throws IOException {
        Process process = createDummyProcess();
        testProcesses.add(process);

        registry.register(process);

        assertThat(registry.getActiveProcessCount()).isEqualTo(1);
        assertThat(registry.isRegistered(process)).isTrue();
    }

    @Test
    void testRegister_withNullProcess_throwsException() {
        assertThatThrownBy(() -> registry.register(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Process cannot be null");
    }

    @Test
    void testRegister_multipleProcesses() throws IOException {
        Process process1 = createDummyProcess();
        Process process2 = createDummyProcess();
        testProcesses.add(process1);
        testProcesses.add(process2);

        registry.register(process1);
        registry.register(process2);

        assertThat(registry.getActiveProcessCount()).isEqualTo(2);
        assertThat(registry.isRegistered(process1)).isTrue();
        assertThat(registry.isRegistered(process2)).isTrue();
    }

    @Test
    void testUnregister_withRegisteredProcess() throws IOException {
        Process process = createDummyProcess();
        testProcesses.add(process);

        registry.register(process);
        boolean result = registry.unregister(process);

        assertThat(result).isTrue();
        assertThat(registry.getActiveProcessCount()).isZero();
        assertThat(registry.isRegistered(process)).isFalse();
    }

    @Test
    void testUnregister_withUnregisteredProcess() throws IOException {
        Process process = createDummyProcess();
        testProcesses.add(process);

        boolean result = registry.unregister(process);

        assertThat(result).isFalse();
        assertThat(registry.getActiveProcessCount()).isZero();
    }

    @Test
    void testUnregister_withNullProcess() {
        boolean result = registry.unregister(null);

        assertThat(result).isFalse();
    }

    @Test
    void testGetActiveProcessCount_initially() {
        assertThat(registry.getActiveProcessCount()).isZero();
    }

    @Test
    void testGetActiveProcessCount_afterRegistrations() throws IOException {
        Process process1 = createDummyProcess();
        Process process2 = createDummyProcess();
        Process process3 = createDummyProcess();
        testProcesses.add(process1);
        testProcesses.add(process2);
        testProcesses.add(process3);

        registry.register(process1);
        assertThat(registry.getActiveProcessCount()).isEqualTo(1);

        registry.register(process2);
        assertThat(registry.getActiveProcessCount()).isEqualTo(2);

        registry.register(process3);
        assertThat(registry.getActiveProcessCount()).isEqualTo(3);

        registry.unregister(process2);
        assertThat(registry.getActiveProcessCount()).isEqualTo(2);
    }

    @Test
    void testGetActiveProcesses_returnsSnapshot() throws IOException {
        Process process1 = createDummyProcess();
        Process process2 = createDummyProcess();
        testProcesses.add(process1);
        testProcesses.add(process2);

        registry.register(process1);
        registry.register(process2);

        List<ProcessRegistry.ProcessMetadata> snapshot = registry.getActiveProcesses();

        assertThat(snapshot).hasSize(2);
        assertThat(snapshot.stream().map(ProcessRegistry.ProcessMetadata::getPid))
            .contains(process1.pid(), process2.pid());

        // Verify it's unmodifiable
        assertThatThrownBy(() -> snapshot.clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testGetMetadata_forRegisteredProcess() throws IOException {
        Process process = createDummyProcess();
        testProcesses.add(process);

        registry.register(process);
        ProcessRegistry.ProcessMetadata metadata = registry.getMetadata(process);

        assertThat(metadata).isNotNull();
        assertThat(metadata.getPid()).isEqualTo(process.pid());
        assertThat(metadata.getStartTime()).isNotNull();
        assertThat(metadata.wasAliveAtRegistration()).isTrue();
    }

    @Test
    void testGetMetadata_forUnregisteredProcess() throws IOException {
        Process process = createDummyProcess();
        testProcesses.add(process);

        ProcessRegistry.ProcessMetadata metadata = registry.getMetadata(process);

        assertThat(metadata).isNull();
    }

    @Test
    void testIsRegistered_withNullProcess() {
        assertThat(registry.isRegistered(null)).isFalse();
    }

    @Test
    void testPerformHealthCheck_withAllAliveProcesses() throws IOException {
        Process process1 = createDummyProcess();
        Process process2 = createDummyProcess();
        testProcesses.add(process1);
        testProcesses.add(process2);

        registry.register(process1);
        registry.register(process2);

        int cleaned = registry.performHealthCheck();

        assertThat(cleaned).isZero();
        assertThat(registry.getActiveProcessCount()).isEqualTo(2);
    }

    @Test
    void testPerformHealthCheck_withDeadProcesses() throws IOException, InterruptedException {
        Process process1 = createDummyProcess();
        Process process2 = createDummyProcess();
        testProcesses.add(process1);
        testProcesses.add(process2);

        registry.register(process1);
        registry.register(process2);

        // Kill one process
        process1.destroyForcibly();
        process1.waitFor(2, TimeUnit.SECONDS);

        int cleaned = registry.performHealthCheck();

        assertThat(cleaned).isEqualTo(1);
        assertThat(registry.getActiveProcessCount()).isEqualTo(1);
        assertThat(registry.isRegistered(process1)).isFalse();
        assertThat(registry.isRegistered(process2)).isTrue();
    }

    @Test
    void testCleanupAll_withNoProcesses() {
        int terminated = registry.cleanupAll();

        assertThat(terminated).isZero();
        assertThat(registry.getActiveProcessCount()).isZero();
    }

    @Test
    void testCleanupAll_withActiveProcesses() throws IOException, InterruptedException {
        Process process1 = createDummyProcess();
        Process process2 = createDummyProcess();
        testProcesses.add(process1);
        testProcesses.add(process2);

        registry.register(process1);
        registry.register(process2);

        int terminated = registry.cleanupAll();

        assertThat(terminated).isEqualTo(2);
        assertThat(registry.getActiveProcessCount()).isZero();

        // Wait a bit for processes to fully terminate
        Thread.sleep(500);

        assertThat(process1.isAlive()).isFalse();
        assertThat(process2.isAlive()).isFalse();
    }

    @Test
    void testCleanupAll_withMixedProcesses() throws IOException, InterruptedException {
        Process process1 = createDummyProcess();
        Process process2 = createDummyProcess();
        testProcesses.add(process1);
        testProcesses.add(process2);

        registry.register(process1);
        registry.register(process2);

        // Kill one process before cleanup
        process1.destroyForcibly();
        process1.waitFor(2, TimeUnit.SECONDS);

        int terminated = registry.cleanupAll();

        // Only the alive process should be counted as terminated
        assertThat(terminated).isEqualTo(1);
        assertThat(registry.getActiveProcessCount()).isZero();
    }

    @Test
    void testThreadSafety_concurrentRegistrations() throws InterruptedException, IOException {
        int threadCount = 10;
        int processesPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Process> allProcesses = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < processesPerThread; j++) {
                        Process process = createDummyProcess();
                        synchronized (allProcesses) {
                            allProcesses.add(process);
                        }
                        registry.register(process);
                        successCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    // Ignore for test
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Add all created processes to test cleanup
        testProcesses.addAll(allProcesses);

        assertThat(successCount.get()).isEqualTo(threadCount * processesPerThread);
        assertThat(registry.getActiveProcessCount()).isEqualTo(threadCount * processesPerThread);
    }

    @Test
    void testThreadSafety_concurrentUnregistrations() throws InterruptedException, IOException {
        int processCount = 20;
        List<Process> processes = new ArrayList<>();

        // Register processes first
        for (int i = 0; i < processCount; i++) {
            Process process = createDummyProcess();
            processes.add(process);
            testProcesses.add(process);
            registry.register(process);
        }

        // Unregister concurrently
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(processCount);

        for (Process process : processes) {
            executor.submit(() -> {
                try {
                    registry.unregister(process);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(registry.getActiveProcessCount()).isZero();
    }

    @Test
    void testProcessMetadata_ageIncreases() throws IOException, InterruptedException {
        Process process = createDummyProcess();
        testProcesses.add(process);

        registry.register(process);
        ProcessRegistry.ProcessMetadata metadata = registry.getMetadata(process);

        long age1 = metadata.getAgeMillis();
        Thread.sleep(100);
        long age2 = metadata.getAgeMillis();

        assertThat(age2).isGreaterThan(age1);
        assertThat(age2 - age1).isGreaterThanOrEqualTo(100);
    }

    @Test
    void testProcessMetadata_toString() throws IOException {
        Process process = createDummyProcess();
        testProcesses.add(process);

        registry.register(process);
        ProcessRegistry.ProcessMetadata metadata = registry.getMetadata(process);

        String str = metadata.toString();

        assertThat(str).contains("ProcessMetadata");
        assertThat(str).contains("pid=" + process.pid());
        assertThat(str).contains("startTime=");
        assertThat(str).contains("age=");
        assertThat(str).contains("wasAlive=true");
    }

    /**
     * Creates a dummy process for testing.
     * On Windows, uses "timeout" command, on Unix uses "sleep".
     */
    private Process createDummyProcess() throws IOException {
        ProcessBuilder builder;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows: use timeout command (waits for specified seconds)
            builder = new ProcessBuilder("timeout", "/t", "300", "/nobreak");
        } else {
            // Unix/Linux/Mac: use sleep command
            builder = new ProcessBuilder("sleep", "300");
        }

        return builder.start();
    }
}
