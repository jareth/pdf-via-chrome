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
- **headless-chrome-pdf-test-app**: A Spring Boot application for manual testing (not yet implemented).

## Core Architecture

### Resource Management Pattern

This project uses the **AutoCloseable pattern** extensively to ensure proper cleanup of Chrome processes and WebSocket connections. Always use try-with-resources:

```java
try (ChromeManager chromeManager = new ChromeManager(options)) {
    ChromeProcess process = chromeManager.start();
    try (CdpSession session = CdpClient.createSession(process)) {
        // Use session
    }
}
```

**Key AutoCloseable classes:**
- `ChromeManager` - Manages Chrome process lifecycle
- `CdpSession` - Manages CDP WebSocket connection
- Future: `PdfGenerator` - Will be the top-level API

### Builder Pattern

The project uses the builder pattern for configuration objects:

- `ChromeOptions.Builder` - Chrome browser configuration (headless, debugging port, flags, timeouts)
- `CdpClient.Builder` - CDP session configuration
- Future: `PdfOptions.Builder`, `PageOptions.Builder`

### Package Structure (headless-chrome-pdf-core)

Base package: `com.github.headlesschromepdf`

- **chrome/**: Browser process management
  - `ChromeManager` - Launches and manages Chrome process lifecycle (AutoCloseable)
  - `ChromeOptions` - Chrome configuration with builder pattern
  - `ChromeProcess` - Represents a running Chrome process
  - `ChromeLaunchException` - Thrown when Chrome fails to start

- **cdp/**: Chrome DevTools Protocol interaction
  - `CdpSession` - Manages WebSocket connection to Chrome (AutoCloseable)
  - `CdpClient` - Factory for creating CDP sessions
  - `CdpConnectionException` - Thrown when CDP connection fails

- **util/**: Utilities
  - `ChromePathDetector` - Auto-detects Chrome installation on Windows/Linux/macOS

- **api/**: Public API (not yet implemented)
  - Will contain: `PdfGenerator`, `PdfOptions`, `PdfGenerationResult`, `PageOptions`

- **converter/**: Conversion implementations (not yet implemented)
  - Will contain: `HtmlToPdfConverter`, `UrlToPdfConverter`

- **wait/**: Wait strategies for page readiness (not yet implemented)
  - Will contain: `WaitStrategy`, `NetworkIdleWait`, `ElementWait`, `TimeoutWait`

- **exception/**: Custom exceptions (partially implemented)
  - Planned: `PdfGenerationException`, `BrowserTimeoutException`

## Implementation Status

Currently in **Phase 2** (Core Infrastructure). Completed components:

- Chrome path detection for Windows/Linux/macOS
- ChromeManager with AutoCloseable for browser lifecycle management
- CdpSession with AutoCloseable for CDP connections
- ChromeOptions builder pattern
- Process tracking and cleanup with shutdown hooks
- Exception hierarchy started

**Not yet implemented:**
- Public API (PdfGenerator, PdfOptions)
- PDF conversion logic
- Wait strategies
- Test application

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

## Development Notes

- **Thread safety**: All public APIs must be thread-safe (design consideration for future work)
- **Resource cleanup**: Always ensure processes are cleaned up to prevent zombie Chrome instances
- **Platform differences**: Test on Windows/Linux/macOS as Chrome paths and behavior differ
- **Memory**: Chrome can be memory-intensive; consider memory requirements in design
- **Logging levels**: Use TRACE for CDP protocol events, DEBUG for internal operations, INFO for major lifecycle events
