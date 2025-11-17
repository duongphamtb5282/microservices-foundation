#!/usr/bin/env python3
"""
Script to migrate auth-service to modular structure
"""
import os
import shutil
import re
from pathlib import Path

BASE_PATH = Path("/Users/duongphamthaibinh/Downloads/SourceCode/design/beautiful/java/microservices/auth-service/src/main/java/com/demo/auth")

# File movement mappings
MIGRATIONS = {
    # User Module
    "entity/User.java": "modules/user/entity/User.java",
    "repository/UserRepository.java": "modules/user/repository/UserRepository.java",
    "service/UserService.java": "modules/user/service/UserService.java",
    "service/UserDetailsServiceImpl.java": "modules/user/service/UserDetailsServiceImpl.java",
    "service/UserRegistrationService.java": "modules/user/service/UserRegistrationService.java",
    "controller/UserController.java": "modules/user/controller/UserController.java",
    "controller/RegistrationController.java": "modules/user/controller/RegistrationController.java",
    "mapper/UserMapper.java": "modules/user/mapper/UserMapper.java",
    "mapper/UserDtoMapper.java": "modules/user/mapper/UserDtoMapper.java",
    "service/dto/UserInfoDto.java": "modules/user/dto/response/UserInfoDto.java",
    "dto/UserDTO.java": "modules/user/dto/response/UserDTO.java",
    "dto/RegistrationRequestDto.java": "modules/user/dto/request/RegistrationRequestDto.java",
    "dto/RegistrationResponseDto.java": "modules/user/dto/response/RegistrationResponseDto.java",
    
    # Authentication Module
    "service/AuthenticationService.java": "modules/authentication/service/AuthenticationService.java",
    "service/JwtService.java": "modules/authentication/service/JwtService.java",
    "service/StatelessRefreshTokenService.java": "modules/authentication/service/StatelessRefreshTokenService.java",
    "controller/AuthController.java": "modules/authentication/controller/AuthenticationController.java",
    "dto/LoginRequestDto.java": "modules/authentication/dto/request/LoginRequestDto.java",
    "dto/AuthenticationRequestDto.java": "modules/authentication/dto/request/AuthenticationRequestDto.java",
    "dto/AuthenticationResponseDto.java": "modules/authentication/dto/response/AuthenticationResponseDto.java",
    "service/dto/RefreshTokenRequestDto.java": "modules/authentication/dto/request/RefreshTokenRequestDto.java",
    
    # Authentication Security
    "security/custom/CustomJwtConfig.java": "modules/authentication/security/jwt/custom/CustomJwtConfig.java",
    "security/custom/CustomJwtAuthenticationProvider.java": "modules/authentication/security/jwt/custom/CustomJwtAuthenticationProvider.java",
    "security/custom/CustomJwtAuthenticationToken.java": "modules/authentication/security/jwt/custom/CustomJwtAuthenticationToken.java",
    "security/custom/CustomJwtValidationService.java": "modules/authentication/security/jwt/custom/CustomJwtValidationService.java",
    
    "security/keycloak/KeycloakJwtConfig.java": "modules/authentication/security/jwt/keycloak/KeycloakJwtConfig.java",
    "security/keycloak/KeycloakJwtAuthenticationProvider.java": "modules/authentication/security/jwt/keycloak/KeycloakJwtAuthenticationProvider.java",
    "security/keycloak/KeycloakJwtAuthenticationToken.java": "modules/authentication/security/jwt/keycloak/KeycloakJwtAuthenticationToken.java",
    "security/keycloak/KeycloakJwtAuthenticationConverter.java": "modules/authentication/security/jwt/keycloak/KeycloakJwtAuthenticationConverter.java",
    "security/keycloak/KeycloakTokenValidationService.java": "modules/authentication/security/jwt/keycloak/KeycloakTokenValidationService.java",
    "security/keycloak/KeycloakRoleConverter.java": "modules/authentication/security/jwt/keycloak/KeycloakRoleConverter.java",
    "security/keycloak/KeycloakProperties.java": "modules/authentication/security/jwt/keycloak/KeycloakProperties.java",
    "security/keycloak/KeycloakSecurityAuditLogger.java": "modules/authentication/security/jwt/keycloak/KeycloakSecurityAuditLogger.java",
    
    "security/common/AbstractJwtAuthenticationProvider.java": "modules/authentication/security/jwt/common/AbstractJwtAuthenticationProvider.java",
    "security/common/AbstractJwtValidationService.java": "modules/authentication/security/jwt/common/AbstractJwtValidationService.java",
    "security/common/JwtAuthenticationToken.java": "modules/authentication/security/jwt/common/JwtAuthenticationToken.java",
    "security/common/JwtTokenValidationService.java": "modules/authentication/security/jwt/common/JwtTokenValidationService.java",
    "security/common/JwtValidationResult.java": "modules/authentication/security/jwt/common/JwtValidationResult.java",
    "security/common/TokenType.java": "modules/authentication/security/jwt/common/TokenType.java",
    
    "security/filters/JwtAuthenticationFilter.java": "modules/authentication/security/filters/JwtAuthenticationFilter.java",
    "security/filters/JwtAuthenticationFilterRouting.java": "modules/authentication/security/filters/JwtAuthenticationFilterRouting.java",
    
    # Role Module
    "entity/Role.java": "modules/role/entity/Role.java",
    "entity/RoleType.java": "modules/role/entity/RoleType.java",
    "repository/RoleRepository.java": "modules/role/repository/RoleRepository.java",
    "service/RoleService.java": "modules/role/service/RoleService.java",
    "service/mapper/RoleMapper.java": "modules/role/mapper/RoleMapper.java",
    
    # Permission Module
    "entity/Permission.java": "modules/permission/entity/Permission.java",
    "entity/RolePermission.java": "modules/permission/entity/RolePermission.java",
    
    # Common - Config
    "config/ProvidersSecurityConfiguration.java": "common/config/ProvidersSecurityConfiguration.java",
    "config/AuthCacheConfiguration.java": "common/config/AuthCacheConfiguration.java",
    "config/FallbackJwtConfig.java": "common/config/FallbackJwtConfig.java",
    "config/RetryConfiguration.java": "common/config/RetryConfiguration.java",
    
    # Common - Security
    "security/SecurityConfigurationFactory.java": "common/security/SecurityConfigurationFactory.java",
    "security/SecurityProperties.java": "common/security/SecurityProperties.java",
    "security/PasswordEncoderConfig.java": "common/security/PasswordEncoderConfig.java",
    "security/RateLimitingConfig.java": "common/security/RateLimitingConfig.java",
    "security/SecurityConfiguration.java": "common/security/SecurityConfiguration.java",
    "security/CustomBearerTokenAuthenticationEntryPoint.java": "common/security/CustomBearerTokenAuthenticationEntryPoint.java",
    
    # Common - Cache
    "service/AuthCacheService.java": "common/cache/AuthCacheService.java",
    
    # Common - Exception
    "service/exception/AuthenticationException.java": "common/exception/AuthenticationException.java",
    "service/exception/ResourceNotFoundException.java": "common/exception/ResourceNotFoundException.java",
    
    # Database Service
    "service/AuthDatabaseService.java": "common/config/AuthDatabaseService.java",
    "service/AuthSecurityService.java": "common/config/AuthSecurityService.java",
    
    # Controller for cache
    "controller/CacheController.java": "common/config/CacheController.java",
}

