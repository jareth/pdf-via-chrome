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

For more detailed examples including headers/footers, CSS injection, JavaScript execution, page ranges, and wait strategies, see [CLAUDE.md](CLAUDE.md).

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

**Status**: Phase 5-6 Complete - Core PDF generation, wait strategies, and test application implemented. Working towards advanced features and production readiness.
