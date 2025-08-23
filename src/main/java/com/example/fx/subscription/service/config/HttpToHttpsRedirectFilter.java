package com.example.fx.subscription.service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
      String redirectURL = "https://" + httpRequest.getServerName() + ":" + httpsPort + httpRequest.getRequestURI();
      if (httpRequest.getQueryString() != null) {
        redirectURL += "?" + httpRequest.getQueryString();
      }
      httpResponse.setStatus(308);
      httpResponse.setHeader("Location", redirectURL);
    } else {
      chain.doFilter(request, response);
    }
  }
}
