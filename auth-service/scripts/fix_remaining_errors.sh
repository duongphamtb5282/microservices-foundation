#!/bin/bash
cd /Users/duongphamthaibinh/Downloads/SourceCode/design/beautiful/java/microservices/auth-service/src/main/java

# Fix StatelessRefreshTokenService - replace AuthenticationResponseDto constructor with builder
sed -i '' '/return new AuthenticationResponseDto(/,/);/c\
            return AuthenticationResponseDto.builder()\
                .accessToken(newAccessToken)\
                .refreshToken(newRefreshToken)\
                .tokenType("Bearer")\
                .username(username)\
                .build();
' com/demo/auth/module/authentication/service/StatelessRefreshTokenService.java

echo "✅ Fixed all remaining AuthenticationResponseDto constructor calls"

