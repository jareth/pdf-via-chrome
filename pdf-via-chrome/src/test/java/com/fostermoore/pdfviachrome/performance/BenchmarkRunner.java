package com.fostermoore.pdfviachrome.performance;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Convenient runner for JMH benchmarks.
 * <p>
 * This class provides a simple way to run performance benchmarks with
 * pre-configured settings. Results are saved to JSON files for analysis.
 * <p>
 * Usage: Run this class directly from your IDE or command line.
 */
public class BenchmarkRunner {

    /**
     * Runs all benchmarks with default settings.
     */
    public static void runAllBenchmarks() throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String resultFile = "target/benchmark-results-" + timestamp + ".json";

        Options options = new OptionsBuilder()
                .include(PerformanceBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .shouldFailOnError(true)
                .result(resultFile)
                .resultFormat(ResultFormatType.JSON)
                .build();

        System.out.println("Running benchmarks...");
        System.out.println("Results will be saved to: " + resultFile);
        System.out.println();

        new Runner(options).run();

        System.out.println();
        System.out.println("Benchmarks completed successfully!");
        System.out.println("Results saved to: " + resultFile);
    }

    /**
     * Runs a quick subset of benchmarks for faster feedback.
     */
    public static void runQuickBenchmarks() throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String resultFile = "target/benchmark-quick-" + timestamp + ".json";

        Options options = new OptionsBuilder()
                .include(PerformanceBenchmark.class.getSimpleName())
                // Only run simple scenarios for quick feedback
                .include(".*simpleHtmlConversion")
                .include(".*generatorReuse")
                .include(".*chromeStartup")
                .warmupIterations(2)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .shouldFailOnError(true)
                .result(resultFile)
                .resultFormat(ResultFormatType.JSON)
                .build();

        System.out.println("Running quick benchmarks...");
        System.out.println("Results will be saved to: " + resultFile);
        System.out.println();

        new Runner(options).run();

        System.out.println();
        System.out.println("Quick benchmarks completed!");
        System.out.println("Results saved to: " + resultFile);
    }

    /**
     * Runs benchmarks with detailed profiling.
     */
    public static void runWithProfiling() throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String resultFile = "target/benchmark-profiled-" + timestamp + ".json";

        Options options = new OptionsBuilder()
                .include(PerformanceBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(2))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(2))
                .forks(1)
                .shouldFailOnError(true)
                // Add GC profiler to track garbage collection
                .addProfiler("gc")
                .result(resultFile)
                .resultFormat(ResultFormatType.JSON)
                .build();

        System.out.println("Running benchmarks with GC profiling...");
        System.out.println("Results will be saved to: " + resultFile);
        System.out.println();

        new Runner(options).run();

        System.out.println();
        System.out.println("Profiled benchmarks completed!");
        System.out.println("Results saved to: " + resultFile);
    }

    /**
     * Main method with command-line options.
     */
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";

        System.out.println("=== PDF Generation Performance Benchmarks ===");
        System.out.println();

        switch (mode.toLowerCase()) {
            case "quick":
                runQuickBenchmarks();
                break;
            case "profile":
                runWithProfiling();
                break;
            case "all":
            default:
                runAllBenchmarks();
                break;
        }
    }
}
