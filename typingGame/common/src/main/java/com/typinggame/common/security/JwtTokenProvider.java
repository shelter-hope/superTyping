package com.typinggame.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpiration;

    public String generateToken(Integer userId) {
        return generateToken(userId, "user");
    }

    public String generateToken(Integer userId, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", normalizeRole(role))
                .claim("tokenType", "access")
                .id(UUID.randomUUID().toString())
                .signWith(getSignInKey())
                .expiration(new java.util.Date(System.currentTimeMillis() + jwtExpiration))
                .compact();
    }

    public String generateRefreshToken(Integer userId) {
        return generateRefreshToken(userId, "user");
    }

    public String generateRefreshToken(Integer userId, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", normalizeRole(role))
                .claim("tokenType", "refresh")
                .id(UUID.randomUUID().toString())
                .signWith(getSignInKey())
                .expiration(new java.util.Date(System.currentTimeMillis() + jwtRefreshExpiration))
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Integer getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return Integer.parseInt(claims.getSubject());
    }

    public String getRoleFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return normalizeRole(claims.get("role", String.class));
    }

    public String getTokenType(String token) {
        Claims claims = extractAllClaims(token);
        String tokenType = claims.get("tokenType", String.class);
        return tokenType == null ? "access" : tokenType;
    }

    public String getTokenId(String token) {
        return extractAllClaims(token).getId();
    }

    public Date getExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String normalizeRole(String role) {
        return "admin".equalsIgnoreCase(role) ? "admin" : "user";
    }

    public long getJwtExpiration() {
        return jwtExpiration;
    }

    public long getJwtRefreshExpiration() {
        return jwtRefreshExpiration;
    }
}
