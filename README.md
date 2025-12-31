# pdf-via-chrome

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-Coming%20Soon-lightgrey.svg)](#)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](#)

> **Note**: This project is under active development. APIs and features are subject to change.

## Overview

**pdf-via-chrome** is a Java library for generating PDFs from HTML content and URLs using headless Chrome/Chromium via the Chrome DevTools Protocol (CDP). It provides a clean, fluent API for PDF generation with extensive customization options, making it ideal for server-side PDF generation in Java applications.

Unlike browser automation frameworks like Selenium or Playwright, this library focuses specifically on PDF generation, offering a lightweight solution that leverages the native PDF rendering capabilities of Chrome/Chromium.

## Features

- **Simple API**: Fluent builder pattern for easy PDF generation
- **Multiple Input Sources**: Generate PDFs from HTML strings, URLs, or DOM Documents (org.w3c.dom.Document)
- **Extensive Customization**: Configure page size, margins, orientation, headers, footers, page ranges, and more
- **CSS & JavaScript Injection**: Inject custom CSS and execute JavaScript before PDF generation
- **Chrome DevTools Protocol**: Direct integration with Chrome via CDP for optimal performance
- **Wait Strategies**: Built-in strategies for handling dynamic content:
  - Fixed duration timeout (TimeoutWait)
  - Network idle detection (NetworkIdleWait)
  - DOM element presence (ElementWait)
  - Custom JavaScript conditions (CustomConditionWait)
- **Resource Management**: Automatic browser lifecycle management with AutoCloseable pattern
- **Thread Safety**: Thread-safe concurrent PDF generation from a single PdfGenerator instance
- **Exception Handling**: Comprehensive error handling with detailed diagnostics
- **Testcontainers Support**: Easy integration testing with containerized Chrome

## Requirements

- **Java**: 17 or higher
- **Maven**: 3.8+ (for building)
- **Chrome/Chromium**: Installed and accessible in system PATH, or provide custom path
  - Supported on Linux, macOS, and Windows
  - Minimum Chrome version: 90+

## Building

Clone the repository and build with Maven:

```bash
# Clone the repository
git clone https://github.com/your-username/headless-chrome-pdf.git
cd headless-chrome-pdf

# Build the project
mvn clean install

# Skip tests if needed
mvn clean install -DskipTests

# Run tests only
mvn test

# Run integration tests
mvn verify

# Some test are skipped by default due to requiring a local chrome setup
mvn verify -DCHROME_INTEGRATION_TESTS=true

# Run security vulnerability scan
mvn dependency-check:check

# Security scan is automatically run during 'mvn verify'
```

The build will produce:
- `pdf-via-chrome/target/pdf-via-chrome-1.0.0-SNAPSHOT.jar` - Core library
- `pdf-via-chrome-test-app/target/pdf-via-chrome-test-app-1.0.0-SNAPSHOT.jar` - Test application

## Module Structure

This project uses a Maven multi-module structure:

```
headless-chrome-pdf/
├── pom.xml                              # Parent POM with dependency management
├── pdf-via-chrome/            # Core library module
│   ├── src/
│   │   ├── main/java/                   # Library source code
│   │   │   └── com/fostermoore/pdfviachrome/
│   │   │       ├── api/                 # Public API interfaces and builders
│   │   │       ├── chrome/              # Chrome browser management
│   │   │       ├── cdp/                 # CDP protocol interaction
│   │   │       ├── converter/           # Conversion implementations
│   │   │       ├── wait/                # Wait strategies
│   │   │       ├── exception/           # Custom exceptions
│   │   │       └── util/                # Utility classes
│   │   ├── test/java/                   # Unit tests
│   │   └── test/resources/              # Test resources
│   └── pom.xml
└── pdf-via-chrome-test-app/        # Test application module
    ├── src/
    │   ├── main/java/                   # Spring Boot application
    │   └── main/resources/              # Application configuration
    └── pom.xml
```

### Module Descriptions

- **pdf-via-chrome**: The main library containing all PDF generation functionality. This is the module you'll depend on in your projects.
- **pdf-via-chrome-test-app**: A simple Spring Boot web application for manual testing and demonstration purposes. Not intended for production use.

## Quick Start

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.fostermoore</groupId>
    <artifactId>pdf-via-chrome</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
// Generate PDF from HTML with default options
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml("<html><body><h1>Hello World</h1></body></html>")
        .generate();
    Files.write(Path.of("output.pdf"), pdf);
}

