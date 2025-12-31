package com.fostermoore.pdfviachrome.performance;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Memory profiling utility for PDF generation operations.
 * <p>
 * This class provides methods to measure and report memory usage during
 * PDF generation, helping identify memory leaks and optimize resource usage.
 * <p>
 * Run this class directly to execute memory profiling tests.
 */
public class MemoryProfiler {

    private static final Logger logger = LoggerFactory.getLogger(MemoryProfiler.class);
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /**
     * Captures current memory usage snapshot.
     */
    public static class MemorySnapshot {
        private final long heapUsed;
        private final long heapMax;
        private final long nonHeapUsed;
        private final long timestamp;

        public MemorySnapshot() {
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();

            this.heapUsed = heapMemory.getUsed();
            this.heapMax = heapMemory.getMax();
            this.nonHeapUsed = nonHeapMemory.getUsed();
            this.timestamp = System.currentTimeMillis();
        }

        public long getHeapUsedMB() {
            return heapUsed / 1024 / 1024;
        }

        public long getHeapMaxMB() {
            return heapMax / 1024 / 1024;
        }

        public long getNonHeapUsedMB() {
            return nonHeapUsed / 1024 / 1024;
        }

        public long getTotalUsedMB() {
            return (heapUsed + nonHeapUsed) / 1024 / 1024;
        }

        @Override
        public String toString() {
            return String.format("Heap: %d/%d MB, Non-Heap: %d MB, Total: %d MB",
                    getHeapUsedMB(), getHeapMaxMB(), getNonHeapUsedMB(), getTotalUsedMB());
        }
    }

    /**
     * Measures memory usage for a single PDF generation.
     */
    public static void profileSingleGeneration() throws Exception {
        logger.info("=== Single PDF Generation Memory Profile ===");

        String html = loadResource("/performance-test/simple.html");

        // Force GC before measurement
        System.gc();
        Thread.sleep(500);

        MemorySnapshot before = new MemorySnapshot();
        logger.info("Before generation: {}", before);

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(html).generate();

            MemorySnapshot during = new MemorySnapshot();
            logger.info("During generation: {}", during);
            logger.info("Memory increase: {} MB", during.getTotalUsedMB() - before.getTotalUsedMB());
            logger.info("PDF size: {} KB", pdf.length / 1024);
        }

        // Force GC after close
        System.gc();
        Thread.sleep(500);

