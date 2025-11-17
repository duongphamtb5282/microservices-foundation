#!/usr/bin/env python3
"""
Script to update all import statements after migration
"""
import os
import re
from pathlib import Path

BASE_PATH = Path("/Users/duongphamthaibinh/Downloads/SourceCode/design/beautiful/java/microservices/auth-service/src/main/java")

# Import mappings: old package -> new package
IMPORT_MAPPINGS = {
    # Entities
    "com.demo.auth.entity.User": "com.demo.auth.modules.user.entity.User",
    "com.demo.auth.entity.Role": "com.demo.auth.modules.role.entity.Role",
    "com.demo.auth.entity.RoleType": "com.demo.auth.modules.role.entity.RoleType",
    "com.demo.auth.entity.Permission": "com.demo.auth.modules.permission.entity.Permission",
    "com.demo.auth.entity.RolePermission": "com.demo.auth.modules.permission.entity.RolePermission",
    
    # Repositories
    "com.demo.auth.repository.UserRepository": "com.demo.auth.modules.user.repository.UserRepository",
    "com.demo.auth.repository.RoleRepository": "com.demo.auth.modules.role.repository.RoleRepository",
    
    # User Services
    "com.demo.auth.service.UserService": "com.demo.auth.modules.user.service.UserService",
    "com.demo.auth.service.UserDetailsServiceImpl": "com.demo.auth.modules.user.service.UserDetailsServiceImpl",
    "com.demo.auth.service.UserRegistrationService": "com.demo.auth.modules.user.service.UserRegistrationService",
    
    # Authentication Services
    "com.demo.auth.service.AuthenticationService": "com.demo.auth.modules.authentication.service.AuthenticationService",
    "com.demo.auth.service.JwtService": "com.demo.auth.modules.authentication.service.JwtService",
    "com.demo.auth.service.StatelessRefreshTokenService": "com.demo.auth.modules.authentication.service.StatelessRefreshTokenService",
    
    # Role Service
    "com.demo.auth.service.RoleService": "com.demo.auth.modules.role.service.RoleService",
    
    # User Controllers
    "com.demo.auth.controller.UserController": "com.demo.auth.modules.user.controller.UserController",
    "com.demo.auth.controller.RegistrationController": "com.demo.auth.modules.user.controller.RegistrationController",
    
    # Authentication Controllers
    "com.demo.auth.controller.AuthController": "com.demo.auth.modules.authentication.controller.AuthenticationController",
    
    # User DTOs
    "com.demo.auth.service.dto.UserInfoDto": "com.demo.auth.modules.user.dto.response.UserInfoDto",
    "com.demo.auth.dto.UserDTO": "com.demo.auth.modules.user.dto.response.UserDTO",
    "com.demo.auth.dto.RegistrationRequestDto": "com.demo.auth.modules.user.dto.request.RegistrationRequestDto",
    "com.demo.auth.dto.RegistrationResponseDto": "com.demo.auth.modules.user.dto.response.RegistrationResponseDto",
    
    # Authentication DTOs
    "com.demo.auth.service.dto.RefreshTokenRequestDto": "com.demo.auth.modules.authentication.dto.request.RefreshTokenRequestDto",
    "com.demo.auth.dto.LoginRequestDto": "com.demo.auth.modules.authentication.dto.request.LoginRequestDto",
    "com.demo.auth.dto.AuthenticationRequestDto": "com.demo.auth.modules.authentication.dto.request.AuthenticationRequestDto",
    "com.demo.auth.dto.AuthenticationResponseDto": "com.demo.auth.modules.authentication.dto.response.AuthenticationResponseDto",
    
    # Mappers
    "com.demo.auth.mapper.UserMapper": "com.demo.auth.modules.user.mapper.UserMapper",
    "com.demo.auth.mapper.UserDtoMapper": "com.demo.auth.modules.user.mapper.UserDtoMapper",
    
    # Security - Custom JWT
    "com.demo.auth.security.custom.CustomJwtConfig": "com.demo.auth.modules.authentication.security.jwt.custom.CustomJwtConfig",
    "com.demo.auth.security.custom.CustomJwtAuthenticationProvider": "com.demo.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationProvider",
    "com.demo.auth.security.custom.CustomJwtAuthenticationToken": "com.demo.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationToken",
    "com.demo.auth.security.custom.CustomJwtValidationService": "com.demo.auth.modules.authentication.security.jwt.custom.CustomJwtValidationService",
    
    # Security - Keycloak JWT
    "com.demo.auth.security.keycloak.KeycloakJwtConfig": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtConfig",
    "com.demo.auth.security.keycloak.KeycloakJwtAuthenticationProvider": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationProvider",
    "com.demo.auth.security.keycloak.KeycloakJwtAuthenticationToken": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationToken",
    "com.demo.auth.security.keycloak.KeycloakJwtAuthenticationConverter": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationConverter",
    "com.demo.auth.security.keycloak.KeycloakTokenValidationService": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakTokenValidationService",
    "com.demo.auth.security.keycloak.KeycloakRoleConverter": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakRoleConverter",
    "com.demo.auth.security.keycloak.KeycloakProperties": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakProperties",
    "com.demo.auth.security.keycloak.KeycloakSecurityAuditLogger": "com.demo.auth.modules.authentication.security.jwt.keycloak.KeycloakSecurityAuditLogger",
    
    # Security - Common JWT
    "com.demo.auth.security.common.AbstractJwtAuthenticationProvider": "com.demo.auth.modules.authentication.security.jwt.common.AbstractJwtAuthenticationProvider",
    "com.demo.auth.security.common.AbstractJwtValidationService": "com.demo.auth.modules.authentication.security.jwt.common.AbstractJwtValidationService",
    "com.demo.auth.security.common.JwtAuthenticationToken": "com.demo.auth.modules.authentication.security.jwt.common.JwtAuthenticationToken",
    "com.demo.auth.security.common.JwtTokenValidationService": "com.demo.auth.modules.authentication.security.jwt.common.JwtTokenValidationService",
    "com.demo.auth.security.common.JwtValidationResult": "com.demo.auth.modules.authentication.security.jwt.common.JwtValidationResult",
    "com.demo.auth.security.common.TokenType": "com.demo.auth.modules.authentication.security.jwt.common.TokenType",
    
    # Security - Filters
    "com.demo.auth.security.filters.JwtAuthenticationFilter": "com.demo.auth.modules.authentication.security.filters.JwtAuthenticationFilter",
    "com.demo.auth.security.filters.JwtAuthenticationFilterRouting": "com.demo.auth.modules.authentication.security.filters.JwtAuthenticationFilterRouting",
    
    # Common - Config
    "com.demo.auth.config.ProvidersSecurityConfiguration": "com.demo.auth.common.config.ProvidersSecurityConfiguration",
    "com.demo.auth.config.AuthCacheConfiguration": "com.demo.auth.common.config.AuthCacheConfiguration",
    "com.demo.auth.config.FallbackJwtConfig": "com.demo.auth.common.config.FallbackJwtConfig",
    "com.demo.auth.config.RetryConfiguration": "com.demo.auth.common.config.RetryConfiguration",
    "com.demo.auth.service.AuthDatabaseService": "com.demo.auth.common.config.AuthDatabaseService",
    "com.demo.auth.service.AuthSecurityService": "com.demo.auth.common.config.AuthSecurityService",
    "com.demo.auth.controller.CacheController": "com.demo.auth.common.config.CacheController",
    
    # Common - Security
    "com.demo.auth.security.SecurityConfigurationFactory": "com.demo.auth.common.security.SecurityConfigurationFactory",
    "com.demo.auth.security.SecurityProperties": "com.demo.auth.common.security.SecurityProperties",
    "com.demo.auth.security.PasswordEncoderConfig": "com.demo.auth.common.security.PasswordEncoderConfig",
    "com.demo.auth.security.RateLimitingConfig": "com.demo.auth.common.security.RateLimitingConfig",
    "com.demo.auth.security.SecurityConfiguration": "com.demo.auth.common.security.SecurityConfiguration",
    "com.demo.auth.security.CustomBearerTokenAuthenticationEntryPoint": "com.demo.auth.common.security.CustomBearerTokenAuthenticationEntryPoint",
    
    # Common - Cache
    "com.demo.auth.service.AuthCacheService": "com.demo.auth.common.cache.AuthCacheService",
}