// Generate PDF from URL
try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromUrl("https://example.com")
        .generate();
    Files.write(Path.of("webpage.pdf"), pdf);
}

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

## Usage Examples

### Headers and Footers

Add headers and footers to your PDFs with custom templates:

```java
// Simple page numbers in footer
PdfOptions options = PdfOptions.builder()
    .simplePageNumbers()  // "Page X of Y"
    .build();

// Document title in header
PdfOptions options = PdfOptions.builder()
    .headerWithTitle()
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

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(htmlContent)
        .withOptions(options)
        .generate();
    Files.write(Path.of("document.pdf"), pdf);
}
```

**Supported template variables:**
- `<span class="pageNumber"></span>` - Current page number
- `<span class="totalPages"></span>` - Total pages
- `<span class="date"></span>` - Formatted date
- `<span class="title"></span>` - Document title
- `<span class="url"></span>` - Document URL

### CSS Injection

Inject custom CSS to override page styles for printing:

```java
// Inject custom CSS
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

### JavaScript Execution

Execute JavaScript to modify the page before PDF generation:

```java
String html = """
    <!DOCTYPE html>
    <html>
    <body>
        <h1 id="title">Original Title</h1>
        <div class="ads">Advertisement</div>
        <p class="content">Important content here</p>
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
    """;

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(html)
        .executeJavaScript(asyncJsCode)
        .generate();
    Files.write(Path.of("output.pdf"), pdf);
}
```

### Page Ranges

Select specific pages for PDF output:

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
        <div class="page-break"><h1>Page 1</h1></div>
        <div class="page-break"><h1>Page 2</h1></div>
        <div class="page-break"><h1>Page 3</h1></div>
        <div class="page-break"><h1>Page 4</h1></div>
        <div><h1>Page 5</h1></div>
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
}
```

**Page range format:**
- Single page: `"1"` or `"5"`
- Range: `"1-5"` or `"10-20"`
- Multiple pages: `"1,3,5,7"`
- Mixed: `"1-5, 8, 11-13, 20"`
- All pages: `""` (empty string)

### Wait Strategies

Handle dynamic content with wait strategies:

```java
import com.fostermoore.pdfviachrome.wait.*;

// Wait for a fixed duration
WaitStrategy timeout = WaitStrategy.timeout(Duration.ofSeconds(5));

// Wait for network to become idle (no activity for 500ms)
WaitStrategy networkIdle = WaitStrategy.networkIdle(Duration.ofMillis(500));

// Wait for a specific DOM element to appear
WaitStrategy elementWait = WaitStrategy.elementPresent("#content-loaded");

// Wait for custom JavaScript condition to be true
WaitStrategy customWait = WaitStrategy.customCondition("window.myApp && window.myApp.ready === true");

// Using wait strategies with low-level API
ChromeOptions chromeOptions = ChromeOptions.builder().build();
try (ChromeManager chromeManager = new ChromeManager(chromeOptions)) {
    ChromeProcess process = chromeManager.start();

    try (CdpSession session = new CdpSession(process.getWebSocketDebuggerUrl())) {
        session.connect();

        // Wait before converting
        WaitStrategy networkIdle = WaitStrategy.networkIdle();
        networkIdle.await(session.getService(), Duration.ofSeconds(30));

        HtmlToPdfConverter converter = new HtmlToPdfConverter(session);
        byte[] pdf = converter.convert(htmlContent, PdfOptions.defaults());
        Files.write(Path.of("output.pdf"), pdf);
    }
}
```

