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
     * 生成jwt
     * 使用HS256算法，密钥以Base64形式配置并解码为原始随机字节
     *
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims    设置的信息
     * @return
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 指定签名的时候使用的签名算法，也就是header那部分
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        // 生成JWT的时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 设置jwt的body
        JwtBuilder builder = Jwts.builder()
                // 如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给builder的claim赋值，一旦写在标准的声明赋值之后，就是覆盖了那些标准的声明的
                .setClaims(claims)
                // 设置签名使用的签名算法和签名使用的秘钥
                .signWith(signatureAlgorithm, decodeSecretKey(secretKey))
                // 设置过期时间
                .setExpiration(exp);

        return builder.compact();
    }

    /**
     * Token解密
     *
     * @param secretKey jwt秘钥 此秘钥一定要保留好在服务端, 不能暴露出去, 否则sign就可以被伪造, 如果对接多个客户端建议改造成多个
     * @param token     加密后的token
     * @return
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 得到DefaultJwtParser
        Claims claims = Jwts.parser()
                // 设置签名的秘钥
                .setSigningKey(decodeSecretKey(secretKey))
                // 设置需要解析的jwt
                .parseClaimsJws(token).getBody();
        return claims;
    }

    /**
     * Decode a Base64 HMAC key and enforce the HS256 minimum of 256 bits
     * @param encodedSecretKey
     * @return
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
