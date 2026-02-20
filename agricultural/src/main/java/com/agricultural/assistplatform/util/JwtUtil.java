package com.agricultural.assistplatform.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：生成与解析 Token（用户端/商家端 7 天有效期）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-days:7}")
    private int expireDays;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String subject, String type) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + (long) expireDays * 24 * 60 * 60 * 1000);
        return Jwts.builder()
                .setSubject(subject)
                .claim("type", type)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        String sub = parseToken(token).getSubject();
        return sub == null ? null : Long.parseLong(sub);
    }

    public String getType(String token) {
        return (String) parseToken(token).get("type");
    }
}