**Common use cases:**
- Single-page applications with lazy loading: `WaitStrategy.networkIdle()`
- Pages with loading indicators: `WaitStrategy.customCondition("document.querySelector('.loading-spinner') === null")`
- Content that depends on specific elements: `WaitStrategy.elementPresent(".main-content")`

### Combined Features

Combine multiple features for complex PDF generation:

```java
// Combine CSS injection, JavaScript execution, and custom options
String css = ".sidebar { display: none; }";
String js = "document.querySelector('.main-content').style.width = '100%';";

PdfOptions options = PdfOptions.builder()
    .paperSize(PaperFormat.A4)
    .printBackground(true)
    .standardHeaderFooter()
    .margins("1cm")
    .build();

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(htmlContent)
        .withCustomCss(css)
        .executeJavaScript(js)
        .withOptions(options)
        .generate();
    Files.write(Path.of("custom.pdf"), pdf);
}
```

### DOM Document Input

Generate PDFs from DOM Documents:

```java
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

// Create a DOM Document programmatically
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();
Document document = builder.newDocument();

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
```

### Thread-Safe Concurrent Generation

Reuse a single PdfGenerator instance for multiple PDFs:

```java
// Thread-safe: reuse generator for multiple PDFs
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

## Configuration

### PdfGenerator Configuration

Configure Chrome settings when building the PdfGenerator:

```java
PdfGenerator generator = PdfGenerator.create()
    .withChromePath(Path.of("/path/to/chrome"))      // Custom Chrome path
    .withTimeout(Duration.ofSeconds(60))              // Timeout for operations
    .withHeadless(true)                               // Run Chrome in headless mode
    .withNoSandbox(true)                              // Disable sandbox (for Docker)
    .withDisableDevShmUsage(true)                     // Disable /dev/shm usage
    .build();
```

**Configuration options:**
- `withChromePath(Path)` - Path to Chrome/Chromium executable (auto-detected if not specified)
- `withTimeout(Duration)` - Timeout for Chrome operations (default: 30 seconds)
- `withHeadless(boolean)` - Run Chrome in headless mode (default: true)
- `withNoSandbox(boolean)` - Disable Chrome sandbox (required for Docker, default: false)
- `withDisableDevShmUsage(boolean)` - Disable /dev/shm usage (useful in containers with limited /dev/shm, default: false)

### PDF Options

Customize PDF output with PdfOptions:

```java
PdfOptions options = PdfOptions.builder()
    // Paper size
    .paperSize(PaperFormat.A4)           // LETTER, A4, LEGAL, A3, TABLOID, etc.
    .width("8.5in")                       // Custom width (overrides paperSize)
    .height("11in")                       // Custom height

    // Layout
    .landscape(true)                      // Landscape orientation (default: false)
    .scale(0.8)                           // Scale factor 0.1-2.0 (default: 1.0)

    // Margins
    .margins("1cm")                       // All margins
    .marginTop("2cm")                     // Individual margins
    .marginBottom("2cm")
    .marginLeft("1.5cm")
    .marginRight("1.5cm")

    // Graphics and backgrounds
    .printBackground(true)                // Print background graphics (default: false)

    // Headers and footers
    .displayHeaderFooter(true)            // Enable headers/footers (default: false)
    .headerTemplate("<div>Header</div>")  // Custom header HTML
    .footerTemplate("<div>Footer</div>")  // Custom footer HTML

    // Page selection
    .pageRanges("1-5, 8, 11-13")         // Select specific pages

    // CSS page size
    .preferCssPageSize(true)              // Use CSS-defined page size (default: false)

    .build();
```

**Paper formats:**
- `LETTER` (8.5 x 11 inches)
- `LEGAL` (8.5 x 14 inches)
- `TABLOID` (11 x 17 inches)
- `LEDGER` (17 x 11 inches)
- `A0`, `A1`, `A2`, `A3`, `A4`, `A5`, `A6`

**Margin units:**
Margins support multiple units: `"1in"`, `"2.54cm"`, `"96px"`, `"72pt"`

### Chrome Options (Low-Level API)

For advanced use cases, you can configure Chrome directly:

```java
ChromeOptions options = ChromeOptions.builder()
    .chromePath(Path.of("/usr/bin/chromium"))
    .headless(true)
    .debuggingPort(9222)
    .userDataDir(Path.of("/tmp/chrome-data"))
    .windowSize(1920, 1080)
    .timeout(Duration.ofSeconds(60))
    .noSandbox(true)
    .disableDevShmUsage(true)
    .addArgument("--disable-gpu")
    .addArgument("--font-render-hinting=none")
    .build();

