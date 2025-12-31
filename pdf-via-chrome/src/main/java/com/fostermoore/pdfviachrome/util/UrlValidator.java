package com.fostermoore.pdfviachrome.util;

import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for validating URLs to prevent SSRF (Server-Side Request Forgery) attacks.
 *
 * <p>This validator helps protect against malicious URLs that could:
 *
 * <ul>
 *   <li>Access internal/private network resources (192.168.x.x, 10.x.x.x, 172.16.x.x-172.31.x.x)
 *   <li>Access localhost or loopback addresses (127.x.x.x, ::1)
 *   <li>Use link-local addresses (169.254.x.x, fe80::/10)
 *   <li>Use file:// or other non-HTTP(S) protocols
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 *
 * <pre>{@code
 * // Basic validation (blocks private IPs)
 * UrlValidator.validate("https://example.com");
 *
 * // Validation with domain whitelist
 * Set<String> allowedDomains = Set.of("example.com", "trusted.org");
 * UrlValidator.builder()
 *     .url("https://example.com/page")
 *     .allowedDomains(allowedDomains)
 *     .validate();
 *
 * // Validation with domain blacklist
 * Set<String> blockedDomains = Set.of("malicious.com");
 * UrlValidator.builder()
 *     .url("https://some-site.com")
 *     .blockedDomains(blockedDomains)
 *     .validate();
 *
 * // Allow private IPs (not recommended for production)
 * UrlValidator.builder()
 *     .url("http://192.168.1.1")
 *     .allowPrivateIps(true)
 *     .validate();
 * }</pre>
 *
 * @since 1.0.0
 */
public class UrlValidator {

  private static final Logger logger = LoggerFactory.getLogger(UrlValidator.class);

  private static final Set<String> ALLOWED_PROTOCOLS = Set.of("http", "https", "data");

  // Private IP ranges (RFC 1918)
  private static final String PRIVATE_IP_10 = "10.";
  private static final String PRIVATE_IP_192 = "192.168.";
  private static final String LOCALHOST_127 = "127.";

  // IPv6 loopback and link-local
  private static final String IPV6_LOOPBACK = "::1";
  private static final String IPV6_LINK_LOCAL_PREFIX = "fe80:";

  // Link-local IPv4 range (169.254.0.0/16)
  private static final String LINK_LOCAL_169 = "169.254.";

  private final String url;
  private final boolean allowPrivateIps;
  private final Set<String> allowedDomains;
  private final Set<String> blockedDomains;

  private UrlValidator(Builder builder) {
    this.url = builder.url;
    this.allowPrivateIps = builder.allowPrivateIps;
    this.allowedDomains = builder.allowedDomains;
    this.blockedDomains = builder.blockedDomains;
  }

  /**
   * Validates a URL with default settings (blocks private IPs, allows HTTP/HTTPS only).
   *
   * @param url the URL to validate
   * @throws PdfGenerationException if the URL is invalid or potentially malicious
   */
  public static void validate(String url) {
    builder().url(url).validate();
  }

  /**
   * Creates a new builder for customizing validation rules.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Performs the validation checks on the URL.
   *
   * @throws PdfGenerationException if validation fails
   */
  public void validate() {
    if (url == null || url.isBlank()) {
      throw new PdfGenerationException("URL cannot be null or empty");
    }

    logger.debug("Validating URL: {}", url);

    // Data URLs are safe (no network requests), skip detailed validation
    if (url.toLowerCase().startsWith("data:")) {
      logger.debug("Data URL detected, skipping SSRF validation");
      return;
    }

    // Parse and validate URL structure
    URL parsedUrl = parseUrl(url);

    // Validate protocol
    validateProtocol(parsedUrl);

    // Check domain blacklist/whitelist
    validateDomainLists(parsedUrl);

    // Validate against SSRF attacks
    // Skip SSRF validation if:
    // 1. Private IPs are explicitly allowed, OR
    // 2. Whitelist is configured (whitelist takes precedence)
    if (!allowPrivateIps && (allowedDomains == null || allowedDomains.isEmpty())) {
      validateAgainstSsrf(parsedUrl);
    }

    logger.debug("URL validation passed: {}", url);
  }

