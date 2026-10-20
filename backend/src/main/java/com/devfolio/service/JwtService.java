package com.devfolio.service;

import com.devfolio.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    @Value("${jwt.secret:hardcoded-jwt-secret-do-not-use-change-in-env-32chars}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration; // 24h par défaut (en millisecondes)

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim("userId", user.getId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        // VULN-012 : Rejet immédiat si le token a été invalidé lors d'un /logout
        if (token == null || token.isBlank() || tokenBlacklistService.isBlacklisted(token)) {
            throw new JwtException("Token révoqué ou absent");
        }
        // Validation stricte : si la signature est invalide ou le token expiré,
        // le parser lève une exception et l'accès est immédiatement rejeté.
        return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } 
    }