try (ChromeManager chromeManager = new ChromeManager(options)) {
    ChromeProcess process = chromeManager.start();
    // Use the Chrome process...
}
```

## Platform Support

### Windows

**Chrome Detection:**
Automatically searches these locations:
- `C:\Program Files\Google\Chrome\Application\chrome.exe`
- `C:\Program Files (x86)\Google\Chrome\Application\chrome.exe`
- `%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe`

**Manual Configuration:**
```java
PdfGenerator generator = PdfGenerator.create()
    .withChromePath(Path.of("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"))
    .build();
```

**Notes:**
- Chrome 90+ recommended
- Windows 10/11 supported
- Use double backslashes `\\` in paths or forward slashes `/`

### Linux

**Chrome Detection:**
Automatically searches these locations:
- `/usr/bin/google-chrome`
- `/usr/bin/chromium-browser`
- `/usr/bin/chromium`
- `/snap/bin/chromium`

**Installing Chrome/Chromium:**
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y chromium-browser

# Or Google Chrome
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt install ./google-chrome-stable_current_amd64.deb

# CentOS/RHEL
sudo yum install chromium
```

**Notes:**
- Requires X11 libraries even in headless mode
- For minimal environments, install dependencies: `fonts-liberation libnss3 libatk-bridge2.0-0`

### macOS

**Chrome Detection:**
Automatically searches these locations:
- `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`
- `/Applications/Chromium.app/Contents/MacOS/Chromium`

**Installing Chrome:**
```bash
# Using Homebrew
brew install --cask google-chrome
```

**Notes:**
- macOS 10.15+ recommended
- ARM (M1/M2) and Intel both supported

## Docker

### Dockerfile Example

```dockerfile
FROM eclipse-temurin:17-jre-alpine

# Install Chromium
RUN apk add --no-cache \
    chromium \
    nss \
    freetype \
    harfbuzz \
    ca-certificates \
    ttf-freefont

# Set environment variable for Chrome path
ENV CHROME_BIN=/usr/bin/chromium-browser

# Copy application
COPY target/your-app.jar /app/app.jar

WORKDIR /app

# Run with sandbox disabled (required in Docker)
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Configuration

When running in Docker, disable the Chrome sandbox and /dev/shm usage:

```java
PdfGenerator generator = PdfGenerator.create()
    .withNoSandbox(true)              // Required in Docker
    .withDisableDevShmUsage(true)     // Recommended if /dev/shm is limited
    .build();
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  pdf-service:
    build: .
    environment:
      - CHROME_BIN=/usr/bin/chromium-browser
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '2'
```

### Testcontainers Support

For integration testing with Docker:

```java
@Testcontainers
class PdfGenerationIT {
    @Container
    private static final GenericContainer<?> chrome = new GenericContainer<>(
            DockerImageName.parse("zenika/alpine-chrome:latest"))
        .withExposedPorts(9222)
        .withCommand("--remote-debugging-port=9222", "--remote-debugging-address=0.0.0.0");

