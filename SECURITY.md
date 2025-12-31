# Security Policy

## Overview

**pdf-via-chrome** is a Java library for generating PDFs from HTML and URLs using headless Chrome. This document outlines security considerations when using the library and provides guidance for reporting vulnerabilities.

## Reporting Vulnerabilities

If you discover a security vulnerability in this project, please report it to us responsibly:

1. **Do not** open a public GitHub issue
2. Email security details to: [security contact email - to be configured]
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested remediation (if any)

We will acknowledge receipt within 48 hours and provide a detailed response within 5 business days.

## Security Considerations

### 1. SSRF (Server-Side Request Forgery) Protection

**Risk**: When generating PDFs from URLs, malicious users could attempt to access internal network resources.

**Built-in Protection**:
- URLs are automatically validated before processing
- Private IP addresses are blocked (10.x.x.x, 192.168.x.x, 172.16-31.x.x)
- Localhost and loopback addresses are blocked (127.x.x.x, ::1)
- Link-local addresses are blocked (169.254.x.x, fe80::/10)
- Only HTTP and HTTPS protocols are allowed
- Hostnames are resolved to verify they don't point to private IPs

**Additional Protection** (optional):

```java
// Use domain whitelist for strictest control
Set<String> allowedDomains = Set.of("example.com", "trusted.org");
UrlValidator.builder()
    .url(userProvidedUrl)
    .allowedDomains(allowedDomains)
    .validate();

// Or use domain blacklist
Set<String> blockedDomains = Set.of("internal-site.com");
UrlValidator.builder()
    .url(userProvidedUrl)
    .blockedDomains(blockedDomains)
    .validate();
```

**Recommendation**:
- Always validate and sanitize user-provided URLs before processing
- Use domain whitelisting in production environments when possible
- Never allow PDF generation from arbitrary user-provided URLs without validation

### 2. HTML Injection / XSS

**Risk**: Untrusted HTML content could contain malicious scripts or content.

**Mitigation**:
- **Warning**: This library does NOT sanitize HTML by default
- Chrome's headless mode executes JavaScript in the HTML
- Never generate PDFs from untrusted HTML without sanitization
- Consider using a library like OWASP Java HTML Sanitizer for untrusted content

**Example with HTML Sanitization** (requires adding `com.googlecode.owasp-java-html-sanitizer` dependency):

```java
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

// Sanitize untrusted HTML before PDF generation
PolicyFactory policy = Sanitizers.FORMATTING
    .and(Sanitizers.LINKS)
    .and(Sanitizers.BLOCKS);

String unsafeHtml = getUserInput(); // Potentially malicious
String safeHtml = policy.sanitize(unsafeHtml);

try (PdfGenerator generator = PdfGenerator.create().build()) {
    byte[] pdf = generator.fromHtml(safeHtml).generate();
}
```

**Recommendation**:
- Treat user-provided HTML as untrusted
- Sanitize HTML before PDF generation
- Consider disabling JavaScript for untrusted content
- Use Content Security Policy (CSP) headers when appropriate

### 3. Resource Exhaustion / DoS Protection

**Risk**: Malicious inputs could cause excessive resource consumption.

**Built-in Protection**:
- **Timeouts**: All operations have configurable timeouts
  - Default page load timeout: 30 seconds
  - Default Chrome startup timeout: 45 seconds
  - Chrome shutdown timeout: 5 seconds
- **Process isolation**: Each Chrome instance runs in a separate process
- **Automatic cleanup**: Resources are automatically released via AutoCloseable

**Timeout Configuration**:

```java
// Configure custom timeouts
try (PdfGenerator generator = PdfGenerator.create()
    .withTimeout(Duration.ofSeconds(60))  // Overall operation timeout
    .build()) {

    PageOptions pageOptions = PageOptions.builder()
        .pageLoadTimeout(Duration.ofSeconds(30))  // Page load timeout
        .build();

    byte[] pdf = generator.fromUrl(url).generate();
}
```

**Additional Protection Recommendations**:

```java
// 1. Limit concurrent PDF generations
ExecutorService executor = Executors.newFixedThreadPool(5); // Max 5 concurrent PDFs

// 2. Implement request queuing with limits
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
ExecutorService limitedExecutor = new ThreadPoolExecutor(
    2, 5, 60L, TimeUnit.SECONDS, queue,
    new ThreadPoolExecutor.CallerRunsPolicy()
);

// 3. Monitor Chrome memory usage
// Chrome typically uses 100-200MB per instance
// Set JVM and system memory limits accordingly
```

**Recommendation**:
- Limit concurrent PDF generation operations
- Implement request queuing and rate limiting
- Monitor system resources (CPU, memory)
- Set appropriate timeouts for all operations
- Consider implementing circuit breakers for high-traffic scenarios

### 4. Chrome Sandbox Mode

**Risk**: Running Chrome without sandboxing can expose the system to browser vulnerabilities.

**Default Behavior**:
- Chrome runs with sandbox **ENABLED** by default
- Sandbox provides process isolation and security

**Docker/Container Environments**:
- The `--no-sandbox` flag is sometimes required in containers
- Use with caution as it reduces security

