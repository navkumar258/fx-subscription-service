package com.example.fx.subscription.service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@ConditionalOnProperty("server.http.port")
@Component
public class HttpToHttpsRedirectFilter implements Filter {

  private final int httpPort;
  private final int httpsPort;

  public HttpToHttpsRedirectFilter(
          @Value("${server.http.port}") int httpPort,
          @Value("${server.port}") int httpsPort
  ) {
    this.httpPort = httpPort;
    this.httpsPort = httpsPort;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (!httpRequest.isSecure() && httpRequest.getServerPort() == httpPort) {
      String serverName = httpRequest.getServerName();
      String uri = httpRequest.getRequestURI();
      String query = httpRequest.getQueryString();

      try {
        URI redirectUri = new URI(
                "https",
                null,
                serverName,
                httpsPort,
                uri,
                query,
                null
        );

        httpResponse.setStatus(HttpServletResponse.SC_PERMANENT_REDIRECT);
        httpResponse.setHeader("Location", redirectUri.toASCIIString());
      } catch (URISyntaxException _) {
        httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URI components");
      }
    } else {
      chain.doFilter(request, response);
    }
  }
}
