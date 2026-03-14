package com.example.fx.subscription.service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpToHttpsRedirectFilterTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain chain;

  private HttpToHttpsRedirectFilter filter;

  @BeforeEach
  void setup() {
    filter = new HttpToHttpsRedirectFilter(8080, 8443);
  }

  @Test
  void whenInsecureAndOnHttpPort_shouldRedirect() throws IOException, ServletException {
    when(request.isSecure()).thenReturn(false);
    when(request.getServerPort()).thenReturn(8080);
    when(request.getServerName()).thenReturn("localhost");
    when(request.getRequestURI()).thenReturn("/api/v1/secure");

    filter.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_PERMANENT_REDIRECT);
    verify(response).setHeader("Location", "https://localhost:8443/api/v1/secure");
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void whenInsecureAndOnHttpPortWithQueryString_shouldRedirect() throws IOException, ServletException {
    when(request.isSecure()).thenReturn(false);
    when(request.getServerPort()).thenReturn(8080);
    when(request.getServerName()).thenReturn("localhost");
    when(request.getRequestURI()).thenReturn("/api/v1/secure");
    when(request.getQueryString()).thenReturn("param=value");

    filter.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_PERMANENT_REDIRECT);
    verify(response).setHeader("Location", "https://localhost:8443/api/v1/secure?param=value");
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void whenQueryStringContainsCRLF_shouldPercentEncode() throws IOException, ServletException {
    when(request.isSecure()).thenReturn(false);
    when(request.getServerPort()).thenReturn(8080);
    when(request.getServerName()).thenReturn("localhost");
    when(request.getRequestURI()).thenReturn("/api/v1/secure");
    // Attacker tries to inject a new header via CRLF
    when(request.getQueryString()).thenReturn("q=1\r\nInjected-Header: true");

    filter.doFilter(request, response, chain);

    verify(response).setStatus(HttpServletResponse.SC_PERMANENT_REDIRECT);
    // The URI class should escape \r and \n into %0D and %0A
    verify(response).setHeader("Location", "https://localhost:8443/api/v1/secure?q=1%0D%0AInjected-Header:%20true");
  }

  @Test
  void whenUriComponentsAreInvalid_shouldReturn400BadRequest() throws IOException, ServletException {
    // Setup: Force an insecure request on the correct port
    when(request.isSecure()).thenReturn(false);
    when(request.getServerPort()).thenReturn(8080);

    // An opening square bracket '[' is illegal in a URI host name
    // and will cause the new URI(...) constructor to throw a URISyntaxException.
    when(request.getServerName()).thenReturn("[invalid_host]");
    when(request.getRequestURI()).thenReturn("/api");

    filter.doFilter(request, response, chain);

    // Verify: The catch block should trigger a 400 error
    verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), contains("Invalid URI components"));

    // Ensure no redirect or chain continuation happened
    verify(response, never()).setStatus(308);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void whenSecureOrNotOnHttpPort_shouldPassThrough() throws IOException, ServletException {
    // Test secure (HTTPS) request
    when(request.isSecure()).thenReturn(true);
    when(request.getServerPort()).thenReturn(8443);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(response, never()).setStatus(HttpServletResponse.SC_PERMANENT_REDIRECT);
    verify(response, never()).setHeader(eq("Location"), anyString());

    // Test insecure on wrong port
    reset(request, response, chain);
    when(request.isSecure()).thenReturn(false);
    when(request.getServerPort()).thenReturn(9000);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(response, never()).setStatus(308);
    verify(response, never()).setHeader(eq("Location"), anyString());
  }

}
