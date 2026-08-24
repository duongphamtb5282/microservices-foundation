package com.pacific.auth.modules.authentication.security.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pacific.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationProvider;
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
 * Regression test: the provider chain must run exactly once per request. The old implementation
 * duplicated the fallback chain (authenticateToken + tryFallbackProviders), invoking each provider
 * twice per failed attempt.
 */
class JwtAuthenticationFilterRoutingTest {

  private CustomJwtAuthenticationProvider customProvider;
  private KeycloakJwtAuthenticationProvider keycloakProvider;
  private AuthenticationManager authenticationManager;
  private JwtAuthenticationFilterRouting filter;

  @BeforeEach
  void setUp() {
    customProvider = mock(CustomJwtAuthenticationProvider.class);
    keycloakProvider = mock(KeycloakJwtAuthenticationProvider.class);
    authenticationManager = mock(AuthenticationManager.class);
    filter = new JwtAuthenticationFilterRouting(customProvider, keycloakProvider);
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
    verify(customProvider, never()).authenticate(any());
    verify(keycloakProvider, never()).authenticate(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void managerSuccess_returnsAuthenticationWithoutTryingProviders() throws Exception {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    when(authenticationManager.authenticate(any())).thenReturn(auth);

    filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(auth);
    verify(customProvider, never()).authenticate(any());
    verify(keycloakProvider, never()).authenticate(any());
  }

  @Test
  void managerFails_triesEachProviderExactlyOnce() throws Exception {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
    when(customProvider.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    Authentication keycloakAuth = mock(Authentication.class);
    when(keycloakAuth.isAuthenticated()).thenReturn(true);
    when(keycloakProvider.authenticate(any())).thenReturn(keycloakAuth);

    filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

    // Single chain: each fallback provider is consulted exactly once, never re-run.
    verify(authenticationManager, times(1)).authenticate(any());
    verify(customProvider, times(1)).authenticate(any());
    verify(keycloakProvider, times(1)).authenticate(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(keycloakAuth);
  }

  @Test
  void customProviderSuccess_withoutManager() throws Exception {
    filter.setAuthenticationManager(null);

    Authentication customAuth = mock(Authentication.class);
    when(customAuth.isAuthenticated()).thenReturn(true);
    when(customProvider.authenticate(any())).thenReturn(customAuth);

    filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(customAuth);
    verify(keycloakProvider, never()).authenticate(any());
  }

  @Test
  void allProvidersFail_continuesChainUnauthenticated() throws Exception {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
    when(customProvider.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
    when(keycloakProvider.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    MockHttpServletRequest request = requestWithToken();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(authenticationManager, times(1)).authenticate(any());
    verify(customProvider, times(1)).authenticate(any());
    verify(keycloakProvider, times(1)).authenticate(any());
  }

  private MockHttpServletRequest requestWithToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer test.jwt.token");
    return request;
  }
}
