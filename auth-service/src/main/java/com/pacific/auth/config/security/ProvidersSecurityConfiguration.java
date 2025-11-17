package com.pacific.auth.config.security;

import com.pacific.auth.modules.authentication.security.filters.JwtAuthenticationFilter;
import com.pacific.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationProvider;
import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationProvider;
import com.pacific.auth.modules.user.service.UserDetailsServiceImpl;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Providers security configuration for auth-service. Consolidates all security concerns and
 * authentication providers into a single, clean configuration. This is the main security
 * configuration that: - Enables web security - Enables method security - Configures CORS - Sets up
 * security filter chain with JWT authentication - Manages authentication providers (DAO, Custom
 * JWT, Keycloak) - Configures authorization rules
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class ProvidersSecurityConfiguration {

  private final UserDetailsServiceImpl userDetailsService;
  private final CorsConfigurationSource corsConfigurationSource;
  private final PasswordEncoder passwordEncoder;
  private final SecurityEndpointsProperties securityEndpointsProperties;

  // JWT Authentication Providers (optional - only loaded when needed)
  // Note: Using @Lazy to break circular dependencies

  @Autowired private ApplicationContext applicationContext;

  @Value("${auth-service.security.authentication.mode:custom}")
  private String authMode;

  public ProvidersSecurityConfiguration(
      UserDetailsServiceImpl userDetailsService,
      CorsConfigurationSource corsConfigurationSource,
      PasswordEncoder passwordEncoder,
      SecurityEndpointsProperties securityEndpointsProperties) {
    this.userDetailsService = userDetailsService;
    this.corsConfigurationSource = corsConfigurationSource;
    this.passwordEncoder = passwordEncoder;
    this.securityEndpointsProperties = securityEndpointsProperties;
  }

  /** Unified Security Filter Chain for auth-service */
  @Bean
  @Primary
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    log.info("🔐 Configuring unified security filter chain for auth-service");

    // Configure CORS using injected configuration
    http.cors(cors -> cors.configurationSource(corsConfigurationSource));

    // Disable CSRF for stateless JWT authentication
    http.csrf(AbstractHttpConfigurer::disable);

    // Configure session management
    http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            headers ->
                headers
                    .frameOptions(frameOptions -> frameOptions.deny())
                    .contentTypeOptions(contentTypeOptions -> contentTypeOptions.and())
                    .httpStrictTransportSecurity(hstsConfig -> hstsConfig.maxAgeInSeconds(31536000))
                    .addHeaderWriter(
                        (request, response) -> {
                          response.setHeader("X-Content-Type-Options", "nosniff");
                          response.setHeader("X-Frame-Options", "DENY");
                          response.setHeader("X-XSS-Protection", "1; mode=block");
                          response.setHeader(
                              "Cache-Control", "no-cache, no-store, must-revalidate");
                          response.setHeader("Pragma", "no-cache");
                          response.setHeader("Expires", "0");
                        }));

    // Configure exception handling with custom entry point
    http.exceptionHandling(
        exceptions ->
            exceptions.authenticationEntryPoint(new CustomBearerTokenAuthenticationEntryPoint()));

    // Add JWT authentication filter if available
    try {
      JwtAuthenticationFilter jwtAuthenticationFilter =
          applicationContext.getBean(JwtAuthenticationFilter.class);

      // Set the authentication manager in the filter
      jwtAuthenticationFilter.setAuthenticationManager(authenticationManager());

      http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
      log.info("✅ JWT authentication filter added with authentication manager");
    } catch (Exception e) {
      log.warn("⚠️ JWT authentication filter not available, using basic authentication only");
    }

    // Configure authorization
    String[] publicEndpoints = securityEndpointsProperties.publicEndpointsArray();
    String[] keycloakEndpoints = securityEndpointsProperties.keycloakEndpointsArray();
    String[] adminEndpoints = securityEndpointsProperties.adminEndpointsArray();
    String[] userEndpoints = securityEndpointsProperties.userEndpointsArray();

    http.authorizeHttpRequests(
        authz -> {
          if (publicEndpoints.length > 0) {
            authz.requestMatchers(publicEndpoints).permitAll();
          }

          if (keycloakEndpoints.length > 0) {
            authz.requestMatchers(keycloakEndpoints).permitAll();
          }

          if (adminEndpoints.length > 0) {
            authz.requestMatchers(adminEndpoints).hasRole("ADMIN");
          }

          if (userEndpoints.length > 0) {
            authz
                .requestMatchers(userEndpoints)
                .hasAnyRole("USER", "ADMIN", "DEFAULT-ROLES-MASTER");
          }

          authz.anyRequest().authenticated();
        });

    log.info("✅ Unified security filter chain configured successfully");
    return http.build();
  }

  /** Unified Authentication Manager */
  @Bean
  @Primary
  public AuthenticationManager authenticationManager() {
    log.info("🔐 Configuring unified authentication manager for auth-service");

    // Create basic DAO authentication provider
    DaoAuthenticationProvider daoAuthProvider = new DaoAuthenticationProvider();
    daoAuthProvider.setUserDetailsService(userDetailsService);
    daoAuthProvider.setPasswordEncoder(passwordEncoder);

    // Build provider list with available providers
    List<org.springframework.security.authentication.AuthenticationProvider> providers =
        new ArrayList<>();
    providers.add(daoAuthProvider);
    switch (authMode.toLowerCase()) {
      case "custom":
        // Add JWT providers if available (get from application context after initialization)
        try {
          CustomJwtAuthenticationProvider customJwtProvider =
              applicationContext.getBean(
                  "customJwtAuthenticationProvider", CustomJwtAuthenticationProvider.class);
          providers.add(customJwtProvider);
          log.info("✅ CustomJwtAuthenticationProvider added");
        } catch (Exception e) {
          log.debug("CustomJwtAuthenticationProvider not available: {}", e.getMessage());
        }
      case "keycloak":
        try {
          KeycloakJwtAuthenticationProvider keycloakJwtProvider =
              applicationContext.getBean(KeycloakJwtAuthenticationProvider.class);
          providers.add(keycloakJwtProvider);
          log.info("✅ KeycloakJwtAuthenticationProvider added");
        } catch (Exception e) {
          log.debug("KeycloakJwtAuthenticationProvider not available: {}", e.getMessage());
        }
    }

    AuthenticationManager manager = new ProviderManager(providers);
    log.info("✅ Unified authentication manager configured with {} providers", providers.size());

    return manager;
  }

  /** Provide UserDetailsService for the base security configuration */
  @Bean
  @Primary
  public UserDetailsService userDetailsService() {
    return userDetailsService;
  }
}
