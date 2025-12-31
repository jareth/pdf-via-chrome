package com.fostermoore.pdfviachrome.util;

import static org.assertj.core.api.Assertions.*;

import com.fostermoore.pdfviachrome.exception.PdfGenerationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link UrlValidator}. */
class UrlValidatorTest {

  @Test
  void shouldAcceptValidHttpUrl() {
    assertThatCode(() -> UrlValidator.validate("http://example.com"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptValidHttpsUrl() {
    assertThatCode(() -> UrlValidator.validate("https://example.com"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptUrlWithPath() {
    assertThatCode(() -> UrlValidator.validate("https://example.com/path/to/page"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptUrlWithQueryParams() {
    assertThatCode(() -> UrlValidator.validate("https://example.com/search?q=test&lang=en"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptUrlWithFragment() {
    assertThatCode(() -> UrlValidator.validate("https://example.com/page#section"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptUrlWithPort() {
    assertThatCode(() -> UrlValidator.validate("https://example.com:8080/page"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectNullUrl() {
    assertThatThrownBy(() -> UrlValidator.validate(null))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("URL cannot be null or empty");
  }

  @Test
  void shouldRejectEmptyUrl() {
    assertThatThrownBy(() -> UrlValidator.validate(""))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("URL cannot be null or empty");
  }

  @Test
  void shouldRejectBlankUrl() {
    assertThatThrownBy(() -> UrlValidator.validate("   "))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("URL cannot be null or empty");
  }

  @Test
  void shouldRejectMalformedUrl() {
    assertThatThrownBy(() -> UrlValidator.validate("not a url"))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("Invalid URL syntax");
  }

  @ParameterizedTest
  @ValueSource(strings = {"file:///etc/passwd", "ftp://example.com", "gopher://example.com"})
  void shouldRejectNonHttpProtocols(String url) {
    assertThatThrownBy(() -> UrlValidator.validate(url))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("Protocol")
        .hasMessageContaining("not allowed");
  }

  @Test
  void shouldRejectLocalhostByName() {
    assertThatThrownBy(() -> UrlValidator.validate("http://localhost:8080"))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("localhost");
  }

  @Test
  void shouldRejectLocalhostByIp() {
    assertThatThrownBy(() -> UrlValidator.validate("http://127.0.0.1"))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("localhost");
  }

  @Test
  void shouldRejectLoopbackIp() {
    assertThatThrownBy(() -> UrlValidator.validate("http://127.1.2.3"))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("loopback");
  }

  @Test
  void shouldRejectIpv6Loopback() {
    assertThatThrownBy(() -> UrlValidator.validate("http://[::1]"))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("localhost");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "http://10.0.0.1",
      "http://10.255.255.255",
      "http://192.168.1.1",
      "http://192.168.0.100",
      "http://172.16.0.1",
      "http://172.31.255.255"
  })
  void shouldRejectPrivateIpv4Addresses(String url) {
    assertThatThrownBy(() -> UrlValidator.validate(url))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("private IP");
  }

  @Test
  void shouldRejectLinkLocalIpv4() {
    // Link-local addresses are rejected as private IPs
    assertThatThrownBy(() -> UrlValidator.validate("http://169.254.1.1"))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("private IP");
  }

  @Test
  void shouldAccept172OutsidePrivateRange() {
    // 172.15.x.x and 172.32.x.x are not in the private range (172.16-31.x.x)
    assertThatCode(() -> UrlValidator.validate("http://172.15.0.1"))
        .doesNotThrowAnyException();
    assertThatCode(() -> UrlValidator.validate("http://172.32.0.1"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowPrivateIpsWhenConfigured() {
    assertThatCode(() ->
        UrlValidator.builder()
            .url("http://192.168.1.1")
            .allowPrivateIps(true)
            .validate())
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowDomainsInWhitelist() {
    Set<String> allowedDomains = Set.of("example.com", "trusted.org");

    assertThatCode(() ->
        UrlValidator.builder()
            .url("https://example.com/page")
            .allowedDomains(allowedDomains)
            .validate())
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowSubdomainsInWhitelist() {
    Set<String> allowedDomains = Set.of("example.com");

    assertThatCode(() ->
        UrlValidator.builder()
            .url("https://www.example.com")
            .allowedDomains(allowedDomains)
            .validate())
        .doesNotThrowAnyException();

    assertThatCode(() ->
        UrlValidator.builder()
            .url("https://api.example.com")
            .allowedDomains(allowedDomains)
            .validate())
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectDomainsNotInWhitelist() {
    Set<String> allowedDomains = Set.of("example.com");

    assertThatThrownBy(() ->
        UrlValidator.builder()
            .url("https://other.com")
            .allowedDomains(allowedDomains)
            .validate())
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("not in the allowed domains list");
  }

  @Test
  void shouldRejectDomainsInBlacklist() {
    Set<String> blockedDomains = Set.of("malicious.com", "blocked.org");

    assertThatThrownBy(() ->
        UrlValidator.builder()
            .url("https://malicious.com")
            .blockedDomains(blockedDomains)
            .validate())
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("blocked domains list");
  }

  @Test
  void shouldRejectSubdomainsInBlacklist() {
    Set<String> blockedDomains = Set.of("malicious.com");

    assertThatThrownBy(() ->
        UrlValidator.builder()
            .url("https://www.malicious.com")
            .blockedDomains(blockedDomains)
            .validate())
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("blocked domains list");
  }

  @Test
  void shouldAllowDomainsNotInBlacklist() {
    Set<String> blockedDomains = Set.of("malicious.com");

    assertThatCode(() ->
        UrlValidator.builder()
            .url("https://example.com")
            .blockedDomains(blockedDomains)
            .validate())
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectUnresolvableDomain() {
    assertThatThrownBy(() -> UrlValidator.validate("http://this-domain-does-not-exist-12345.com"))
        .isInstanceOf(PdfGenerationException.class)
        .hasMessageContaining("Unable to resolve host");
  }
}
