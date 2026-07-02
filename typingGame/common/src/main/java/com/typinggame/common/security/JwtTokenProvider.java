package com.typinggame.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpiration;

    public String generateToken(Integer userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .signWith(getSignInKey())
                .expiration(new java.util.Date(System.currentTimeMillis() + jwtExpiration))
                .compact();
    }

    public String generateRefreshToken(Integer userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
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

    public long getJwtExpiration() {
        return jwtExpiration;
    }

    public long getJwtRefreshExpiration() {
        return jwtRefreshExpiration;
    }
}