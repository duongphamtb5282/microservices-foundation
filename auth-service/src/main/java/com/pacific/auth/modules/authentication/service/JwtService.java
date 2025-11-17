package com.pacific.auth.modules.authentication.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** JWT Service for token generation and validation */
@Service
@Slf4j
public class JwtService {

  @Value(
      "${auth-service.security.authentication.jwt.secret:mySecretKeyThatIsAtLeast256BitsLongForHS256Algorithm}")
  private String secret;

  @Value("${auth-service.security.authentication.jwt.access-token-ttl:1h}")
  private String accessTokenTtl = "1h"; // Default fallback

  @Value("${auth-service.security.authentication.jwt.issuer}")
  private String issuer;

  /** Generate JWT token for user */
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    return createToken(claims, userDetails.getUsername());
  }

  /** Generate JWT token with custom claims */
  public String generateToken(Map<String, Object> claims, String subject) {
    return createToken(claims, subject);
  }

  /** Generate access token */
  public String generateAccessToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "access");
    return createToken(claims, username);
  }

  /** Generate access token with user roles */
  public String generateAccessToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "access");

    // Add user roles/authorities to the JWT token
    if (userDetails != null && userDetails.getAuthorities() != null) {
      List<String> roles =
          userDetails.getAuthorities().stream()
              .map(authority -> authority.getAuthority())
              .collect(java.util.stream.Collectors.toList());
      claims.put("roles", roles);
      log.info("🔐 Adding roles to JWT token: {}", roles);
    }

    return createToken(claims, userDetails.getUsername());
  }

  /** Generate refresh token */
  public String generateRefreshToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    return createToken(claims, username);
  }

  /** Create JWT token */
  private String createToken(Map<String, Object> claims, String subject) {
    log.info("🔐 Generating JWT token for subject: {}", subject);
    log.info("🔐 Access token TTL: {}", accessTokenTtl);

    Date now = new Date();
    long expirationMillis = parseDuration(accessTokenTtl);
    log.info("🔐 Parsed expiration milliseconds: {}", expirationMillis);
    Date expiryDate = new Date(now.getTime() + expirationMillis);

    String token =
        Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuer(issuer) // Use configurable issuer
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();

    log.info("✅ JWT token generated successfully");
    return token;
  }

  private long parseDuration(String duration) {
    if (duration == null || duration.isEmpty()) {
      log.warn("⚠️ Duration is null or empty, using default 1 hour");
      return 60 * 60 * 1000; // 1 hour default
    }

    if (duration.endsWith("h")) {
      return Long.parseLong(duration.substring(0, duration.length() - 1)) * 60 * 60 * 1000;
    } else if (duration.endsWith("m")) {
      return Long.parseLong(duration.substring(0, duration.length() - 1)) * 60 * 1000;
    } else if (duration.endsWith("s")) {
      return Long.parseLong(duration.substring(0, duration.length() - 1)) * 1000;
    } else {
      // Default to milliseconds if no unit specified
      return Long.parseLong(duration);
    }
  }

  /** Extract username from token */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /** Extract expiration date from token */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /** Extract specific claim from token */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /** Extract all claims from token */
  private Claims extractAllClaims(String token) {
    return Jwts.parser().setSigningKey(getSigningKey()).parseClaimsJws(token).getBody();
  }

  /** Check if token is expired */
  public Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /** Validate token */
  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  /** Get signing key */
  private SecretKey getSigningKey() {
    // Check if the secret is already base64 encoded
    try {
      byte[] keyBytes = Decoders.BASE64.decode(secret);
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (Exception e) {
      // If base64 decoding fails, treat as plain text and encode it
      log.info("🔐 Using plain text secret, encoding to base64");
      byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
      return Keys.hmacShaKeyFor(keyBytes);
    }
  }

  /** Get token expiration time in milliseconds */
  public Long getExpirationTime() {
    return Long.valueOf(parseDuration(accessTokenTtl));
  }
}
