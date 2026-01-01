# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**pdf-via-chrome** is a Java library for generating PDFs from HTML content and URLs using headless Chrome/Chromium via the Chrome DevTools Protocol (CDP). It's a multi-module Maven project targeting Java 17+.

**Current Status**: Phase 7 (Documentation and Polish) - Core functionality complete. See PROJECT_SPEC.xml for detailed roadmap.

## Build Commands

```bash
# Full build with tests
mvn clean install

# Run unit tests only
mvn test

# Run integration tests (includes code coverage)
mvn verify

# Run URL integration tests (requires Chrome and internet)
mvn verify -DCHROME_INTEGRATION_TESTS=true

# Run performance benchmarks
mvn test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner
```

## Module Structure

- **pdf-via-chrome**: Core library with all PDF generation functionality
- **pdf-via-chrome-test-app**: Spring Boot test application with REST API and web UI

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

### Package Structure

**pdf-via-chrome** (`com.fostermoore.pdfviachrome`):
- **api/**: Public API - `PdfGenerator`, `PdfOptions`, `PageOptions` (all with builders)
- **chrome/**: Browser management - `ChromeManager`, `ChromeOptions`, `ChromeProcess`
- **cdp/**: CDP protocol - `CdpSession`, `CdpClient`
- **converter/**: Conversion logic - `HtmlToPdfConverter`, `UrlToPdfConverter`
- **exception/**: 5 custom exception types for specific failure scenarios
- **util/**: `ChromePathDetector`, `ProcessRegistry`, `ResourceCleanup`, `UrlValidator`
- **wait/**: Wait strategies for dynamic content - `WaitStrategy`, `NetworkIdleWait`, `ElementWait`, `TimeoutWait`, `CustomConditionWait`

**pdf-via-chrome-test-app** (`com.fostermoore.pdfviachrome.testapp`):
- **controller/**: REST endpoints and error handling
- **dto/**: Request/response objects with validation
- **config/**: Spring configuration and beans

## Current Implementation Status

**Phase 7 (Documentation and Polish)** - Core functionality complete. All major features implemented:
- ✅ HTML-to-PDF and URL-to-PDF conversion
- ✅ Custom headers/footers, CSS injection, JavaScript execution, page ranges
- ✅ Wait strategies for dynamic content
- ✅ Spring Boot test application with REST API and web UI
- ✅ SSRF protection and security scanning (zero high/critical vulnerabilities)
- ✅ Performance benchmarking and optimization guidance
- ✅ 80% code coverage with JaCoCo

**See PROJECT_SPEC.xml for detailed roadmap and implementation status.**

## Testing Conventions

- **Unit tests**: `*Test.java` - Mockito for mocking, AssertJ for assertions
- **Integration tests**: `*IT.java` - Run with `mvn verify`, uses Testcontainers
- **Coverage**: 80% line coverage enforced (JaCoCo), view reports at `target/site/jacoco/index.html`

**Key test suites**:
- `AdvancedFeaturesIT` - 14 tests for CSS, JS, headers/footers, page ranges
- `UrlToPdfConverterIT` - 13 tests (disabled by default, enable with `-DCHROME_INTEGRATION_TESTS=true`)
- 33 security tests in `UrlValidatorTest` for SSRF protection

**Performance benchmarks**: See `docs/PERFORMANCE.md` for full details
- Run benchmarks: `mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.fostermoore.pdfviachrome.performance.BenchmarkRunner`
- Key metric: Reuse PdfGenerator instances for 3-5x performance improvement

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

## Quick API Reference

**See README.md for comprehensive usage examples.** Basic patterns:

```java
// Basic HTML to PDF
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml("<html>...</html>").generate();
}

// With options
PdfOptions options = PdfOptions.builder()
    .paperSize(PaperFormat.A4)
    .landscape(true)
    .standardHeaderFooter()  // Convenience method
    .margins("1cm")
    .build();

// Advanced features
generator.fromHtml(html)
    .withCustomCss(css)           // Inject CSS
    .executeJavaScript(js)        // Run JS before PDF
    .withOptions(options)
    .generate();
```

**Key features**: Headers/footers, CSS injection, JS execution, page ranges, wait strategies, DOM Document input.
**See README.md for detailed examples of all features.**

## Test Application

**REST API**: `mvn spring-boot:run -pl pdf-via-chrome-test-app` → http://localhost:8080
- **POST** `/api/pdf/from-html` - Generate PDF from HTML (see `MANUAL_TESTING.md` for curl examples)
- **GET** `/ui` - Web interface for manual testing
- **GET** `/health` - Health check endpoint

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