  /**
   * Parses the URL and ensures it's well-formed.
   *
   * @param urlString the URL string to parse
   * @return the parsed URL object
   * @throws PdfGenerationException if the URL is malformed
   */
  private URL parseUrl(String urlString) {
    try {
      // First parse as URI for better validation
      URI uri = new URI(urlString);

      // Check protocol before converting to URL (some protocols may not be supported by URL class)
      String scheme = uri.getScheme();
      if (scheme == null) {
        throw new PdfGenerationException("URL must have a protocol (e.g., http:// or https://)");
      }

      // Convert to URL (this may fail for unsupported protocols)
      return uri.toURL();
    } catch (URISyntaxException e) {
      throw new PdfGenerationException(
          "Invalid URL syntax: " + urlString + " - " + e.getMessage(), e);
    } catch (MalformedURLException e) {
      // Extract protocol from error if possible
      String message = "Malformed URL: " + urlString;
      try {
        URI uri = new URI(urlString);
        String scheme = uri.getScheme();
        if (scheme != null && !ALLOWED_PROTOCOLS.contains(scheme.toLowerCase())) {
          message = String.format(
              "Protocol '%s' is not allowed. Only HTTP and HTTPS are supported.", scheme);
        }
      } catch (Exception ignored) {
        // Use default message
      }
      throw new PdfGenerationException(message, e);
    }
  }

  /**
   * Validates that the URL uses an allowed protocol (HTTP, HTTPS, or data).
   *
   * @param url the parsed URL
   * @throws PdfGenerationException if the protocol is not allowed
   */
  private void validateProtocol(URL url) {
    String protocol = url.getProtocol().toLowerCase();
    if (!ALLOWED_PROTOCOLS.contains(protocol)) {
      throw new PdfGenerationException(
          String.format(
              "Protocol '%s' is not allowed. Only HTTP, HTTPS, and data are supported.", protocol));
    }
  }

  /**
   * Validates the URL against domain whitelist and blacklist.
   *
   * @param url the parsed URL
   * @throws PdfGenerationException if domain is not allowed
   */
  private void validateDomainLists(URL url) {
    String host = url.getHost().toLowerCase();

    // Check whitelist first (if configured)
    if (allowedDomains != null && !allowedDomains.isEmpty()) {
      boolean allowed =
          allowedDomains.stream()
              .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
      if (!allowed) {
        throw new PdfGenerationException(
            String.format(
                "Domain '%s' is not in the allowed domains list: %s", host, allowedDomains));
      }
    }

    // Check blacklist (if configured)
    if (blockedDomains != null && !blockedDomains.isEmpty()) {
      boolean blocked =
          blockedDomains.stream()
              .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
      if (blocked) {
        throw new PdfGenerationException(
            String.format("Domain '%s' is in the blocked domains list", host));
      }
    }
  }

  /**
   * Validates the URL against SSRF attacks by checking for private/internal IP addresses.
   *
   * @param url the parsed URL
   * @throws PdfGenerationException if the URL points to a private or internal address
   */
  private void validateAgainstSsrf(URL url) {
    String host = url.getHost();

    // Check for localhost
    if (isLocalhost(host)) {
      throw new PdfGenerationException(
          "Access to localhost is not allowed for security reasons. Host: " + host);
    }

    // Resolve hostname to IP address
    InetAddress address;
    try {
      address = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      throw new PdfGenerationException("Unable to resolve host: " + host, e);
    }

    // Check if it's a private IP
    if (isPrivateIpAddress(address)) {
      throw new PdfGenerationException(
          String.format(
              "Access to private IP address '%s' (resolved from '%s') is not allowed for security"
                  + " reasons",
              address.getHostAddress(), host));
    }

    // Check for loopback address
    if (address.isLoopbackAddress()) {
      throw new PdfGenerationException(
          String.format(
              "Access to loopback address '%s' is not allowed for security reasons",
              address.getHostAddress()));
    }

    // Check for link-local address
    if (address.isLinkLocalAddress()) {
      throw new PdfGenerationException(
          String.format(
              "Access to link-local address '%s' is not allowed for security reasons",
              address.getHostAddress()));
    }

    // Check for site-local (deprecated but still check)
    if (address.isSiteLocalAddress()) {
      throw new PdfGenerationException(
          String.format(
              "Access to site-local address '%s' is not allowed for security reasons",
              address.getHostAddress()));
    }
  }

