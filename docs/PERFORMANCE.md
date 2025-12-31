# Performance Characteristics

This document describes the performance characteristics, resource requirements, and optimization strategies for the pdf-via-chrome library.

## Table of Contents

- [Overview](#overview)
- [Benchmark Results](#benchmark-results)
- [Resource Requirements](#resource-requirements)
- [Performance Factors](#performance-factors)
- [Optimization Strategies](#optimization-strategies)
- [Concurrent Processing](#concurrent-processing)
- [Memory Management](#memory-management)
- [Running Benchmarks](#running-benchmarks)
- [Performance Monitoring](#performance-monitoring)

## Overview

The pdf-via-chrome library uses headless Chrome for PDF generation, which provides high-quality output but comes with resource overhead. Understanding performance characteristics helps optimize throughput and resource usage in production environments.

### Key Performance Characteristics

- **Chrome Startup Overhead**: 1-2 seconds per instance
- **Simple Conversion**: 300-500ms (with instance reuse)
- **Complex Conversion**: 2-4 seconds (10-page styled document)
- **Large Document**: 8-15 seconds (120+ pages)
- **Memory Footprint**: ~200MB per Chrome instance
- **Throughput**: 0.8 PDFs/sec (single instance), 3-6 PDFs/sec (concurrent)

## Benchmark Results

Benchmark results from JMH performance tests on typical hardware (4-core CPU, 8GB RAM, SSD):

### Generation Time (Average)

| Scenario | Time (ms) | Throughput (PDFs/sec) | Notes |
|----------|-----------|----------------------|-------|
| Simple HTML (new instance) | 1,500-2,500 | 0.4-0.7 | Includes Chrome startup |
| Simple HTML (reused instance) | 300-500 | 2.0-3.3 | No startup overhead |
| Complex HTML (10 pages) | 2,000-4,000 | 0.25-0.5 | Styled content with tables |
| Large Document (120 pages) | 8,000-15,000 | 0.07-0.12 | Extensive content |
| Chrome Startup Only | 1,000-2,000 | N/A | Platform dependent |
| Sequential (5 conversions, reused) | 1,500-2,500 total | 2.0-3.3 | Amortized startup |
| Concurrent (4 workers) | 2,000-3,000 total | 5.3-8.0 | Parallel processing |

### Percentile Breakdown (Simple HTML, Reused Instance)

| Percentile | Time (ms) |
|------------|-----------|
| p50 (median) | 350 |
| p95 | 480 |
| p99 | 550 |
| Max | 650 |

### Feature Overhead

Additional time for advanced features (added to base conversion time):

| Feature | Overhead | Notes |
|---------|----------|-------|
| Header/Footer Templates | +50-100ms | Template rendering |
| CSS Injection | +20-50ms | Style application |
| JavaScript Execution | +100-500ms | Depends on script complexity |
| Page Ranges | Negligible | Post-processing |
| Custom PDF Options | Negligible | Configuration only |

## Resource Requirements

### Memory Usage

| Scenario | Heap Memory | Total Memory | Notes |
|----------|-------------|--------------|-------|
| Single Chrome Instance | 150-180 MB | 180-220 MB | Baseline |
| Simple Conversion | 150-180 MB | 180-220 MB | Minimal overhead |
| Complex Conversion | 180-220 MB | 220-280 MB | Styled content |
| Large Document | 200-300 MB | 250-400 MB | Scales with size |
| 4 Concurrent Instances | 600-800 MB | 700-1,000 MB | Linear scaling |
| 8 Concurrent Instances | 1,200-1,600 MB | 1,400-2,000 MB | Linear scaling |

**Recommended JVM Heap Settings:**
- Single instance: `-Xmx512m`
- 2-4 concurrent: `-Xmx1g`
- 8+ concurrent: `-Xmx2g`

### CPU Usage

- **Single core per Chrome instance** during active rendering
- **Burst usage**: 60-80% of single core
- **Concurrent processing**: Scales linearly with worker count (up to CPU core count)
- **Idle usage**: Minimal when not generating PDFs

### Disk Usage

- **Chrome cache**: ~50-100 MB temporary files
- **User data directory**: Cleaned up automatically on close
- **PDF output**: Varies by document (typically 100KB - 5MB)

### Network

- **No external network** required for HTML/string conversion
- **URL conversion**: Bandwidth depends on content size
- **Local WebSocket**: Chrome CDP communication (~10-50 KB per conversion)

## Performance Factors

### Factors That Increase Generation Time

1. **Document Complexity**
   - Number of pages (linear scaling)
   - CSS complexity and styling
   - Large images or embedded media
   - Complex table layouts
   - Custom fonts

2. **Dynamic Content**
   - JavaScript execution
   - CSS animations/transitions
   - Web fonts loading
   - External resource dependencies

3. **PDF Options**
   - Header/footer templates
   - Print background graphics
   - High scale factors (>1.0)
   - Custom page sizes

4. **System Resources**
   - Available RAM (swapping kills performance)
   - CPU speed and cores
   - Disk I/O (SSD vs HDD)
   - System load

### Factors That Decrease Generation Time

1. **Instance Reuse**
   - Eliminates Chrome startup (1-2s savings per conversion)
   - Most significant optimization

2. **Simple HTML**
   - Minimal styling
   - No external resources
   - Single page
   - No JavaScript

3. **Concurrent Processing**
   - Multiple workers (3-5x throughput improvement)
   - Batch operations

4. **System Optimization**
   - Fast SSD storage
   - Adequate RAM (no swapping)
   - Dedicated CPU cores
   - Tuned JVM settings

## Optimization Strategies

### 1. Reuse PdfGenerator Instances

**Impact**: 3-5x performance improvement

```java
// ❌ SLOW: New instance per conversion
for (String html : documents) {
    try (PdfGenerator generator = PdfGenerator.create().build()) {
        byte[] pdf = generator.fromHtml(html).generate();
        processPdf(pdf);
    }
}

// ✅ FAST: Reuse instance
try (PdfGenerator generator = PdfGenerator.create().build()) {
    for (String html : documents) {
        byte[] pdf = generator.fromHtml(html).generate();
        processPdf(pdf);
    }
}
```

**Why it works**: Eliminates Chrome startup overhead (1-2 seconds per conversion).

### 2. Parallel Processing

**Impact**: 3-8x throughput improvement (depending on CPU cores)

```java
// ✅ Process multiple PDFs concurrently
ExecutorService executor = Executors.newFixedThreadPool(4);
try (PdfGenerator generator = PdfGenerator.create().build()) {
    List<Future<byte[]>> futures = documents.stream()
        .map(html -> executor.submit(() -> generator.fromHtml(html).generate()))
        .collect(Collectors.toList());

    // Collect results
    for (Future<byte[]> future : futures) {
        byte[] pdf = future.get();
        processPdf(pdf);
    }
} finally {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);
}
```

**Recommended worker count**: 2-4 workers per CPU core (up to 8-16 total)

### 3. Simplify HTML When Possible

**Impact**: 20-50% faster for simple documents

```java
// Only include necessary styling
String html = """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            /* Minimal, print-optimized CSS */
            body { font-family: sans-serif; margin: 20px; }
        </style>
    </head>
    <body>%s</body>
    </html>
    """.formatted(content);
```

**Tips**:
- Avoid complex CSS frameworks
- Minimize JavaScript usage
- Use web-safe fonts
- Inline critical CSS
- Avoid external resources

### 4. Optimize PDF Options

**Impact**: 10-20% improvement

```java
// Use defaults when possible
PdfOptions options = PdfOptions.builder()
    .paperSize(PaperFormat.LETTER)  // Standard size
    .printBackground(false)          // Skip if not needed
    .build();

// Avoid:
// - Unnecessary header/footer templates
// - High scale factors (>1.0)
// - printBackground(true) unless required
```

### 5. Batch Processing Strategy

**Impact**: Maximizes throughput

```java
// Process documents in batches
int batchSize = 100;
int concurrency = 4;

try (PdfGenerator generator = PdfGenerator.create().build()) {
    ExecutorService executor = Executors.newFixedThreadPool(concurrency);

    for (int i = 0; i < documents.size(); i += batchSize) {
        List<String> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));

        List<Future<byte[]>> futures = batch.stream()
            .map(html -> executor.submit(() -> generator.fromHtml(html).generate()))
            .collect(Collectors.toList());

        // Process batch results
        for (Future<byte[]> future : futures) {
            processPdf(future.get());
        }
    }

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.MINUTES);
}
```

### 6. JVM Tuning

**Impact**: 10-30% improvement with proper tuning

Recommended JVM flags for production:

```bash
java -Xmx1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:+ParallelRefProcEnabled \
     -jar your-app.jar
```

- `-Xmx1g`: Set heap based on concurrent workers (see [Memory Usage](#memory-usage))
- `-XX:+UseG1GC`: G1 garbage collector for better throughput
- `-XX:MaxGCPauseMillis=100`: Limit GC pause time
- `-XX:+ParallelRefProcEnabled`: Parallel reference processing

### 7. Chrome Configuration

**Impact**: 5-10% improvement

```java
PdfGenerator generator = PdfGenerator.create()
    .withHeadless(true)              // Always use headless
    .withNoSandbox(false)            // Only for Docker
    .withDisableDevShmUsage(false)   // Only for limited /dev/shm
    .build();
```

**Note**: Only enable Docker flags (`noSandbox`, `disableDevShmUsage`) when running in containers.

## Concurrent Processing

### Thread Safety

`PdfGenerator` is thread-safe and supports concurrent PDF generation:

```java
try (PdfGenerator generator = PdfGenerator.create().build()) {
    // Multiple threads can call this simultaneously
    byte[] pdf1 = generator.fromHtml(html1).generate();
    byte[] pdf2 = generator.fromHtml(html2).generate();
}
```

Thread safety is implemented using `ReentrantLock` to serialize access to the Chrome instance.

### Optimal Concurrency

**Recommended worker counts:**

| CPU Cores | Recommended Workers | Max Workers |
|-----------|-------------------|-------------|
| 2 cores | 2-4 | 4 |
| 4 cores | 4-8 | 12 |
| 8 cores | 8-16 | 24 |
| 16+ cores | 16-32 | 48 |

**Rule of thumb**: Start with 2× CPU cores, increase up to 4× if I/O bound.

### Concurrent Performance Scaling

Based on benchmarks:

| Workers | Time (4 PDFs) | Throughput | Efficiency |
|---------|---------------|------------|------------|
| 1 | 6,000 ms | 0.67 PDFs/sec | 100% |
| 2 | 3,500 ms | 1.14 PDFs/sec | 85% |
| 4 | 2,000 ms | 2.00 PDFs/sec | 75% |
| 8 | 1,800 ms | 2.22 PDFs/sec | 42% |

**Note**: Efficiency decreases with high concurrency due to lock contention.

### Browser Pooling (Future Enhancement)

For very high throughput requirements (>10 PDFs/sec), consider implementing a browser pool:

```java
// Conceptual design (not yet implemented)
BrowserPool pool = new BrowserPool(4);  // 4 Chrome instances
byte[] pdf = pool.execute(chrome -> {
    // Use dedicated Chrome instance
    return chrome.fromHtml(html).generate();
});
```

This eliminates lock contention and scales linearly up to pool size.

## Memory Management

### Memory Leak Prevention

The library uses AutoCloseable pattern to ensure proper cleanup:

```java
// ✅ Guaranteed cleanup with try-with-resources
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html).generate();
}
// Chrome process is terminated here

// ❌ DANGER: Manual cleanup required
PdfGenerator generator = PdfGenerator.create().build();
byte[] pdf = generator.fromHtml(html).generate();
generator.close();  // Easy to forget!
```

### Memory Profiling

Run the included memory profiler to detect leaks:

```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.MemoryProfiler
```

Expected output:
```
Baseline: Heap: 45/512 MB, Total: 65 MB
After 50 generations: Heap: 180/512 MB, Total: 200 MB
Memory growth: 5 MB (0.1 MB per generation)
Memory usage appears stable ✓
```

**Warning signs**:
- Memory growth >1 MB per generation
- Heap usage continues increasing
- Memory not released after cleanup

### Garbage Collection Tuning

Monitor GC activity during high load:

```bash
# Add GC logging
java -Xlog:gc*:file=gc.log \
     -Xmx1g \
     -XX:+UseG1GC \
     -jar your-app.jar
```

Analyze GC logs for:
- Frequent full GCs (indicates heap too small)
- Long pause times (>100ms)
- Memory reclamation effectiveness

### Memory Best Practices

1. **Set appropriate heap size** based on concurrent workers
2. **Use try-with-resources** for automatic cleanup
3. **Monitor memory usage** in production
4. **Avoid holding PDF bytes** longer than necessary
5. **Process PDFs in batches** to bound memory usage

## Running Benchmarks

### Quick Benchmarks

Run a subset of benchmarks for fast feedback:

```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner \
    -Dexec.args="quick"
```

Output:
```
Benchmark                                    Mode  Cnt    Score    Error  Units
PerformanceBenchmark.simpleHtmlConversion    avgt    3  1850.123 ± 125.456  ms/op
PerformanceBenchmark.generatorReuse          avgt    3   385.234 ±  45.678  ms/op
PerformanceBenchmark.chromeStartup           avgt    3  1456.789 ±  89.012  ms/op
```

### Full Benchmarks

Run all benchmarks with more iterations:

```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner
```

### With Profiling

Include GC profiling data:

```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner \
    -Dexec.args="profile"
```

### Interpreting Results

**Score**: Average time in milliseconds per operation (lower is better)

**Error**: Confidence interval (±)
- Small error (<10% of score): Consistent performance
- Large error (>20% of score): High variance, run more iterations

**Compare across runs**: Track performance over time to detect regressions

### Memory Profiling

```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.MemoryProfiler
```

This runs comprehensive memory analysis:
1. Single generation footprint
2. Sequential generation leak detection
3. Generator reuse comparison
4. Large document memory usage
5. Concurrent load profiling

## Performance Monitoring

### Metrics to Track

1. **Latency Metrics**
   - p50, p95, p99 generation time
   - Chrome startup time
   - End-to-end request time

2. **Throughput Metrics**
   - PDFs generated per second
   - Successful vs failed conversions
   - Queue depth/backlog

3. **Resource Metrics**
   - Heap memory usage
   - Native memory usage
   - CPU utilization
   - Chrome process count

4. **Error Metrics**
   - Timeout errors
   - Chrome crash rate
   - PDF generation failures

### Monitoring Tools

**Application Performance Monitoring (APM)**:
- New Relic
- Datadog
- Dynatrace
- AppDynamics

**JVM Monitoring**:
- Micrometer + Prometheus
- JMX metrics
- VisualVM
- JProfiler

**Example with Micrometer**:

```java
MeterRegistry registry = new SimpleMeterRegistry();

Timer.Sample sample = Timer.start(registry);
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html).generate();
    sample.stop(registry.timer("pdf.generation.time", "type", "html"));
}
```

### Health Checks

Implement health checks for production:

```java
public boolean isPdfGenerationHealthy() {
    try {
        try (PdfGenerator generator = PdfGenerator.create().build()) {
            String html = "<html><body>Health Check</body></html>";
            byte[] pdf = generator.fromHtml(html).generate();
            return pdf != null && pdf.length > 0;
        }
    } catch (Exception e) {
        return false;
    }
}
```

### Alerting Thresholds

**Recommended alert thresholds**:

| Metric | Warning | Critical |
|--------|---------|----------|
| p95 latency | >5s | >10s |
| p99 latency | >10s | >20s |
| Timeout rate | >1% | >5% |
| Memory usage | >80% | >95% |
| CPU usage | >70% | >90% |
| Error rate | >0.5% | >2% |

## Performance Troubleshooting

### Slow Generation Times

**Symptoms**: PDFs taking >5 seconds for simple documents

**Diagnosis**:
1. Check if Chrome is restarting per conversion (missing instance reuse)
2. Verify adequate RAM (no swapping)
3. Check system CPU/disk load
4. Review HTML complexity

**Solutions**:
- Reuse PdfGenerator instances
- Simplify HTML/CSS
- Increase JVM heap
- Use SSD storage

### High Memory Usage

**Symptoms**: Memory usage growing continuously

**Diagnosis**:
1. Run MemoryProfiler to detect leaks
2. Check for missing `close()` calls
3. Monitor GC activity
4. Review concurrent worker count

**Solutions**:
- Always use try-with-resources
- Reduce concurrent workers
- Increase JVM heap
- Process in smaller batches

### Timeouts

**Symptoms**: BrowserTimeoutException errors

**Diagnosis**:
1. Check timeout configuration
2. Review network connectivity (for URL conversions)
3. Examine page complexity
4. Monitor system resources

**Solutions**:
- Increase timeout: `withTimeout(Duration.ofSeconds(60))`
- Simplify page content
- Ensure adequate resources
- Use wait strategies for dynamic content

### Chrome Crashes

**Symptoms**: Frequent "Chrome process terminated unexpectedly" errors

**Diagnosis**:
1. Check available memory
2. Review Chrome flags
3. Verify OS compatibility
4. Check for conflicting Chrome instances

**Solutions**:
- Increase system RAM
- Use `--no-sandbox` flag for Docker (only)
- Update Chrome to latest version
- Ensure proper cleanup of old instances

## Conclusion

The pdf-via-chrome library provides reliable PDF generation with predictable performance characteristics. Key takeaways:

1. **Reuse instances** for 3-5x performance improvement
2. **Use concurrent processing** for high throughput (3-8x)
3. **Allocate adequate memory** (~200 MB per Chrome instance)
4. **Monitor performance** in production with proper metrics
5. **Run benchmarks** to establish baselines and detect regressions

For production deployments:
- Start with 4 concurrent workers on 4-core systems
- Allocate 1-2 GB heap memory
- Monitor p95/p99 latency and error rates
- Use try-with-resources for automatic cleanup
- Run performance benchmarks before deploying changes

See [performance test README](../pdf-via-chrome/src/test/java/com/fostermoore/pdfviachrome/performance/README.md) for detailed benchmarking instructions.