    @Test
    void testPdfGeneration() {
        // Use chrome.getHost() and chrome.getMappedPort(9222)
    }
}
```

## Troubleshooting

### Chrome Not Found

**Problem:** `ChromeNotFoundException: Could not find Chrome installation`

**Solutions:**
1. Install Chrome/Chromium on your system
2. Specify the Chrome path explicitly:
   ```java
   PdfGenerator generator = PdfGenerator.create()
       .withChromePath(Path.of("/path/to/chrome"))
       .build();
   ```
3. Set the `CHROME_BIN` environment variable

### Chrome Won't Start in Docker

**Problem:** Chrome crashes or fails to start in Docker containers

**Solutions:**
1. Disable sandbox mode (required in Docker):
   ```java
   PdfGenerator generator = PdfGenerator.create()
       .withNoSandbox(true)
       .build();
   ```
2. Add required dependencies to Dockerfile:
   ```dockerfile
   RUN apt-get update && apt-get install -y \
       chromium \
       fonts-liberation \
       libnss3 \
       libatk-bridge2.0-0
   ```

### PDF Generation Timeout

**Problem:** `BrowserTimeoutException: Operation timed out`

**Solutions:**
1. Increase timeout:
   ```java
   PdfGenerator generator = PdfGenerator.create()
       .withTimeout(Duration.ofSeconds(120))
       .build();
   ```
2. Use wait strategies for slow-loading content:
   ```java
   WaitStrategy networkIdle = WaitStrategy.networkIdle(Duration.ofSeconds(2));
   // Apply wait strategy before conversion
   ```
3. Simplify HTML or reduce external resources

### Page Load Failures

**Problem:** `PageLoadException: Failed to load page`

**Solutions:**
1. Check URL is accessible and valid
2. Verify network connectivity
3. For local files, use `file://` protocol:
   ```java
   generator.fromUrl("file:///path/to/local.html")
   ```
4. Check for JavaScript errors in the page

### Memory Issues

**Problem:** OutOfMemoryError or Chrome crashes

**Solutions:**
1. Increase JVM heap size: `java -Xmx2G -jar app.jar`
2. Allocate more memory in Docker: `--memory=2g`
3. Reuse PdfGenerator instances instead of creating new ones
4. Process PDFs in batches rather than all at once
5. Disable /dev/shm usage in containers:
   ```java
   PdfGenerator generator = PdfGenerator.create()
       .withDisableDevShmUsage(true)
       .build();
   ```

### Headers/Footers Not Appearing

**Problem:** Headers and footers are not visible in the PDF

**Solutions:**
1. Ensure margins are set (headers/footers appear in margins):
   ```java
   PdfOptions options = PdfOptions.builder()
       .displayHeaderFooter(true)
       .marginTop("1cm")      // Required for header
       .marginBottom("1cm")   // Required for footer
       .headerTemplate("<div>Header</div>")
       .build();
   ```
2. Use complete HTML in templates with inline styles:
   ```java
   .headerTemplate("<div style=\"font-size: 10px; width: 100%; text-align: center;\">Header</div>")
   ```

### CSS Not Applied

**Problem:** Custom CSS is not affecting the PDF output

**Solutions:**
1. Use `!important` to override existing styles:
   ```css
   .no-print { display: none !important; }
   ```
2. Ensure CSS is injected after page loads (library handles this automatically)
3. Use `@media print` queries for print-specific styles:
   ```css
   @media print {
       .page-break { page-break-after: always; }
   }
   ```

### JavaScript Not Executing

**Problem:** JavaScript code doesn't modify the page before PDF generation

**Solutions:**
1. Check for JavaScript errors in the code
2. Use `await` for asynchronous operations:
   ```javascript
   await new Promise(resolve => setTimeout(resolve, 1000));
   ```
3. Ensure JavaScript is valid and doesn't reference undefined variables
4. Test JavaScript in browser console first

### SSRF Security Warnings

**Problem:** URL validation rejects legitimate URLs

**Solutions:**
1. Understand that the library blocks private IPs by default for security
2. To allow specific domains, disable validation (use with caution):
   ```java
   // Custom validation with allowlist
   UrlValidator validator = UrlValidator.builder()
       .allowPrivateIpAddresses()  // Use only if you trust the URLs
       .build();
   ```
3. See [SECURITY.md](SECURITY.md) for comprehensive security documentation

### Process Cleanup

**Problem:** Zombie Chrome processes remain after application shutdown

**Solutions:**
1. Always use try-with-resources:
   ```java
   try (PdfGenerator generator = PdfGenerator.create().build()) {
       // Generate PDFs...
   } // Automatic cleanup
   ```
