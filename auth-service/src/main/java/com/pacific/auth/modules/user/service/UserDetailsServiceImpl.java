package com.pacific.auth.modules.user.service;

import com.pacific.auth.modules.role.entity.RoleType;
import com.pacific.auth.modules.role.service.RoleService;
import com.pacific.auth.modules.user.entity.User;
import com.pacific.auth.modules.user.mapper.UserDtoMapper;
import com.pacific.auth.modules.user.repository.UserRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;
  private final UserDtoMapper userDtoMapper;
  private final RoleService roleService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("Loading user by username: {}", username);

    // Load user without roles to avoid circular references
    User user =
        userRepository
            .findByUserNameWithoutRoles(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));

    // Load roles separately to avoid circular references
    Set<RoleType> roleNames =
        roleService.findByUserId(user.getId()).stream()
            .map(role -> role.getName())
            .collect(Collectors.toSet());

    log.debug("User {} loaded with {} roles: {}", username, roleNames.size(), roleNames);

    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUserName())
        .password(user.getPassword())
        .authorities(getAuthorities(roleNames))
        .build();
  }

  /**
   * Convert role names to Spring Security authorities
   *
   * @param roleNames the user role names
   * @return collection of granted authorities
   */
  private Collection<GrantedAuthority> getAuthorities(Set<RoleType> roleNames) {
    if (roleNames == null || roleNames.isEmpty()) {
      log.warn("No roles found for user, assigning default ROLE_USER");
      return Collections.singletonList(new SimpleGrantedAuthority(RoleType.USER.getAuthority()));
    }

    return roleNames.stream()
        .map(roleType -> new SimpleGrantedAuthority(roleType.getAuthority()))
        .collect(Collectors.toSet());
  }
}
