package com.sky.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    /**
     * Creates an HS256 JWT using the configured Base64-encoded key
     *
     * @param secretKey Base64-encoded signing key
     * @param ttlMillis Token lifetime in milliseconds
     * @param claims    Claims to include in the token
     * @return Signed JWT
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        JwtBuilder builder = Jwts.builder()
                // Set custom claims before standard claims so they cannot overwrite registered values
                .setClaims(claims)
                .signWith(signatureAlgorithm, decodeSecretKey(secretKey))
                .setExpiration(exp);

        return builder.compact();
    }

    /**
     * Parses a signed JWT and verifies it with the configured key
     *
     * @param secretKey Base64-encoded signing key
     * @param token Signed JWT to parse
     * @return Verified claims from the token
     * @throws io.jsonwebtoken.JwtException If the token is invalid or its signature cannot be verified
     */
    public static Claims parseJWT(String secretKey, String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(decodeSecretKey(secretKey))
                .parseClaimsJws(token).getBody();
        return claims;
    }

    /**
     * Decodes a Base64 HMAC key and enforces the HS256 minimum of 256 bits
     *
     * @param encodedSecretKey Base64-encoded signing key
     * @return Decoded HMAC key bytes
     * @throws IllegalArgumentException If the key is missing, invalid Base64, or shorter than 256 bits
     */
    public static byte[] decodeSecretKey(String encodedSecretKey) {
        if (encodedSecretKey == null || encodedSecretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret must be configured as Base64");
        }

        final byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedSecretKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT secret must be valid Base64", exception);
        }

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("HS256 JWT secret must decode to at least 32 bytes");
        }
        return keyBytes;
    }

}