2. If not using try-with-resources, call `close()` explicitly:
   ```java
   PdfGenerator generator = PdfGenerator.create().build();
   try {
       // Generate PDFs...
   } finally {
       generator.close();
   }
   ```
3. Shutdown hooks automatically clean up processes, but explicit cleanup is preferred

### Performance Issues

**Problem:** PDF generation is slow or consuming too many resources

**Solutions:**
1. Reuse PdfGenerator instances (3-5x faster):
   ```java
   // Good: reuse generator
   try (PdfGenerator generator = PdfGenerator.create().build()) {
       for (String html : htmlList) {
           byte[] pdf = generator.fromHtml(html).generate();
       }
   }

   // Bad: create new generator each time
   for (String html : htmlList) {
       try (PdfGenerator generator = PdfGenerator.create().build()) {
           byte[] pdf = generator.fromHtml(html).generate();
       }
   }
   ```
2. Simplify HTML and reduce external resources
3. Use concurrent processing for batch operations:
   ```java
   ExecutorService executor = Executors.newFixedThreadPool(4);
   try (PdfGenerator generator = PdfGenerator.create().build()) {
       List<Future<byte[]>> futures = htmlList.stream()
           .map(html -> executor.submit(() -> generator.fromHtml(html).generate()))
           .toList();

       for (Future<byte[]> future : futures) {
           byte[] pdf = future.get();
           // Process PDF...
       }
   }
   ```
4. See [docs/PERFORMANCE.md](docs/PERFORMANCE.md) for comprehensive performance guidance

For more detailed information, see the comprehensive documentation in [CLAUDE.md](CLAUDE.md).

## Security

This project includes automated security vulnerability scanning using OWASP Dependency-Check.

### Dependency Scanning

The build automatically scans all dependencies for known security vulnerabilities:

```bash
# Run security scan manually
mvn dependency-check:check

# Security scan runs automatically during verify phase
mvn verify
```

**Configuration:**
- Fails build on vulnerabilities with CVSS score >= 7 (HIGH and CRITICAL)
- Generates HTML and JSON reports in `target/dependency-check-report.html`
- Uses suppression file `dependency-check-suppressions.xml` for managing false positives
- Updates vulnerability database automatically (cached in `~/.m2/dependency-check-data`)

**First Run:**
The initial scan downloads the National Vulnerability Database (~10-15 minutes). Subsequent scans are much faster using the cached database.

**Managing False Positives:**
If you encounter false positives, add suppression entries to `dependency-check-suppressions.xml`. See the file for examples.

**Optional NVD API Key:**
For faster database updates, you can obtain a free NVD API key from https://nvd.nist.gov/developers/request-an-api-key and set it as an environment variable:

```bash
export NVD_API_KEY="your-api-key-here"
```

Then uncomment the `nvdApiKey` configuration in the parent `pom.xml`.

## Documentation

- **[CLAUDE.md](CLAUDE.md)**: Comprehensive developer guide including:
  - Complete API usage examples (basic and advanced)
  - Architecture and design patterns
  - Build commands and testing conventions
  - Integration test suites and coverage
  - Chrome process management details
  - Test application REST API documentation
- **Javadoc**: API documentation available via `mvn javadoc:javadoc` (target/site/apidocs)
- **Test Application**: Spring Boot demo app with REST endpoints for manual testing

## Contributing

Contributions are welcome! Please feel free to submit issues, fork the repository, and create pull requests.

Guidelines for contributing:
- Follow the existing code style
- Add unit tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PRs

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

```
Copyright 2024 pdf-via-chrome contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Acknowledgments

Built using:
- [chrome-devtools-java-client](https://github.com/kklisura/chrome-devtools-java-client) - Chrome DevTools Protocol client
- [Spring Boot](https://spring.io/projects/spring-boot) - For the test application
- [JUnit 5](https://junit.org/junit5/) - Testing framework
- [Testcontainers](https://www.testcontainers.org/) - Integration testing with Docker

---

**Status**: Phase 7 In Progress - Documentation and Polish. Core PDF generation, wait strategies, test application, security features, and comprehensive usage documentation complete.
