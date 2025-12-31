# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**pdf-via-chrome** is a Java library for generating PDFs from HTML content and URLs using headless Chrome/Chromium via the Chrome DevTools Protocol (CDP). It's a multi-module Maven project targeting Java 17+.

## Build Commands

```bash
# Full build with tests
mvn clean install

# Skip tests during build
mvn clean install -DskipTests

# Run unit tests only
mvn test

# Run integration tests
mvn verify

# Run tests in a specific module
mvn test -pl pdf-via-chrome

# Run a single test class
mvn test -Dtest=ChromeManagerTest

# Run a single test method
mvn test -Dtest=ChromeManagerTest#testStartChrome
```

## Module Structure

This is a Maven multi-module project with two modules:

- **pdf-via-chrome**: The main library containing all PDF generation functionality. All core implementation happens here.
- **pdf-via-chrome-test-app**: A Spring Boot application for manual testing and demonstrating the library. Includes REST API endpoints for HTML-to-PDF generation.

## Core Architecture

### Resource Management Pattern

This project uses the **AutoCloseable pattern** extensively to ensure proper cleanup of Chrome processes and WebSocket connections.

**Recommended approach** - Use PdfGenerator (high-level API):

```java
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html).generate();
    // PdfGenerator handles all resource cleanup automatically
}
```

**Low-level approach** - Direct ChromeManager and CdpSession usage:

```java
try (ChromeManager chromeManager = new ChromeManager(options)) {
    ChromeProcess process = chromeManager.start();
    try (CdpSession session = new CdpSession(process.getWebSocketDebuggerUrl())) {
        session.connect();
        // Use session directly
    }
}
```

**Key AutoCloseable classes:**
- `ChromeManager` - Manages Chrome process lifecycle
- `CdpSession` - Manages CDP WebSocket connection
- `PdfGenerator` - Top-level API with lazy initialization and thread-safe operation

### Builder Pattern

The project uses the builder pattern for configuration objects:

- `ChromeOptions.Builder` - Chrome browser configuration (headless, debugging port, flags, timeouts)
- `CdpClient.Builder` - CDP session configuration
- `PdfGenerator.Builder` - PDF generator configuration (Chrome path, timeout, headless mode, Docker options)
- `PdfOptions.Builder` - PDF output configuration (paper size, margins, orientation, scale, headers/footers)
- `PageOptions.Builder` - Page-specific settings (viewport, user agent, JavaScript, device scale factor)

### Package Structure (pdf-via-chrome)

Base package: `com.fostermoore.pdfviachrome`