# Package name mappings
PACKAGE_MAPPINGS = {
    "com.demo.auth.entity": {
        "User": "com.demo.auth.modules.user.entity",
        "Role": "com.demo.auth.modules.role.entity",
        "RoleType": "com.demo.auth.modules.role.entity",
        "Permission": "com.demo.auth.modules.permission.entity",
        "RolePermission": "com.demo.auth.modules.permission.entity",
    },
    "com.demo.auth.repository": {
        "User": "com.demo.auth.modules.user.repository",
        "Role": "com.demo.auth.modules.role.repository",
    },
    "com.demo.auth.service": {
        "User": "com.demo.auth.modules.user.service",
        "Authentication": "com.demo.auth.modules.authentication.service",
        "Jwt": "com.demo.auth.modules.authentication.service",
        "StatelessRefreshToken": "com.demo.auth.modules.authentication.service",
        "Role": "com.demo.auth.modules.role.service",
        "AuthCache": "com.demo.auth.common.cache",
        "AuthDatabase": "com.demo.auth.common.config",
        "AuthSecurity": "com.demo.auth.common.config",
    },
    "com.demo.auth.controller": {
        "User": "com.demo.auth.modules.user.controller",
        "Registration": "com.demo.auth.modules.user.controller",
        "Auth": "com.demo.auth.modules.authentication.controller",
        "Cache": "com.demo.auth.common.config",
    },
    "com.demo.auth.security": {
        "custom": "com.demo.auth.modules.authentication.security.jwt.custom",
        "keycloak": "com.demo.auth.modules.authentication.security.jwt.keycloak",
        "common": "com.demo.auth.modules.authentication.security.jwt.common",
        "filters": "com.demo.auth.modules.authentication.security.filters",
        "other": "com.demo.auth.common.security",
    },
    "com.demo.auth.config": "com.demo.auth.common.config",
    "com.demo.auth.mapper": {
        "User": "com.demo.auth.modules.user.mapper",
        "Role": "com.demo.auth.modules.role.mapper",
    },
    "com.demo.auth.dto": {
        "User": "com.demo.auth.modules.user.dto.response",
        "Registration": "com.demo.auth.modules.user.dto",
        "Login": "com.demo.auth.modules.authentication.dto.request",
        "Authentication": "com.demo.auth.modules.authentication.dto",
        "RefreshToken": "com.demo.auth.modules.authentication.dto.request",
    },
}