  /**
   * Checks if the host is localhost or a loopback address.
   *
   * @param host the hostname to check
   * @return true if the host is localhost
   */
  private boolean isLocalhost(String host) {
    String lowerHost = host.toLowerCase();
    return lowerHost.equals("localhost")
        || lowerHost.equals("127.0.0.1")
        || lowerHost.equals("[::1]")
        || lowerHost.equals("::1");
  }

  /**
   * Checks if an IP address is in a private range.
   *
   * @param address the IP address to check
   * @return true if the address is private
   */
  private boolean isPrivateIpAddress(InetAddress address) {
    String ip = address.getHostAddress();

    // Check IPv4 private ranges
    if (ip.startsWith(PRIVATE_IP_10)) { // 10.0.0.0/8
      return true;
    }
    if (ip.startsWith(PRIVATE_IP_192)) { // 192.168.0.0/16
      return true;
    }
    if (ip.startsWith(LINK_LOCAL_169)) { // 169.254.0.0/16 (link-local)
      return true;
    }

    // Check 172.16.0.0/12 range (172.16.0.0 - 172.31.255.255)
    if (ip.startsWith("172.")) {
      String[] parts = ip.split("\\.");
      if (parts.length >= 2) {
        try {
          int secondOctet = Integer.parseInt(parts[1]);
          if (secondOctet >= 16 && secondOctet <= 31) {
            return true;
          }
        } catch (NumberFormatException e) {
          // Invalid IP format, will be caught elsewhere
        }
      }
    }

    // Check IPv6 unique local addresses (fc00::/7)
    if (ip.toLowerCase().startsWith("fc") || ip.toLowerCase().startsWith("fd")) {
      return true;
    }

    // Check IPv6 link-local (fe80::/10)
    if (ip.toLowerCase().startsWith(IPV6_LINK_LOCAL_PREFIX)) {
      return true;
    }

    return false;
  }

  /**
   * Builder for creating customized URL validators.
   *
   * @since 1.0.0
   */
  public static class Builder {
    private String url;
    private boolean allowPrivateIps = false;
    private Set<String> allowedDomains;
    private Set<String> blockedDomains;

    /**
     * Sets the URL to validate.
     *
     * @param url the URL string
     * @return this builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Configures whether to allow private IP addresses.
     *
     * <p><strong>Warning:</strong> Allowing private IPs can expose internal network resources.
     * Only enable this in controlled environments where SSRF is not a concern.
     *
     * @param allowPrivateIps true to allow private IPs, false to block them
     * @return this builder
     */
    public Builder allowPrivateIps(boolean allowPrivateIps) {
      this.allowPrivateIps = allowPrivateIps;
      return this;
    }

    /**
     * Sets a whitelist of allowed domains.
     *
     * <p>If configured, only URLs from these domains will be allowed. Supports subdomain matching
     * (e.g., "example.com" matches "www.example.com").
     *
     * @param allowedDomains set of allowed domain names
     * @return this builder
     */
    public Builder allowedDomains(Set<String> allowedDomains) {
      this.allowedDomains = allowedDomains;
      return this;
    }

    /**
     * Sets a blacklist of blocked domains.
     *
     * <p>URLs from these domains will be rejected. Supports subdomain matching.
     *
     * @param blockedDomains set of blocked domain names
     * @return this builder
     */
    public Builder blockedDomains(Set<String> blockedDomains) {
      this.blockedDomains = blockedDomains;
      return this;
    }

    /**
     * Builds the validator and performs validation.
     *
     * @throws PdfGenerationException if validation fails
     */
    public void validate() {
      new UrlValidator(this).validate();
    }
  }
}