- **api/**: Public API (IMPLEMENTED)
  - `PdfGenerator` - Main entry point with fluent API (AutoCloseable, thread-safe, lazy initialization)
  - `PdfOptions` - PDF output configuration with builder (paper size, margins, scale, headers/footers)
  - `PageOptions` - Page-specific settings with builder (viewport, user agent, JavaScript enablement)

- **chrome/**: Browser process management (IMPLEMENTED)
  - `ChromeManager` - Launches and manages Chrome process lifecycle (AutoCloseable)
  - `ChromeOptions` - Chrome configuration with builder pattern
  - `ChromeProcess` - Represents a running Chrome process

- **cdp/**: Chrome DevTools Protocol interaction (IMPLEMENTED)
  - `CdpSession` - Manages WebSocket connection to Chrome (AutoCloseable)
  - `CdpClient` - Factory for creating CDP sessions

- **converter/**: Conversion implementations (FULLY IMPLEMENTED)
  - `HtmlToPdfConverter` - Converts HTML strings to PDF with DOMContentLoaded event handling
  - `UrlToPdfConverter` - Converts URLs to PDF with page navigation and load event handling

- **exception/**: Custom exceptions (FULLY IMPLEMENTED)
  - `PdfGenerationException` - Base exception for PDF generation failures
  - `BrowserTimeoutException` - Thrown when browser operations timeout
  - `PageLoadException` - Thrown when page loading fails
  - `ChromeNotFoundException` - Thrown when Chrome executable cannot be found
  - `CdpConnectionException` - Thrown when CDP connection fails

- **util/**: Utilities (IMPLEMENTED)
  - `ChromePathDetector` - Auto-detects Chrome installation on Windows/Linux/macOS
  - `ProcessRegistry` - Global registry for tracking Chrome processes across JVM
  - `ResourceCleanup` - Utilities for resource cleanup with shutdown hooks
  - `UrlValidator` - URL validation for SSRF protection (blocks private IPs, validates protocols)

- **wait/**: Wait strategies for page readiness (FULLY IMPLEMENTED)
  - `WaitStrategy` - Base interface for wait strategies
  - `TimeoutWait` - Wait for a fixed duration
  - `NetworkIdleWait` - Wait until network activity stops
  - `ElementWait` - Wait for specific DOM elements to appear
  - `CustomConditionWait` - Wait for custom JavaScript conditions

### Package Structure (pdf-via-chrome-test-app)

Base package: `com.fostermoore.pdfviachrome.testapp`

- **controller/**: REST controllers (PARTIALLY IMPLEMENTED)
  - `PdfController` - REST endpoint for HTML-to-PDF generation
  - `HealthCheckController` - Health check endpoint
  - `GlobalExceptionHandler` - Global exception handling with appropriate HTTP status codes

- **dto/**: Data Transfer Objects (IMPLEMENTED)
  - `HtmlRequest` - Request DTO for HTML-to-PDF endpoint with validation
  - `PdfOptionsDto` - PDF options mapping for JSON requests

- **config/**: Spring configuration (IMPLEMENTED)
  - `PdfGeneratorConfig` - Bean configuration for PdfGenerator

- **ui/**: Web UI controllers (NOT YET IMPLEMENTED)
  - Planned: Thymeleaf-based UI for manual testing

## Implementation Status

Currently in **Phase 5-6** (Wait Strategies and Test Application). The project has complete core PDF generation functionality, wait strategies for dynamic content, and a Spring Boot test application with REST API endpoints.

### Completed Components

**Core Infrastructure (Phase 2):**
- Chrome path detection for Windows/Linux/macOS (ChromePathDetector)
- ChromeManager with AutoCloseable for browser lifecycle management
- CdpSession with AutoCloseable for CDP connections
- ChromeOptions builder pattern with extensive Chrome flags
- Process tracking and cleanup with shutdown hooks (ProcessRegistry, ResourceCleanup)
- Complete exception hierarchy (5 custom exception types)

**Public API (Phase 3):**
- PdfGenerator - Main fluent API with lazy initialization, thread-safety via ReentrantLock
- PdfOptions.Builder - Complete PDF configuration (paper sizes, margins with units, scale, templates)
- PageOptions.Builder - Page configuration (viewport, user agent, JavaScript, device scale factor)

**Conversion Logic (Phase 4 - Complete):**
- HtmlToPdfConverter - Full HTML to PDF conversion with event-based page loading
- UrlToPdfConverter - Full URL to PDF conversion with navigation and load event handling
- PdfGenerator.fromHtml() - Integrated HTML to PDF generation
- PdfGenerator.fromUrl() - Integrated URL to PDF generation
- PdfGenerator.fromDocument() - DOM Document to PDF generation

**Test Application (Phase 6 - Partial):**
- Spring Boot application structure with health check endpoint
- PdfController - REST endpoint POST /api/pdf/from-html for HTML-to-PDF generation
- PdfOptionsDto - Complete JSON mapping for all PDF options
- HtmlRequest DTO with validation (@NotBlank)
- GlobalExceptionHandler - Comprehensive error handling with appropriate HTTP status codes
- PdfGenerator configured as Spring Bean with automatic resource cleanup
- Unit tests (8 tests, all passing) and integration tests (5 tests) for REST endpoints
- Manual testing documentation with curl and PowerShell examples

**Wait Strategies (Phase 5 - Complete):**
- WaitStrategy interface - Base interface for wait strategies
- TimeoutWait - Fixed duration waiting
- NetworkIdleWait - Network activity monitoring with configurable idle time
- ElementWait - DOM element presence detection
- CustomConditionWait - JavaScript condition evaluation
- Comprehensive unit and integration tests for all wait strategies

**Security Review (Phase 7 - Complete):**
- OWASP Dependency Check integration - Scans for vulnerabilities on mvn verify
- UrlValidator - Comprehensive SSRF protection utility with builder pattern
- URL validation in UrlToPdfConverter - Automatic SSRF attack prevention
- Private IP blocking (10.x.x.x, 192.168.x.x, 172.16-31.x.x)
- Localhost and loopback address blocking
- Link-local address blocking (169.254.x.x, fe80::/10)
- Protocol validation (HTTP, HTTPS, data: only)
- Domain whitelist/blacklist support
- DNS resolution for IP validation
- Data URL safe handling (no network requests)
- SECURITY.md - Comprehensive security documentation
- Zero critical or high severity vulnerabilities
- 33 security validation tests (all passing)

### Not Yet Implemented

- **Test application UI**: Thymeleaf-based web UI for manual testing (REST API endpoints completed)
- **Test application URL endpoint**: POST /api/pdf/from-url endpoint (moved to Version 2, Phase 8)
- **Advanced features**: Browser pooling, request interception, custom headers, authentication

### Working Features

The library can currently:
1. Auto-detect Chrome installation or use custom path
2. Launch and manage Chrome processes with proper cleanup
3. Generate PDFs from HTML strings with full customization
4. Generate PDFs from URLs with full navigation and load handling
5. Generate PDFs from DOM Documents (org.w3c.dom.Document)
6. Configure paper size (Letter, A4, Legal, etc.) and custom dimensions
7. Set margins with multiple units (inches, cm, px)
8. Control orientation (portrait/landscape)
9. Enable/disable background graphics
10. Use wait strategies for dynamic content:
   - Wait for fixed duration (TimeoutWait)
   - Wait for network idle (NetworkIdleWait)
   - Wait for DOM elements to appear (ElementWait)
   - Wait for custom JavaScript conditions (CustomConditionWait)
11. Set custom headers and footers:
   - Convenience methods: simplePageNumbers(), headerWithTitle(), footerWithDate(), standardHeaderFooter()
   - Custom HTML templates with CDP variables (pageNumber, totalPages, date, title, url)
12. Inject custom CSS for print-specific styling:
   - withCustomCss(String css) for inline CSS injection
   - withCustomCssFromFile(Path cssFile) for external CSS files
   - Works with both HTML and URL sources
   - Applied after page load, before PDF generation
13. Select specific pages for PDF output using page ranges:
   - Supports single pages: "1", "5", "10"
   - Supports ranges: "1-5", "10-20"
   - Supports mixed format: "1-5, 8, 11-13, 20"
   - Validates page range format and ensures page numbers start from 1
   - Empty string generates all pages
14. Execute custom JavaScript before PDF generation:
   - executeJavaScript(String jsCode) for inline JavaScript execution
   - executeJavaScriptFromFile(Path jsFile) for external JavaScript files
   - Works with both HTML and URL sources
   - Executes after page load and CSS injection, before PDF generation
   - Supports both synchronous and asynchronous (Promise-based) JavaScript
   - Use cases: Remove elements, trigger rendering, modify content, wait for dynamic content
15. Handle thread-safe concurrent PDF generation
16. Security features to prevent attacks and vulnerabilities:
   - Automatic URL validation to prevent SSRF (Server-Side Request Forgery) attacks
   - Private IP address blocking (10.x.x.x, 192.168.x.x, 172.16-31.x.x)
   - Localhost and loopback address blocking
   - Protocol validation (only HTTP, HTTPS, and data: URLs allowed)
   - Domain whitelist/blacklist support for fine-grained control
   - DNS resolution to detect IP-based SSRF attempts
   - OWASP Dependency Check integration for vulnerability scanning
   - Comprehensive security documentation in SECURITY.md

The test application provides:
1. REST API endpoint: POST /api/pdf/from-html - Generate PDFs from HTML via HTTP
2. Complete PDF options support via JSON (all PdfOptions fields)
3. Request validation and comprehensive error handling
4. Appropriate HTTP status codes for different error types
5. Health check endpoint for application monitoring
6. Manual testing examples (curl, PowerShell) in MANUAL_TESTING.md

Refer to PROJECT_SPEC.xml for the complete implementation roadmap (7 phases total).

## Testing Conventions

- **Unit tests**: `*Test.java` - Use Mockito for mocking, run with `mvn test`
- **Integration tests**: `*IT.java` or `*IntegrationTest.java` - Run with `mvn verify`
- **Assertions**: Use AssertJ for fluent assertions
- **Coverage target**: 80% line coverage (JaCoCo)

### Integration Test Suites

The project includes comprehensive integration test suites that validate end-to-end functionality:

**AdvancedFeaturesIT** - Comprehensive tests for advanced PDF generation features:
- Individual feature tests: CSS injection, JavaScript execution, page ranges, headers/footers
- Combined feature tests: CSS+JS, headers+ranges, CSS+JS+ranges, all features together
- Edge case tests: JS modifying DOM before CSS, complex header templates, file loading, mixed page ranges
- Total: 14 tests validating feature combinations and interactions
- Uses test resources: `multi-page.html`, `print-styles.css`, `page-manipulation.js`
- Validates PDFs using Apache PDFBox (page count, text extraction, dimensions)

**UrlToPdfConverterIT** - Comprehensive tests for URL-to-PDF conversion:
- URL navigation tests: Simple URLs, HTTP vs HTTPS, query parameters, fragments
- Custom PDF options: Landscape, A4, margins, scale, print background
- Header and footer templates with URLs
- Error handling: Invalid domains, timeouts, disconnected sessions
- Edge cases: 404 pages, multiple conversions in same session
- Total: 13 tests validating URL navigation, PDF generation, and error scenarios
- Disabled by default (requires Chrome and internet connectivity)
- Enable with: `mvn verify -DCHROME_INTEGRATION_TESTS=true`
- Uses @EnabledIfEnvironmentVariable to skip tests when not explicitly enabled

**Other Integration Test Suites:**
- `PdfGenerationIT` - End-to-end PDF generation with PdfGenerator API
- `HtmlToPdfIT` - HTML to PDF conversion with Testcontainers
- `HeaderFooterIT` - Header and footer functionality
- `ChromeManagerIT` - Chrome process management
- `HtmlToPdfConverterIT` - HTML converter integration tests
- `NetworkIdleWaitIT` - Network idle wait strategy
- `WaitStrategyIT` - Wait strategy integration tests
- `PdfGeneratorIT` - PdfGenerator API integration tests
- `ProcessRegistryIT` - Process registry integration tests
- And more (12 integration test suites total)

**Running Integration Tests:**
```bash
# Run all integration tests
mvn verify

# Run specific integration test
mvn verify -Dit.test=AdvancedFeaturesIT

# Run URL-to-PDF integration tests (requires Chrome and internet)
mvn verify -Dit.test=UrlToPdfConverterIT -DCHROME_INTEGRATION_TESTS=true

# Most integration tests require Docker (Testcontainers)
# Tests are automatically skipped if Docker is not available
# UrlToPdfConverterIT requires Chrome installed and internet connectivity
```

## Code Coverage Reporting

The project uses JaCoCo for code coverage analysis with an enforced 80% line coverage threshold.

**Running Coverage Reports:**
```bash
# Generate coverage reports (automatically runs with mvn verify)
mvn verify

# Generate coverage reports without integration tests
mvn test jacoco:report
```

**Viewing Coverage Reports:**
- **HTML Report**: Open `target/site/jacoco/index.html` in your browser
  - Located in each module directory (e.g., `pdf-via-chrome/target/site/jacoco/index.html`)
  - Provides interactive drill-down into packages, classes, and methods
  - Shows line, branch, and method coverage metrics
- **XML Report**: `target/site/jacoco/jacoco.xml` (for CI/CD integration)
- **CSV Report**: `target/site/jacoco/jacoco.csv` (for spreadsheet analysis)

**Coverage Configuration:**
- **Threshold**: 80% line coverage, 75% branch coverage (enforced on build)
- **Exclusions**: DTOs, test application classes, and exception classes
- **Merged Reports**: Combines unit test and integration test coverage
- **Build Failure**: Build fails if coverage drops below threshold

**Coverage Files:**
- `jacoco.exec` - Unit test coverage data
- `jacoco-it.exec` - Integration test coverage data
- `jacoco-merged.exec` - Combined coverage data

All `.exec` files are excluded from version control via `.gitignore`.

## Performance Testing and Benchmarking

The project includes comprehensive performance benchmarks using JMH (Java Microbenchmark Harness) and memory profiling tools.

### Performance Test Suite

Located in `pdf-via-chrome/src/test/java/com/fostermoore/pdfviachrome/performance/`:

**PerformanceBenchmark** - JMH benchmarks for PDF generation:
- Simple HTML conversion (baseline)
- Complex HTML with CSS and multiple pages (10 pages)
- Large document conversion (120+ pages)
- Chrome startup time measurement
- Generator instance reuse vs new instances
- Sequential conversions (throughput testing)
- Concurrent generation (4 workers)
- Header/footer processing overhead
- CSS injection performance
- JavaScript execution performance
- Custom PDF options overhead

**MemoryProfiler** - Memory usage analysis:
- Single generation memory footprint
- Sequential generation leak detection (50 iterations)
- Generator reuse vs new instances comparison
- Large document memory requirements
- Concurrent load memory profiling (4 workers)

**BenchmarkRunner** - Convenient benchmark execution:
- Run all benchmarks with default settings
- Quick benchmarks (subset for fast feedback)
- Profiled benchmarks (with GC analysis)

### Running Performance Tests

**Quick benchmarks** (3 core scenarios):
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner \
    -Dexec.args="quick"
```

**Full benchmark suite** (all 11 scenarios):
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner
```

**With GC profiling**:
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner \
    -Dexec.args="profile"
```

**Memory profiling**:
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.MemoryProfiler
```

**Direct JMH execution**:
```bash
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=org.openjdk.jmh.Main \
    -Dexec.args="PerformanceBenchmark -f 1 -wi 3 -i 5"
```

### Key Performance Metrics

Based on typical hardware (4-core CPU, 8GB RAM):

| Scenario | Time | Memory | Notes |
|----------|------|--------|-------|
| Simple HTML (new instance) | 1.5-2.5s | ~200 MB | Includes Chrome startup |
| Simple HTML (reused) | 300-500ms | ~200 MB | 3-5x faster |
| Complex HTML (10 pages) | 2-4s | ~250 MB | Styled content |
| Large document (120 pages) | 8-15s | ~400 MB | Linear scaling |
| Chrome startup only | 1-2s | ~150 MB | Platform dependent |
| Concurrent (4 workers) | 2-3s total | ~600 MB | 3-4x throughput |

**Optimization recommendations**:
1. **Reuse PdfGenerator instances** - Eliminates startup overhead (3-5x improvement)
2. **Use concurrent processing** - 2-4 workers per CPU core for batch operations
3. **Allocate adequate memory** - ~200 MB per concurrent worker
4. **Simplify HTML/CSS** - 20-50% improvement for simple documents
5. **Monitor latency** - Track p95/p99 metrics in production

### Performance Documentation

See [docs/PERFORMANCE.md](../docs/PERFORMANCE.md) for comprehensive performance characteristics, including:
- Detailed benchmark results with percentile breakdowns
- Resource requirements (memory, CPU, disk)
- Performance factors and optimization strategies
- Concurrent processing guidelines
- Memory management best practices
- Performance troubleshooting guide

### Test Resources

Performance test HTML files in `src/test/resources/performance-test/`:
- `simple.html` - Single page, minimal content (baseline)
- `complex.html` - 10 pages with CSS, tables, styled content
- `large.html` - 120 pages generated programmatically (stress test)

## Key Technologies

- **Chrome DevTools Protocol**: Using `cdt-java-client` (io.fluidsonic.mirror:cdt-java-client:4.0.0-fluidsonic-1)
  - Note: Uses the fluidsonic fork instead of the original com.github.kklisura.cdt library
  - The fork fixes a bug where `createTab()` incorrectly uses HTTP GET instead of PUT for the /json/new endpoint
  - Chrome requires PUT for tab creation, the original library would fail with HTTP 405 Method Not Allowed
- **Logging**: SLF4J API (implementation chosen by library users)
- **Testing**: JUnit 5, Mockito, AssertJ, Testcontainers (for integration tests with containerized Chrome)
- **Performance Testing**: JMH (Java Microbenchmark Harness) for accurate performance benchmarking

## Chrome Process Management

The ChromeManager handles Chrome with these important considerations:

1. **Auto-detection**: If no Chrome path is specified, ChromePathDetector will search standard locations
2. **User data directory**: Creates temporary directory by default, cleaned up on close
3. **Stability flags**: Automatically adds ~30 Chrome flags for stability (disable extensions, background tasks, etc.)
4. **Chrome 98+ compatibility**: Includes `--remote-allow-origins=*` flag required for Chrome 98+ to allow WebSocket connections to CDP
   - Without this flag, Chrome will reject CDP WebSocket connections with HTTP 403 Forbidden
   - This flag is automatically added by ChromeManager for all Chrome instances
5. **Docker support**: Options for `--no-sandbox` and `--disable-dev-shm-usage` for container environments
6. **Graceful shutdown**: Attempts destroy() first, then destroyForcibly() after timeout
7. **WebSocket URL extraction**: Parses Chrome stdout to find debugging WebSocket URL

## CDP Session Management

The CdpSession provides access to Chrome DevTools Protocol domains:

- `getPage()` - Navigation and PDF generation
- `getRuntime()` - JavaScript execution
- `getNetwork()` - Network monitoring
- `getEmulation()` - Device emulation
- `getDOM()` - DOM manipulation
- `getPerformance()` - Performance monitoring
- `getSecurity()` - Security features

## API Usage Examples

### Basic PDF Generation

```java
// Generate PDF from HTML with default options
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml("<html><body><h1>Hello World</h1></body></html>")
        .generate();
    Files.write(Path.of("output.pdf"), pdf);
}
```

### PDF from URL

```java
// Generate PDF from a URL
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromUrl("https://example.com")
        .generate();
    Files.write(Path.of("webpage.pdf"), pdf);
}
```

### PDF from DOM Document

```java
// Generate PDF from an org.w3c.dom.Document
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

// Create a DOM Document programmatically
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();
Document document = builder.newDocument();

// Build HTML structure
Element html = document.createElement("html");
Element body = document.createElement("body");
Element h1 = document.createElement("h1");
h1.setTextContent("Hello from DOM Document!");

body.appendChild(h1);
html.appendChild(body);
document.appendChild(html);

// Generate PDF
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromDocument(document)
        .generate();
    Files.write(Path.of("document.pdf"), pdf);
}

// Or parse an existing HTML file
Document parsedDoc = builder.parse(new File("input.html"));
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromDocument(parsedDoc)
        .generate();
    Files.write(Path.of("parsed.pdf"), pdf);
}
```

### Custom PDF Options

```java
// Generate PDF with custom options
PdfOptions options = PdfOptions.builder()
    .paperSize(PaperFormat.A4)
    .landscape(true)
    .printBackground(true)
    .margins("1cm")
    .scale(0.8)
    .build();

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(htmlContent)
        .withOptions(options)
        .generate();
    Files.write(Path.of("custom.pdf"), pdf);
}
```

### Headers and Footers

```java
// Simple page numbers in footer
PdfOptions options = PdfOptions.builder()
    .simplePageNumbers()  // "Page X of Y"
    .build();

// Document title in header
PdfOptions options = PdfOptions.builder()
    .headerWithTitle()
    .build();

// Date in footer
PdfOptions options = PdfOptions.builder()
    .footerWithDate()
    .build();

// Standard header and footer (title + page numbers)
PdfOptions options = PdfOptions.builder()
    .standardHeaderFooter()
    .build();

// Custom header/footer templates
PdfOptions options = PdfOptions.builder()
    .displayHeaderFooter(true)
    .headerTemplate("<div style=\"font-size: 10px; text-align: center; width: 100%;\"><span class=\"title\"></span></div>")
    .footerTemplate("<div style=\"font-size: 10px; text-align: center; width: 100%;\">Page <span class=\"pageNumber\"></span> of <span class=\"totalPages\"></span></div>")
    .build();

// Supported template variables:
// - <span class="pageNumber"></span> - current page number
// - <span class="totalPages"></span> - total pages
// - <span class="date"></span> - formatted date
// - <span class="title"></span> - document title
// - <span class="url"></span> - document URL
```

### CSS Injection

```java
// Inject custom CSS to override page styles for printing
String customCss = """
    @media print {
        .no-print { display: none !important; }
        .page-break { page-break-after: always; }
    }
    body {
        font-size: 12pt;
        line-height: 1.5;
    }
    """;

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(htmlContent)
        .withCustomCss(customCss)
        .generate();
    Files.write(Path.of("styled.pdf"), pdf);
}

// Load CSS from a file
Path cssFile = Path.of("styles/print.css");
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(htmlContent)
        .withCustomCssFromFile(cssFile)
        .generate();
    Files.write(Path.of("styled.pdf"), pdf);
}

// Combine CSS injection with PDF options
PdfOptions options = PdfOptions.builder()
    .paperSize(PaperFormat.A4)
    .printBackground(true)
    .build();

String css = ".header { display: none; } .footer { display: none; }";

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(htmlContent)
        .withCustomCss(css)
        .withOptions(options)
        .generate();
    Files.write(Path.of("clean.pdf"), pdf);
}

// Works with URLs too
String printCss = """
    nav, aside, .sidebar {
        display: none !important;
    }
    article {
        width: 100% !important;
        max-width: none !important;
    }
    """;

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromUrl("https://example.com")
        .withCustomCss(printCss)
        .generate();
    Files.write(Path.of("webpage.pdf"), pdf);
}
```

### Page Ranges

```java
// Generate multi-page HTML document
String multiPageHtml = """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            @media print {
                .page-break { page-break-after: always; }
            }
        </style>
    </head>
    <body>
        <div class="page-break"><h1>Page 1</h1><p>Content for page 1...</p></div>
        <div class="page-break"><h1>Page 2</h1><p>Content for page 2...</p></div>
        <div class="page-break"><h1>Page 3</h1><p>Content for page 3...</p></div>
        <div class="page-break"><h1>Page 4</h1><p>Content for page 4...</p></div>
        <div><h1>Page 5</h1><p>Content for page 5...</p></div>
    </body>
    </html>
    """;

try (PdfGenerator generator = PdfGenerator.create().build()) {
    // Extract only pages 1-3
    PdfOptions rangeOptions = PdfOptions.builder()
        .pageRanges("1-3")
        .build();

    byte[] pages1to3 = generator.fromHtml(multiPageHtml)
        .withOptions(rangeOptions)
        .generate();
    Files.write(Path.of("pages-1-3.pdf"), pages1to3);

    // Extract specific pages (1, 3, 5)
    PdfOptions specificPages = PdfOptions.builder()
        .pageRanges("1,3,5")
        .build();

    byte[] oddPages = generator.fromHtml(multiPageHtml)
        .withOptions(specificPages)
        .generate();
    Files.write(Path.of("odd-pages.pdf"), oddPages);

    // Mixed ranges: pages 1-2 and page 4
    PdfOptions mixedRanges = PdfOptions.builder()
        .pageRanges("1-2,4")
        .build();

    byte[] selectedPages = generator.fromHtml(multiPageHtml)
        .withOptions(mixedRanges)
        .generate();
    Files.write(Path.of("selected-pages.pdf"), selectedPages);
}

// Page range format examples:
// - Single page: "1" or "5" or "10"
// - Range: "1-5" or "10-20"
// - Multiple pages: "1,3,5,7"
// - Mixed: "1-5, 8, 11-13, 20"
// - All pages: "" (empty string)
```

### JavaScript Execution

```java
// Execute JavaScript to modify the page before PDF generation
String html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>My Document</title>
    </head>
    <body>
        <h1 id="title">Original Title</h1>
        <div class="ads">Advertisement</div>
        <p class="content">Important content here</p>
        <div class="ads">Another Ad</div>
    </body>
    </html>
    """;

String jsCode = """
    // Remove all ads
    document.querySelectorAll('.ads').forEach(ad => ad.remove());

    // Modify the title
    document.getElementById('title').textContent = 'PDF Export';
    document.title = 'Exported PDF';
    """;

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html)
        .executeJavaScript(jsCode)
        .generate();
    Files.write(Path.of("output.pdf"), pdf);
}

// Execute JavaScript from a file
Path jsFile = Path.of("scripts/prepare-pdf.js");
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html)
        .executeJavaScriptFromFile(jsFile)
        .generate();
    Files.write(Path.of("output.pdf"), pdf);
}

// Asynchronous JavaScript with Promises
String asyncJsCode = """
    // Wait for some async operation
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Trigger dynamic rendering
    await window.renderChart();

    // Wait for elements to appear
    while (!document.querySelector('.rendered-content')) {
        await new Promise(resolve => setTimeout(resolve, 100));
    }
    """;

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html)
        .executeJavaScript(asyncJsCode)
        .generate();
    Files.write(Path.of("output.pdf"), pdf);
}

// Combine with CSS injection and PDF options
String css = ".sidebar { display: none; }";
String js = "document.querySelector('.main-content').style.width = '100%';";

PdfOptions options = PdfOptions.builder()
    .paperSize(PaperFormat.A4)
    .printBackground(true)
    .build();

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html)
        .withCustomCss(css)
        .executeJavaScript(js)
        .withOptions(options)
        .generate();
    Files.write(Path.of("custom.pdf"), pdf);
}
```

### Custom Chrome Configuration

```java
// Configure Chrome settings
PdfGenerator generator = PdfGenerator.create()
    .withChromePath(Path.of("/path/to/chrome"))
    .withTimeout(Duration.ofSeconds(60))
    .withHeadless(true)
    .withNoSandbox(true)  // For Docker environments
    .withDisableDevShmUsage(true)  // For limited /dev/shm
    .build();

try (generator) {
    byte[] pdf = generator.fromHtml(html).generate();
    Files.write(Path.of("output.pdf"), pdf);
}
```

### Wait Strategies for Dynamic Content

```java
import com.fostermoore.pdfviachrome.wait.*;

// Wait for a fixed duration before generating PDF
WaitStrategy timeout = WaitStrategy.timeout(Duration.ofSeconds(5));
// Use with low-level API - wait strategies are typically used at the converter level

// Wait for network to become idle (no activity for specified duration)
WaitStrategy networkIdle = WaitStrategy.networkIdle(Duration.ofMillis(500));
// Useful for single-page applications and dynamic content

// Wait for a specific DOM element to appear
WaitStrategy elementWait = WaitStrategy.elementPresent("#content-loaded");
// Waits until the element with id="content-loaded" appears in the DOM

// Wait for custom JavaScript condition to be true
WaitStrategy customWait = WaitStrategy.customCondition("window.myApp && window.myApp.ready === true");
// Evaluates JavaScript expression repeatedly until it returns true

// Using wait strategies with converters (low-level API)
ChromeOptions chromeOptions = ChromeOptions.builder().build();
try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
    ChromeProcess process = chromeManager.start();

    try (CdpSession session = new CdpSession(process.getWebSocketDebuggerUrl())) {
        session.connect();

        // Create converter with wait strategy
        HtmlToPdfConverter converter = new HtmlToPdfConverter(session);

        // Wait strategy can be applied before conversion
        WaitStrategy networkIdle = WaitStrategy.networkIdle();
        networkIdle.await(session.getService(), Duration.ofSeconds(30));

        byte[] pdf = converter.convert(htmlContent, PdfOptions.defaults());
        Files.write(Path.of("output.pdf"), pdf);
    }
}

// Common use cases:
// 1. Single-page applications with lazy loading
WaitStrategy spaWait = WaitStrategy.networkIdle(Duration.ofSeconds(2));

// 2. Pages with specific loading indicators
WaitStrategy loaderWait = WaitStrategy.customCondition(
    "document.querySelector('.loading-spinner') === null"
);

// 3. Content that depends on specific elements
WaitStrategy contentReady = WaitStrategy.elementPresent(".main-content");

// 4. Multiple conditions (chain wait strategies)
// First wait for element, then wait for network idle
WaitStrategy elementFirst = WaitStrategy.elementPresent("#data-loaded");
WaitStrategy thenNetworkIdle = WaitStrategy.networkIdle();
// Apply both sequentially in your conversion logic
```

### Multiple PDFs with Single Generator

```java
// Reuse generator for multiple PDFs (thread-safe)
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf1 = generator.fromHtml(html1).generate();
    byte[] pdf2 = generator.fromUrl("https://example.com").generate();
    byte[] pdf3 = generator.fromDocument(domDocument).generate();
    byte[] pdf4 = generator.fromHtml(html2)
        .withOptions(PdfOptions.builder().landscape(true).build())
        .generate();

    Files.write(Path.of("doc1.pdf"), pdf1);
    Files.write(Path.of("doc2.pdf"), pdf2);
    Files.write(Path.of("doc3.pdf"), pdf3);
    Files.write(Path.of("doc4.pdf"), pdf4);
}
```

### Direct Converter Usage (Low-Level API)

```java
// Use HtmlToPdfConverter directly for more control
ChromeOptions chromeOptions = ChromeOptions.builder().build();
try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
    ChromeProcess process = chromeManager.start();

    try (CdpSession session = new CdpSession(process.getWebSocketDebuggerUrl())) {
        session.connect();

        HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
        PdfOptions options = PdfOptions.builder()
            .paperSize(PaperFormat.LETTER)
            .printBackground(true)
            .build();

        byte[] pdf = converter.convert(htmlContent, options);
        Files.write(Path.of("output.pdf"), pdf);
    }
}
```

## Test Application REST API

The test application provides a REST endpoint for testing the library via HTTP requests.

### Starting the Test Application

```bash
# Run from project root
mvn spring-boot:run -pl pdf-via-chrome-test-app

# Or build and run the JAR
mvn clean package -pl pdf-via-chrome-test-app
java -jar pdf-via-chrome-test-app/target/pdf-via-chrome-test-app-1.0.0-SNAPSHOT.jar
```

Application runs on `http://localhost:8080`

### Generate PDF from HTML

```bash
# Basic request
curl -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<html><body><h1>Hello World</h1></body></html>"
  }' \
  --output test.pdf

# With custom options
curl -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<html><body><h1>Custom PDF</h1></body></html>",
    "options": {
      "paperFormat": "A4",
      "landscape": true,
      "printBackground": true,
      "margins": "1cm"
    }
  }' \
  --output custom.pdf
```

### PowerShell Example

```powershell
$body = @{
    content = "<html><body><h1>Hello World</h1></body></html>"
    options = @{
        paperFormat = "A4"
        printBackground = $true
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/pdf/from-html" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -OutFile "output.pdf"
```

### Available Options

All PdfOptions fields are supported via JSON:
- `paperFormat`: "LETTER", "A4", "LEGAL", "A3", etc.
- `landscape`: true/false
- `printBackground`: true/false
- `scale`: 0.1 to 2.0
- `margins`: "1cm" or individual margins (marginTop, marginBottom, marginLeft, marginRight)
- `displayHeaderFooter`: true/false
- `headerTemplate`, `footerTemplate`: HTML strings
- `pageRanges`: "1-5, 8, 11-13"
- `preferCssPageSize`: true/false

### Error Responses

The API returns appropriate HTTP status codes:
- `200 OK` - PDF generated successfully
- `400 Bad Request` - Validation errors or invalid options
- `422 Unprocessable Entity` - Page load failures
- `500 Internal Server Error` - PDF generation failures
- `503 Service Unavailable` - Chrome not found or CDP connection issues
- `504 Gateway Timeout` - Browser operation timeout

See `pdf-via-chrome-test-app/MANUAL_TESTING.md` for more examples.

## Development Notes

- **Thread safety**: PdfGenerator is thread-safe via ReentrantLock; a single instance can generate multiple PDFs concurrently
- **Lazy initialization**: PdfGenerator only starts Chrome on first generate() call to minimize resource usage
- **Resource cleanup**: Always use try-with-resources to ensure Chrome processes are cleaned up and prevent zombie instances
- **Platform differences**: Test on Windows/Linux/macOS as Chrome paths and behavior differ
- **Memory**: Chrome can be memory-intensive; consider memory requirements in design (each Chrome instance ~100-200MB)
- **Logging levels**: Use TRACE for CDP protocol events, DEBUG for internal operations, INFO for major lifecycle events
- **Docker environments**: Use `withNoSandbox(true)` and `withDisableDevShmUsage(true)` when running in containers
- **Security**: URL validation is automatically applied in UrlToPdfConverter to prevent SSRF attacks; see SECURITY.md for comprehensive security guidance
- **Vulnerability scanning**: Run `mvn verify` to execute OWASP Dependency Check; build fails on critical/high vulnerabilities (CVSS ≥ 7)