def move_file(src_rel, dest_rel):
    """Move file from src to dest"""
    src = BASE_PATH / src_rel
    dest = BASE_PATH / dest_rel
    
    if not src.exists():
        print(f"⚠️  Source not found: {src_rel}")
        return False
    
    # Create destination directory
    dest.parent.mkdir(parents=True, exist_ok=True)
    
    try:
        shutil.copy2(src, dest)
        print(f"✅ Moved: {src_rel} -> {dest_rel}")
        return True
    except Exception as e:
        print(f"❌ Error moving {src_rel}: {e}")
        return False

def update_package_declaration(file_path):
    """Update package declaration in Java file"""
    if not file_path.exists():
        return False
    
    try:
        content = file_path.read_text()
        
        # Get new package from file path
        rel_path = file_path.relative_to(BASE_PATH)
        package_parts = list(rel_path.parent.parts)
        new_package = "com.demo.auth." + ".".join(package_parts)
        
        # Update package declaration
        content = re.sub(
            r'^package\s+com\.demo\.auth\.[^;]+;',
            f'package {new_package};',
            content,
            flags=re.MULTILINE
        )
        
        file_path.write_text(content)
        print(f"   📦 Updated package: {new_package}")
        return True
    except Exception as e:
        print(f"   ❌ Error updating package in {file_path}: {e}")
        return False

def main():
    print("🚀 Starting migration to modular structure...")
    print(f"📁 Base path: {BASE_PATH}\n")
    
    moved_count = 0
    failed_count = 0
    
    # Move files
    print("📦 Moving files...")
    for src, dest in MIGRATIONS.items():
        if move_file(src, dest):
            moved_count += 1
        else:
            failed_count += 1
    
    print(f"\n✅ Moved: {moved_count} files")
    print(f"❌ Failed: {failed_count} files")
    
    # Update package declarations
    print("\n📦 Updating package declarations...")
    java_files = list(BASE_PATH.glob("modules/**/*.java")) + list(BASE_PATH.glob("common/**/*.java"))
    
    updated_count = 0
    for java_file in java_files:
        if update_package_declaration(java_file):
            updated_count += 1
    
    print(f"\n✅ Updated {updated_count} package declarations")
    print("\n🎉 Migration complete!")
    print("\n⚠️  Next steps:")
    print("1. Update import statements across all files")
    print("2. Rebuild the project")
    print("3. Fix any compilation errors")

if __name__ == "__main__":
    main()