```java
// Only disable sandbox in trusted container environments
try (PdfGenerator generator = PdfGenerator.create()
    .withNoSandbox(true)  // ⚠️ Use only in trusted environments
    .withDisableDevShmUsage(true)  // For limited /dev/shm
    .build()) {
    // ...
}
```

**Recommendation**:
- Keep sandbox enabled unless absolutely necessary
- Only disable in isolated, trusted environments (e.g., containers)
- Document why `--no-sandbox` is required in your deployment
- Consider security implications before disabling

### 5. Temporary File Security

**Risk**: Temporary files could contain sensitive data or be accessed by unauthorized users.

**Built-in Protection**:
- Chrome user data directories are created in system temp directory
- Temporary directories are automatically cleaned up on close
- Uses Java's `Files.createTempDirectory()` with default permissions

**Recommendation**:
- Ensure system temp directory has appropriate permissions
- Clean up PDF data from memory when no longer needed
- Consider encrypting PDFs containing sensitive information
- Implement proper file system access controls

### 6. Dependency Vulnerabilities

**Built-in Protection**:
- OWASP Dependency-Check runs automatically on `mvn verify`
- Build fails if critical/high vulnerabilities (CVSS ≥ 7) are found
- Dependencies are regularly updated

**Check for Vulnerabilities**:

```bash
# Run dependency vulnerability scan
mvn dependency-check:check

# View reports
# - target/dependency-check-report.html
# - target/dependency-check-report.json
```

**Recommendation**:
- Run dependency scans regularly
- Keep dependencies up to date
- Review security advisories for Chrome and dependencies
- Subscribe to security mailing lists

### 7. Input Validation

**General Recommendations**:

```java
// 1. Validate all user inputs
if (url == null || url.isBlank() || url.length() > 2048) {
    throw new IllegalArgumentException("Invalid URL");
}

// 2. Limit HTML content size
if (html.length() > 1_000_000) {  // 1MB limit
    throw new IllegalArgumentException("HTML content too large");
}

// 3. Validate PDF options
PdfOptions options = PdfOptions.builder()
    .scale(Math.max(0.1, Math.min(2.0, userScale)))  // Clamp to valid range
    .margins("1cm")  // Use constants instead of user input
    .build();

// 4. Sanitize file paths
Path outputPath = Paths.get(BASE_DIR, sanitizeFilename(userFilename));
if (!outputPath.startsWith(BASE_DIR)) {
    throw new SecurityException("Path traversal attempt");
}
```

## Security Best Practices

### Production Deployment Checklist

- [ ] Enable URL validation with domain whitelisting
- [ ] Sanitize all user-provided HTML content
- [ ] Configure appropriate timeouts for all operations
- [ ] Implement rate limiting and request queuing
- [ ] Keep Chrome sandbox enabled (unless in trusted container)
- [ ] Run OWASP dependency scans in CI/CD pipeline
- [ ] Monitor system resources (CPU, memory, disk)
- [ ] Implement proper logging and alerting
- [ ] Use HTTPS for all URL-based PDF generation
- [ ] Encrypt PDFs containing sensitive data
- [ ] Implement proper access controls
- [ ] Regular security updates and patches

### Defense in Depth

Implement multiple layers of security:

1. **Input Validation**: Validate and sanitize all inputs
2. **SSRF Protection**: Use URL whitelisting
3. **HTML Sanitization**: Sanitize untrusted HTML
4. **Resource Limits**: Implement timeouts and rate limiting
5. **Process Isolation**: Chrome runs in separate process
6. **Monitoring**: Track resource usage and failures
7. **Updates**: Keep dependencies and Chrome up to date

## Known Limitations

1. **JavaScript Execution**: The library executes JavaScript in HTML by default
   - Risk: Malicious scripts could access page content
   - Mitigation: Sanitize HTML or disable JavaScript for untrusted content

2. **Chrome Binary**: The library depends on a Chrome/Chromium installation
   - Risk: Vulnerabilities in Chrome affect the library
   - Mitigation: Keep Chrome updated to latest stable version

3. **Memory Usage**: Chrome instances can use 100-200MB each
   - Risk: High concurrent usage could exhaust memory
   - Mitigation: Limit concurrent operations, monitor memory

## Supported Versions

| Version | Supported | Security Updates |
|---------|-----------|------------------|
| 1.0.x   | ✅ Yes    | Yes              |
| < 1.0   | ❌ No     | No               |

## Security Updates

Security updates will be released as needed for supported versions. Critical vulnerabilities will be addressed within:
- **Critical (CVSS 9.0-10.0)**: 24-48 hours
- **High (CVSS 7.0-8.9)**: 1-2 weeks
- **Medium (CVSS 4.0-6.9)**: 1 month
- **Low (CVSS 0.1-3.9)**: Best effort

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP SSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)
- [OWASP HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer)
- [Chrome Security](https://www.chromium.org/Home/chromium-security/)
- [NVD - National Vulnerability Database](https://nvd.nist.gov/)

## Contact

For security-related questions or concerns:
- Security issues: [security contact email]
- General questions: GitHub Issues
- Project maintainers: See CONTRIBUTORS.md
