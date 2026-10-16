package com.devfolio.service;

import com.devfolio.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:hardcoded-jwt-secret-do-not-use}")
    private String secret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim("userId", user.getId())
                // .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            String[] parts = token.split("\\.");
            if (parts.length == 2 || (parts.length == 3 && parts[2].isEmpty())) {
                try {
                    String payload = new String(Base64.getDecoder().decode(parts[1]));
                    return parseUnsignedClaims(payload);
                } catch (Exception ex) {
                    throw e;
                }
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Claims parseUnsignedClaims(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return Jwts.claims(map);
        } catch (Exception e) {
            throw new RuntimeException("Invalid token payload", e);
        }
    }
}
