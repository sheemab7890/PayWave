package com.sheemab.shardedsagawallet.services;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    /**
     * JWT token generate karo user ke email se.
     *
     * Token structure:
     * Header.Payload.Signature
     *
     * Payload mein hoga:
     * - subject  : user email
     * - issuedAt : kab banaya
     * - expiry   : kab expire hoga
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Token se email nikalo.
     * JwtFilter yeh call karega.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Token valid hai ya nahi check karo.
     * - Signature sahi hai?
     * - Expire to nahi hua?
     * - Email match karta hai?
     */
    public boolean isTokenValid(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            boolean notExpired = !isTokenExpired(token);
            boolean emailMatches = extractedEmail.equals(email);
            return notExpired && emailMatches;
        } catch (Exception e) {
            log.warn("[JWT] Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extractClaims(String token) {
        // parseSignedClaims = token verify karo + claims nikalo
        // agar tampered ya expired hai toh exception throw hogi
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // secret string ko SecretKey object mein convert karo
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
