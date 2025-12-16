# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**headless-chrome-pdf** is a Java library for generating PDFs from HTML content and URLs using headless Chrome/Chromium via the Chrome DevTools Protocol (CDP). It's a multi-module Maven project targeting Java 17+.

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
mvn test -pl headless-chrome-pdf-core

# Run a single test class
mvn test -Dtest=ChromeManagerTest

# Run a single test method
mvn test -Dtest=ChromeManagerTest#testStartChrome
```

## Module Structure

This is a Maven multi-module project with two modules:

- **headless-chrome-pdf-core**: The main library containing all PDF generation functionality. All core implementation happens here.
- **headless-chrome-pdf-test-app**: A Spring Boot application for manual testing and demonstrating the library. Includes REST API endpoints for HTML-to-PDF generation.

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

### Package Structure (headless-chrome-pdf-core)

Base package: `com.github.headlesschromepdf`

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

- **converter/**: Conversion implementations (PARTIALLY IMPLEMENTED)
  - `HtmlToPdfConverter` - Converts HTML strings to PDF with DOMContentLoaded event handling
  - `ConversionContext` - Context object for conversion operations
  - Not yet implemented: `UrlToPdfConverter`

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

- **wait/**: Wait strategies for page readiness (NOT YET IMPLEMENTED)
  - Planned: `WaitStrategy`, `NetworkIdleWait`, `ElementWait`, `TimeoutWait`

### Package Structure (headless-chrome-pdf-test-app)

Base package: `com.github.headlesschromepdf.testapp`

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

Currently in **Phase 4-6** (Conversion Logic and Test Application). The project has core PDF generation functionality working and a Spring Boot test application with REST API endpoints.

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

**Conversion Logic (Phase 4 - Partial):**
- HtmlToPdfConverter - Full HTML to PDF conversion with event-based page loading
- ConversionContext - Supporting context for conversions
- PdfGenerator.fromHtml() - Integrated HTML to PDF generation

**Test Application (Phase 6 - Partial):**
- Spring Boot application structure with health check endpoint
- PdfController - REST endpoint POST /api/pdf/from-html for HTML-to-PDF generation
- PdfOptionsDto - Complete JSON mapping for all PDF options
- HtmlRequest DTO with validation (@NotBlank)
- GlobalExceptionHandler - Comprehensive error handling with appropriate HTTP status codes
- PdfGenerator configured as Spring Bean with automatic resource cleanup
- Unit tests (8 tests, all passing) and integration tests (5 tests) for REST endpoints
- Manual testing documentation with curl and PowerShell examples

### Not Yet Implemented

- **Wait strategies (wait/ package)**: NetworkIdleWait, ElementWait, TimeoutWait, custom conditions
- **URL converter**: Dedicated UrlToPdfConverter (currently handled via PdfGenerator.fromUrl() with basic implementation)
- **Test application UI**: Thymeleaf-based web UI for manual testing (REST API endpoints completed)
- **Test application URL endpoint**: POST /api/pdf/from-url endpoint (moved to Version 2, Phase 8)
- **Advanced features**: Browser pooling, request interception, custom headers, authentication

### Working Features

The library can currently:
1. Auto-detect Chrome installation or use custom path
2. Launch and manage Chrome processes with proper cleanup
3. Generate PDFs from HTML strings with full customization
4. Generate PDFs from URLs (basic implementation)
5. Configure paper size (Letter, A4, Legal, etc.) and custom dimensions
6. Set margins with multiple units (inches, cm, px)
7. Control orientation (portrait/landscape)
8. Enable/disable background graphics
9. Set custom headers and footers
10. Handle thread-safe concurrent PDF generation

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

## Key Technologies

- **Chrome DevTools Protocol**: Using `chrome-devtools-java-client` (com.github.kklisura.cdt:cdt-java-client:4.0.0)
- **Logging**: SLF4J API (implementation chosen by library users)
- **Testing**: JUnit 5, Mockito, AssertJ, Testcontainers (for integration tests with containerized Chrome)

## Chrome Process Management

The ChromeManager handles Chrome with these important considerations:

1. **Auto-detection**: If no Chrome path is specified, ChromePathDetector will search standard locations
2. **User data directory**: Creates temporary directory by default, cleaned up on close
3. **Stability flags**: Automatically adds ~30 Chrome flags for stability (disable extensions, background tasks, etc.)
4. **Docker support**: Options for `--no-sandbox` and `--disable-dev-shm-usage` for container environments
5. **Graceful shutdown**: Attempts destroy() first, then destroyForcibly() after timeout
6. **WebSocket URL extraction**: Parses Chrome stdout to find debugging WebSocket URL

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

### Multiple PDFs with Single Generator

```java
// Reuse generator for multiple PDFs (thread-safe)
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf1 = generator.fromHtml(html1).generate();
    byte[] pdf2 = generator.fromUrl("https://example.com").generate();
    byte[] pdf3 = generator.fromHtml(html2)
        .withOptions(PdfOptions.builder().landscape(true).build())
        .generate();

    Files.write(Path.of("doc1.pdf"), pdf1);
    Files.write(Path.of("doc2.pdf"), pdf2);
    Files.write(Path.of("doc3.pdf"), pdf3);
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
mvn spring-boot:run -pl headless-chrome-pdf-test-app

# Or build and run the JAR
mvn clean package -pl headless-chrome-pdf-test-app
java -jar headless-chrome-pdf-test-app/target/headless-chrome-pdf-test-app-1.0.0-SNAPSHOT.jar
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

See `headless-chrome-pdf-test-app/MANUAL_TESTING.md` for more examples.

## Development Notes

- **Thread safety**: PdfGenerator is thread-safe via ReentrantLock; a single instance can generate multiple PDFs concurrently
- **Lazy initialization**: PdfGenerator only starts Chrome on first generate() call to minimize resource usage
- **Resource cleanup**: Always use try-with-resources to ensure Chrome processes are cleaned up and prevent zombie instances
- **Platform differences**: Test on Windows/Linux/macOS as Chrome paths and behavior differ
- **Memory**: Chrome can be memory-intensive; consider memory requirements in design (each Chrome instance ~100-200MB)
- **Logging levels**: Use TRACE for CDP protocol events, DEBUG for internal operations, INFO for major lifecycle events
- **Docker environments**: Use `withNoSandbox(true)` and `withDisableDevShmUsage(true)` when running in containers
