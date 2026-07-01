package com.gengetau.qaflow.modules.auth;

import com.gengetau.qaflow.modules.users.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final String jwtSecret;
  private final long accessTokenMinutes;

  public JwtService(
      @Value("${qaflow.security.jwt-secret}") String jwtSecret,
      @Value("${qaflow.security.access-token-minutes}") long accessTokenMinutes) {
    this.jwtSecret = jwtSecret;
    this.accessTokenMinutes = accessTokenMinutes;
  }

  public String issueAccessToken(User user) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(expiresInSeconds());
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey())
        .compact();
  }

  public UUID parseUserId(String token) {
    try {
      String subject =
          Jwts.parser()
              .verifyWith(signingKey())
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();
      return UUID.fromString(subject);
    } catch (IllegalArgumentException | JwtException exception) {
      throw new InvalidTokenException("Invalid access token", exception);
    }
  }

  public long expiresInSeconds() {
    return accessTokenMinutes * 60;
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }
}