def update_imports_in_file(file_path):
    """Update all import statements in a Java file"""
    try:
        content = file_path.read_text()
        original_content = content
        
        # Update each import
        for old_import, new_import in IMPORT_MAPPINGS.items():
            # Match both regular and wildcard imports
            old_pattern = f"import\\s+{re.escape(old_import)};"
            new_statement = f"import {new_import};"
            content = re.sub(old_pattern, new_statement, content)
            
            # Also update wildcard imports
            old_package = old_import.rsplit('.', 1)[0]
            new_package = new_import.rsplit('.', 1)[0]
            old_wildcard = f"import\\s+{re.escape(old_package)}\\.\\*;"
            new_wildcard = f"import {new_package}.*;"
            content = re.sub(old_wildcard, new_wildcard, content)
        
        # Write back if changed
        if content != original_content:
            file_path.write_text(content)
            return True
        return False
    except Exception as e:
        print(f"   ❌ Error updating {file_path}: {e}")
        return False

def main():
    print("🔄 Updating import statements...")
    
    # Find all Java files
    java_files = list(BASE_PATH.glob("**/*.java"))
    print(f"📁 Found {len(java_files)} Java files\n")
    
    updated_count = 0
    for java_file in java_files:
        rel_path = java_file.relative_to(BASE_PATH)
        if update_imports_in_file(java_file):
            print(f"✅ Updated: {rel_path}")
            updated_count += 1
    
    print(f"\n✅ Updated imports in {updated_count} files")
    print("🎉 Import update complete!")

if __name__ == "__main__":
    main()

