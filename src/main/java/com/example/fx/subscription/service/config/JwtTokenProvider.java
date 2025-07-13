package com.example.fx.subscription.service.config;

import com.example.fx.subscription.service.service.FxUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;

@Component
public class JwtTokenProvider {
  private final static Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);

  @Value("${security.jwt.token.secret-key}")
  private String secret;

  @Value("${security.jwt.token.expire-length}")
  private long validityInMilliseconds;

  private SecretKey secretKey;

  private final FxUserDetailsService fxUserDetailsService;

  public JwtTokenProvider(FxUserDetailsService fxUserDetailsService) {
    this.fxUserDetailsService = fxUserDetailsService;
  }

  @PostConstruct
  protected void init() {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public Authentication getAuthentication(String token) {
    String username = getUsername(token);
    UserDetails userDetails = fxUserDetailsService.loadUserByUsername(username);
    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  public String createToken(String username, Set<String> roles) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validityInMilliseconds);
    return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
  }

  public String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      LOGGER.error("JWT validation failed: {}", e.getMessage());
      return false;
    }
  }

  public String getUsername(String token) {
    Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    return claims.getSubject();
  }
}

