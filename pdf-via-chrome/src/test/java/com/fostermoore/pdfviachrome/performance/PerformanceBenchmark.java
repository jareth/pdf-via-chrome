package com.fostermoore.pdfviachrome.performance;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeManager;
import com.fostermoore.pdfviachrome.chrome.ChromeOptions;
import com.fostermoore.pdfviachrome.chrome.ChromeProcess;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * JMH benchmarks for PDF generation performance.
 * <p>
 * Run with: mvn test-compile exec:java -Dexec.classpathScope=test
 * -Dexec.mainClass=org.openjdk.jmh.Main
 * -Dexec.args="PerformanceBenchmark -f 1 -wi 3 -i 5"
 * <p>
 * Or create a runner class and execute from IDE.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerformanceBenchmark {

    // HTML content for different test scenarios
    private String simpleHtml;
    private String complexHtml;
    private String largeHtml;

    // Shared PdfGenerator for testing instance reuse
    private PdfGenerator sharedGenerator;

    /**
     * Setup method - loads test HTML files and initializes shared resources.
     */
    @Setup(Level.Trial)
    public void setup() throws IOException, URISyntaxException {
        // Load test HTML files
        simpleHtml = loadResource("/performance-test/simple.html");
        complexHtml = loadResource("/performance-test/complex.html");
        largeHtml = loadResource("/performance-test/large.html");

        // Initialize shared generator for reuse tests
        sharedGenerator = PdfGenerator.create()
                .withHeadless(true)
                .build();
    }

    /**
     * Cleanup method - closes shared resources.
     */
    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        if (sharedGenerator != null) {
            sharedGenerator.close();
        }
    }

    /**
     * Benchmark: Simple HTML conversion (baseline).
     * Tests the fastest conversion scenario with minimal content.
     */
    @Benchmark
    public void simpleHtmlConversion(Blackhole blackhole) throws Exception {
        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(simpleHtml).generate();
            blackhole.consume(pdf);
        }
    }

    /**
     * Benchmark: Complex HTML conversion with CSS and multiple pages.
     * Tests realistic document conversion with styled content.
     */
    @Benchmark
    public void complexHtmlConversion(Blackhole blackhole) throws Exception {
        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(complexHtml).generate();
            blackhole.consume(pdf);
        }
    }

    /**
     * Benchmark: Large document conversion (100+ pages).
     * Tests performance with extensive content.
     */
    @Benchmark
    public void largeDocumentConversion(Blackhole blackhole) throws Exception {
        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(largeHtml).generate();
            blackhole.consume(pdf);
        }
    }

    /**
     * Benchmark: Generator instance reuse.
     * Tests performance when reusing the same PdfGenerator for multiple conversions.
     * This eliminates Chrome startup overhead.
     */
    @Benchmark
    public void generatorReuse(Blackhole blackhole) throws Exception {
        byte[] pdf = sharedGenerator.fromHtml(simpleHtml).generate();
        blackhole.consume(pdf);
    }

    /**
     * Benchmark: Custom PDF options.
     * Tests conversion with various PDF configuration options.
     */
    @Benchmark
    public void customPdfOptions(Blackhole blackhole) throws Exception {
        PdfOptions options = PdfOptions.builder()
                .paperSize(PdfOptions.PaperFormat.A4)
                .landscape(true)
                .printBackground(true)
                .margins("1cm")
                .scale(0.9)
                .build();

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(simpleHtml)
                    .withOptions(options)
                    .generate();
            blackhole.consume(pdf);
        }
    }

    /**
     * Benchmark: Chrome startup time.
     * Measures the time to start Chrome and establish CDP connection.
     */
    @Benchmark
    public void chromeStartup(Blackhole blackhole) throws Exception {
        ChromeOptions options = ChromeOptions.builder()
                .headless(true)
                .build();

        try (ChromeManager manager = new ChromeManager(options)) {
            ChromeProcess process = manager.start();
            blackhole.consume(process.getWebSocketDebuggerUrl());
        }
    }

    /**
     * Benchmark: Multiple sequential conversions with same generator.
     * Tests throughput with instance reuse (5 conversions).
     */
    @Benchmark
    public void sequentialConversions(Blackhole blackhole) throws Exception {
        List<byte[]> pdfs = new ArrayList<>();

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            for (int i = 0; i < 5; i++) {
                byte[] pdf = generator.fromHtml(simpleHtml).generate();
                pdfs.add(pdf);
            }
            blackhole.consume(pdfs);
        }
    }

    /**
     * Benchmark: Concurrent PDF generation.
     * Tests parallel processing capability with 4 concurrent workers.
     */
    @Benchmark
    public void concurrentGeneration(Blackhole blackhole) throws Exception {
        int concurrency = 4;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<byte[]>> futures = new ArrayList<>();

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            // Submit 4 concurrent conversion tasks
            for (int i = 0; i < concurrency; i++) {
                futures.add(executor.submit(() -> generator.fromHtml(simpleHtml).generate()));
            }

            // Wait for all to complete
            List<byte[]> results = new ArrayList<>();
            for (Future<byte[]> future : futures) {
                results.add(future.get());
            }
            blackhole.consume(results);
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Benchmark: Header and footer template processing.
     * Tests performance impact of header/footer generation.
     */
    @Benchmark
    public void headerFooterGeneration(Blackhole blackhole) throws Exception {
        PdfOptions options = PdfOptions.builder()
                .standardHeaderFooter()
                .build();

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(complexHtml)
                    .withOptions(options)
                    .generate();
            blackhole.consume(pdf);
        }
    }

    /**
     * Benchmark: CSS injection with conversion.
     * Tests performance impact of custom CSS injection.
     */
    @Benchmark
    public void cssInjection(Blackhole blackhole) throws Exception {
        String customCss = """
                body { font-size: 12pt; }
                .no-print { display: none; }
                @media print {
                    .page-break { page-break-after: always; }
                }
                """;

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(complexHtml)
                    .withCustomCss(customCss)
                    .generate();
            blackhole.consume(pdf);
        }
    }

    /**
     * Benchmark: JavaScript execution with conversion.
     * Tests performance impact of custom JavaScript execution.
     */
    @Benchmark
    public void javaScriptExecution(Blackhole blackhole) throws Exception {
        String jsCode = """
                document.querySelectorAll('.ads').forEach(el => el.remove());
                document.title = 'Modified Document';
                """;

        try (PdfGenerator generator = PdfGenerator.create().withHeadless(true).build()) {
            byte[] pdf = generator.fromHtml(complexHtml)
                    .executeJavaScript(jsCode)
                    .generate();
            blackhole.consume(pdf);
        }
    }

    /**
     * Helper method to load test resources.
     */
    private String loadResource(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(getClass().getResource(resourcePath).toURI());
        return Files.readString(path);
    }

    /**
     * Main method to run benchmarks.
     * This allows running benchmarks directly without maven-exec-plugin.
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