        MemorySnapshot after = new MemorySnapshot();
        logger.info("After cleanup: {}", after);
        logger.info("Memory retained: {} MB", after.getTotalUsedMB() - before.getTotalUsedMB());
        logger.info("");
    }

    /**
     * Measures memory usage for multiple sequential generations.
     * This helps identify memory leaks.
     */
    public static void profileSequentialGenerations(int count) throws Exception {
        logger.info("=== Sequential Generations Memory Profile ({} iterations) ===", count);

        String html = loadResource("/performance-test/simple.html");

        System.gc();
        Thread.sleep(500);

        MemorySnapshot baseline = new MemorySnapshot();
        logger.info("Baseline: {}", baseline);

        List<MemorySnapshot> snapshots = new ArrayList<>();

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            for (int i = 1; i <= count; i++) {
                byte[] pdf = generator.fromHtml(html).generate();

                if (i % 10 == 0) {
                    MemorySnapshot snapshot = new MemorySnapshot();
                    snapshots.add(snapshot);
                    logger.info("After {} generations: {}", i, snapshot);
                }
            }
        }

        System.gc();
        Thread.sleep(500);

        MemorySnapshot final_ = new MemorySnapshot();
        logger.info("Final (after cleanup): {}", final_);

        // Analyze memory growth
        if (snapshots.size() > 1) {
            MemorySnapshot first = snapshots.get(0);
            MemorySnapshot last = snapshots.get(snapshots.size() - 1);
            long growth = last.getTotalUsedMB() - first.getTotalUsedMB();
            long perGeneration = growth / (count - 10);

            logger.info("Memory growth: {} MB ({} MB per generation)", growth, perGeneration);

            if (perGeneration > 1) {
                logger.warn("WARNING: Potential memory leak detected (>{} MB per generation)", perGeneration);
            } else {
                logger.info("Memory usage appears stable");
            }
        }

        logger.info("");
    }

    /**
     * Measures memory usage with generator reuse vs new instances.
     */
    public static void profileGeneratorReuse() throws Exception {
        logger.info("=== Generator Reuse vs New Instances ===");

        String html = loadResource("/performance-test/simple.html");
        int iterations = 20;

        // Test 1: New generator each time
        System.gc();
        Thread.sleep(500);
        MemorySnapshot beforeNew = new MemorySnapshot();

        long startNew = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
                generator.fromHtml(html).generate();
            }
        }
        long timeNew = System.currentTimeMillis() - startNew;

        System.gc();
        Thread.sleep(500);
        MemorySnapshot afterNew = new MemorySnapshot();

        logger.info("New instances: {} iterations in {} ms", iterations, timeNew);
        logger.info("  Memory: {} -> {} MB", beforeNew.getTotalUsedMB(), afterNew.getTotalUsedMB());

        // Test 2: Reuse generator
        System.gc();
        Thread.sleep(500);
        MemorySnapshot beforeReuse = new MemorySnapshot();

        long startReuse = System.currentTimeMillis();
        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            for (int i = 0; i < iterations; i++) {
                generator.fromHtml(html).generate();
            }
        }
        long timeReuse = System.currentTimeMillis() - startReuse;

        System.gc();
        Thread.sleep(500);
        MemorySnapshot afterReuse = new MemorySnapshot();

        logger.info("Reused instance: {} iterations in {} ms", iterations, timeReuse);
        logger.info("  Memory: {} -> {} MB", beforeReuse.getTotalUsedMB(), afterReuse.getTotalUsedMB());

        logger.info("Performance improvement: {:.1f}x faster with reuse",
                (double) timeNew / timeReuse);
        logger.info("");
    }

    /**
     * Measures memory usage for large document conversion.
     */
    public static void profileLargeDocument() throws Exception {
        logger.info("=== Large Document Memory Profile ===");

        String html = loadResource("/performance-test/large.html");

        System.gc();
        Thread.sleep(500);

        MemorySnapshot before = new MemorySnapshot();
        logger.info("Before generation: {}", before);

        long startTime = System.currentTimeMillis();

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(html).generate();

            long duration = System.currentTimeMillis() - startTime;

            MemorySnapshot peak = new MemorySnapshot();
            logger.info("Peak memory: {}", peak);
            logger.info("Memory increase: {} MB", peak.getTotalUsedMB() - before.getTotalUsedMB());
            logger.info("PDF size: {} KB", pdf.length / 1024);
            logger.info("Generation time: {} ms", duration);
        }

        System.gc();
        Thread.sleep(500);

        MemorySnapshot after = new MemorySnapshot();
        logger.info("After cleanup: {}", after);
        logger.info("Memory retained: {} MB", after.getTotalUsedMB() - before.getTotalUsedMB());
        logger.info("");
    }

    /**
     * Measures memory usage under concurrent load.
     */
    public static void profileConcurrentLoad() throws Exception {
        logger.info("=== Concurrent Load Memory Profile ===");

        String html = loadResource("/performance-test/complex.html");
        int concurrency = 4;

        System.gc();
        Thread.sleep(500);

        MemorySnapshot before = new MemorySnapshot();
        logger.info("Before concurrent load: {}", before);

        long startTime = System.currentTimeMillis();

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < concurrency; i++) {
                int threadNum = i + 1;
                Thread thread = new Thread(() -> {
                    try {
                        logger.info("Thread {} starting", threadNum);
                        byte[] pdf = generator.fromHtml(html).generate();
                        logger.info("Thread {} completed ({} KB)", threadNum, pdf.length / 1024);
                    } catch (Exception e) {
                        logger.error("Thread {} failed", threadNum, e);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join();
            }

            long duration = System.currentTimeMillis() - startTime;

            MemorySnapshot peak = new MemorySnapshot();
            logger.info("Peak memory: {}", peak);
            logger.info("Memory increase: {} MB", peak.getTotalUsedMB() - before.getTotalUsedMB());
            logger.info("Total time: {} ms", duration);
        }

        System.gc();
        Thread.sleep(500);

        MemorySnapshot after = new MemorySnapshot();
        logger.info("After cleanup: {}", after);
        logger.info("Memory retained: {} MB", after.getTotalUsedMB() - before.getTotalUsedMB());
        logger.info("");
    }

    /**
     * Loads a test resource file.
     */
    private static String loadResource(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(MemoryProfiler.class.getResource(resourcePath).toURI());
        return Files.readString(path);
    }

    /**
     * Main method to run all profiling tests.
     */
    public static void main(String[] args) {
        logger.info("Starting Memory Profiling Tests");
        logger.info("JVM Max Heap: {} MB", memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024);
        logger.info("");

        try {
            profileSingleGeneration();
            profileSequentialGenerations(50);
            profileGeneratorReuse();
            profileLargeDocument();
            profileConcurrentLoad();

            logger.info("Memory profiling completed successfully");
        } catch (Exception e) {
            logger.error("Memory profiling failed", e);
            System.exit(1);
        }
    }
}
