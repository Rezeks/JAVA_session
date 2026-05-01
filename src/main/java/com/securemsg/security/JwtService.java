package com.securemsg.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT-утилита: генерация и валидация токенов.
 * Токен содержит: login, role, время выпуска и срок действия.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${securemsg.jwt.secret:SecureMessagingSystemDefaultSecretKey32B}") String secret,
            @Value("${securemsg.jwt.expiration-ms:3600000}") long expirationMs) {
        // Pad/truncate to 32 bytes for HMAC-SHA256
        byte[] keyBytes = new byte[32];
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Генерация JWT токена
     */
    public String generateToken(String login, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(login)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Извлечение логина из токена
     */
    public String extractLogin(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Извлечение роли из токена
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Проверка валидности токена
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
