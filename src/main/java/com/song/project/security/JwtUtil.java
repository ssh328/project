package com.song.project.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private static SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {

        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // JWT 만들어주는 함수
    public static String createToken(Authentication auth) {
        var user = (CustomUser) auth.getPrincipal();

        String authorities = auth.getAuthorities().stream().
                map(a -> a.getAuthority()).collect(Collectors.joining(","));

        String jwt = Jwts.builder()
                .claim("username", user.getUsername())
                .claim("authorities", authorities)
                .claim("user_id", user.user_id)
                .claim("userId", user.id)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 3600000)) //유효기간 1시간 = 3600000
                .signWith(key)
                .compact();
        return jwt;
    }

    public static String createTemporaryToken(String username, Long userId, long expirationMillis) {
        return Jwts.builder()
                .claim("username", username)
                .claim("userId", userId)
                .claim("type", "verify")  // ✅ 민감 작업용 구분
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    // ==============================================================
    // ✅ JWT 유효성 검증 (서명 및 만료시간)
    // ==============================================================
    public static boolean validateTemporaryToken(String token) {
        try {
            // JJWT 0.11 이상 버전에서는 verifyWith() 사용
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token); // 파싱이 정상적으로 되면 유효한 토큰
            return true;
        } catch (JwtException e) {
            // 만료되었거나 위조된 토큰
            return false;
        }
    }

    // JWT 까주는 함수
    public static Claims extractToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return claims;
    }

}
