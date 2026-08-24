package com.pacific.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Fetches and caches the Keycloak signing keys (JWKS) used to verify Bearer tokens locally.
 *
 * <p>The JWKS are fetched once at startup and refreshed every {@code gateway.jwks-refresh-interval}
 * (default 15 minutes). A failed fetch never crashes the gateway: the failure is logged, any
 * previously cached keys are kept, and the next scheduled refresh retries. If no keys are available
 * when a token arrives, request-time validation fails with a 401.
 */
@Service
@Slf4j
public class KeycloakJwksService {

  private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(5);

  // NOTE: the default realm here must match the token-issuing realm (auth-service). The YAML
  // block at gateway.keycloak (application.yml) documents this same value but is not bound —
  // the annotation-level default is the live config when GATEWAY_JWKS_URL is unset.
  @Value("${GATEWAY_JWKS_URL:http://localhost:8080/realms/auth-service/protocol/openid-connect/certs}")
  private String jwksUrl;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final ConcurrentMap<String, RSAPublicKey> keysByKid = new ConcurrentHashMap<>();

  public KeycloakJwksService(ObjectMapper objectMapper) {
    this.webClient = WebClient.builder().build();
    this.objectMapper = objectMapper;
  }

  /** Initial fetch at startup. Tolerant of Keycloak being unavailable. */
  @PostConstruct
  public void initialize() {
    refreshKeys();
  }

  /**
   * Periodic refresh of the cached JWKS keys. Never throws; logs and keeps stale keys on failure.
   */
  @Scheduled(fixedDelayString = "${gateway.jwks-refresh-interval:PT15M}")
  public void refreshKeys() {
    try {
      String jwksJson =
          webClient
              .get()
              .uri(jwksUrl)
              .retrieve()
              .bodyToMono(String.class)
              .timeout(FETCH_TIMEOUT)
              .block();
      if (jwksJson == null || jwksJson.isBlank()) {
        log.warn("JWKS fetch from {} returned an empty response", jwksUrl);
        return;
      }

      JsonNode root = objectMapper.readTree(jwksJson);
      JsonNode keys = root.path("keys");
      if (!keys.isArray() || keys.isEmpty()) {
        log.warn("JWKS response from {} contains no keys", jwksUrl);
        return;
      }

      ConcurrentMap<String, RSAPublicKey> freshKeys = new ConcurrentHashMap<>();
      for (JsonNode keyNode : keys) {
        String kid = keyNode.path("kid").asText(null);
        String kty = keyNode.path("kty").asText(null);
        String modulus = keyNode.path("n").asText(null);
        String exponent = keyNode.path("e").asText(null);
        if (kid == null || !"RSA".equals(kty) || modulus == null || exponent == null) {
          continue;
        }
        RSAPublicKey publicKey = buildRsaPublicKey(modulus, exponent);
        if (publicKey != null) {
          freshKeys.put(kid, publicKey);
        }
      }

      if (freshKeys.isEmpty()) {
        log.warn("JWKS fetch from {} completed but no RSA keys could be parsed", jwksUrl);
        return;
      }

      keysByKid.clear();
      keysByKid.putAll(freshKeys);
      log.info("Refreshed Keycloak JWKS from {}: {} RSA keys cached", jwksUrl, freshKeys.size());
    } catch (Exception e) {
      log.warn(
          "Failed to fetch JWKS from {}: {} - keeping previously cached keys if any",
          jwksUrl,
          e.getMessage());
    }
  }

  /**
   * Returns the cached RSA public key for the given key id, or {@code null} if the key id is
   * unknown or no keys have been fetched yet.
   */
  public RSAPublicKey getKey(String kid) {
    return kid == null ? null : keysByKid.get(kid);
  }

  /** Returns {@code true} if at least one signing key is currently cached. */
  public boolean hasKeys() {
    return !keysByKid.isEmpty();
  }

  private RSAPublicKey buildRsaPublicKey(String modulus, String exponent) {
    try {
      BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
      BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
      RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
      return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    } catch (Exception ex) {
      log.warn("Failed to construct RSA public key from JWKS entry: {}", ex.getMessage());
      return null;
    }
  }
}
