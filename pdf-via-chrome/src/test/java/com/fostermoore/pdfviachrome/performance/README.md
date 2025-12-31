# Performance Testing

This package contains performance benchmarks and profiling tools for the pdf-via-chrome library.

## Components

### PerformanceBenchmark
JMH-based benchmarks for measuring PDF generation performance across various scenarios:
- Simple HTML conversion (baseline)
- Complex HTML with CSS and multiple pages
- Large documents (100+ pages)
- Generator instance reuse
- Chrome startup time
- Sequential and concurrent conversions
- Header/footer processing
- CSS injection
- JavaScript execution

### MemoryProfiler
Memory usage analysis tool that measures:
- Single generation memory footprint
- Sequential generation memory growth (leak detection)
- Generator reuse vs new instances
- Large document memory requirements
- Concurrent load memory usage

### BenchmarkRunner
Convenient runner for executing JMH benchmarks with pre-configured settings.

## Running Benchmarks

### Option 1: Using BenchmarkRunner (Recommended)

Run all benchmarks:
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner
```

Quick benchmarks (subset for faster feedback):
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner \
    -Dexec.args="quick"
```

With GC profiling:
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner \
    -Dexec.args="profile"
```

### Option 2: Using JMH Main Directly

```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=org.openjdk.jmh.Main \
    -Dexec.args="PerformanceBenchmark -f 1 -wi 3 -i 5"
```

JMH options:
- `-f 1` : Number of forks (separate JVM processes)
- `-wi 3` : Number of warmup iterations
- `-i 5` : Number of measurement iterations
- `-r 1` : Time per iteration (seconds)

Run specific benchmark:
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=org.openjdk.jmh.Main \
    -Dexec.args="PerformanceBenchmark.simpleHtmlConversion"
```

### Option 3: From IDE

Simply run the `main()` method in `BenchmarkRunner` or `PerformanceBenchmark` from your IDE.

## Running Memory Profiler

```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.MemoryProfiler
```

Or run the `main()` method from your IDE.

## Understanding Results

### JMH Benchmark Output

JMH reports average time per operation in milliseconds (by default):

```
Benchmark                                    Mode  Cnt    Score    Error  Units
PerformanceBenchmark.simpleHtmlConversion    avgt    5  1250.123 ± 45.678  ms/op
PerformanceBenchmark.generatorReuse          avgt    5   250.456 ± 12.345  ms/op
```

- **Mode**: `avgt` = average time per operation
- **Cnt**: Number of measurement iterations
- **Score**: Average execution time
- **Error**: Confidence interval (±)
- **Units**: Measurement unit (ms/op = milliseconds per operation)

### Key Metrics to Monitor

1. **Baseline Performance** (`simpleHtmlConversion`)
   - Target: < 2000 ms
   - Represents fastest possible conversion

2. **Chrome Startup** (`chromeStartup`)
   - Target: < 1500 ms
   - Dominates short operations

3. **Generator Reuse** (`generatorReuse`)
   - Should be 3-5x faster than new instances
   - Eliminates Chrome startup overhead

4. **Memory Usage** (from MemoryProfiler)
   - Single instance: ~200 MB
   - Should not grow significantly with sequential generations
   - Memory should be released after cleanup

### Performance Regression Detection

Compare benchmark results over time:

```bash
# Run baseline
java -jar target/benchmarks.jar -rf json -rff baseline.json

# After changes
java -jar target/benchmarks.jar -rf json -rff current.json

# Compare (use external tools or scripts)
```

## Test Resources

Test HTML files are located in `src/test/resources/performance-test/`:
- `simple.html` - Single page with minimal content
- `complex.html` - 10 pages with CSS, tables, and styled content
- `large.html` - 120 pages for stress testing

## Tips for Accurate Benchmarking

1. **Close background applications** to reduce CPU/memory interference
2. **Use consistent hardware** for comparable results
3. **Run multiple iterations** to account for JVM warmup
4. **Monitor system resources** during benchmarks
5. **Compare relative performance** rather than absolute numbers
6. **Ensure Chrome is installed** and accessible

## Expected Performance Characteristics

Based on typical hardware (4-core CPU, 8GB RAM):

| Scenario | Time | Memory | Notes |
|----------|------|--------|-------|
| Simple HTML (new instance) | 1.5-2.5s | ~200 MB | Includes Chrome startup |
| Simple HTML (reuse) | 300-500ms | ~200 MB | No startup overhead |
| Complex HTML (10 pages) | 2-4s | ~250 MB | Styling overhead |
| Large document (120 pages) | 8-15s | ~400 MB | Scales linearly |
| Chrome startup | 1-2s | ~150 MB | Platform dependent |
| Concurrent (4 workers) | 2-3s total | ~600 MB | 3-4x throughput |

These are approximate values and will vary based on:
- Hardware specifications
- Chrome version
- Operating system
- JVM settings
- System load

## Optimization Recommendations

Based on benchmark results:

1. **Reuse PdfGenerator instances** - Eliminates Chrome startup (3-5x improvement)
2. **Parallel processing** - Use thread pools for batch operations
3. **Memory tuning** - Set appropriate heap size: `-Xmx1g` for 4 concurrent workers
4. **Chrome pooling** - Consider browser pools for very high throughput (future enhancement)

## CI/CD Integration

Run benchmarks in CI to detect performance regressions:

```yaml
# GitHub Actions example
- name: Run Performance Benchmarks
  run: |
    mvn test-compile
    mvn exec:java -Dexec.classpathScope=test \
      -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner \
      -Dexec.args="quick"
```

Store results as artifacts for comparison across builds.
