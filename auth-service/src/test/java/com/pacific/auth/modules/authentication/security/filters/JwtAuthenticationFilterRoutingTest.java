package com.pacific.auth.modules.authentication.security.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Regression test: the authentication chain must run exactly once per request, and only the
 * Keycloak provider remains after the dual-mode stack was removed (S-06).
 */
class JwtAuthenticationFilterRoutingTest {

  private KeycloakJwtAuthenticationProvider keycloakProvider;
  private AuthenticationManager authenticationManager;
  private JwtAuthenticationFilterRouting filter;

  @BeforeEach
  void setUp() {
    keycloakProvider = mock(KeycloakJwtAuthenticationProvider.class);
    authenticationManager = mock(AuthenticationManager.class);
    filter = new JwtAuthenticationFilterRouting(keycloakProvider);
    filter.setAuthenticationManager(authenticationManager);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void noToken_continuesChainWithoutTryingAnyProvider() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isNotNull();
    verify(authenticationManager, never()).authenticate(any());
    verify(keycloakProvider, never()).authenticate(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void managerSuccess_returnsAuthenticationWithoutTryingProvider() throws Exception {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    when(authenticationManager.authenticate(any())).thenReturn(auth);

    filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(auth);
    verify(keycloakProvider, never()).authenticate(any());
  }

  @Test
  void managerFails_triesKeycloakProviderExactlyOnce() throws Exception {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    Authentication keycloakAuth = mock(Authentication.class);
    when(keycloakAuth.isAuthenticated()).thenReturn(true);
    when(keycloakProvider.authenticate(any())).thenReturn(keycloakAuth);

    filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

    // Single chain: each fallback provider is consulted exactly once, never re-run.
    verify(authenticationManager, times(1)).authenticate(any());
    verify(keycloakProvider, times(1)).authenticate(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(keycloakAuth);
  }

  @Test
  void keycloakProviderSuccess_withoutManager() throws Exception {
    filter.setAuthenticationManager(null);

    Authentication keycloakAuth = mock(Authentication.class);
    when(keycloakAuth.isAuthenticated()).thenReturn(true);
    when(keycloakProvider.authenticate(any())).thenReturn(keycloakAuth);

    filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(keycloakAuth);
    verify(keycloakProvider, times(1)).authenticate(any());
  }

  @Test
  void allProvidersFail_continuesChainUnauthenticated() throws Exception {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
    when(keycloakProvider.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    MockHttpServletRequest request = requestWithToken();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(authenticationManager, times(1)).authenticate(any());
    verify(keycloakProvider, times(1)).authenticate(any());
  }

  private MockHttpServletRequest requestWithToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer test.jwt.token");
    return request;
  }
}
