package product_service.security;

import java.util.Date;
 
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
    public String generateAccessToken(Long userId, String email, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

    return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey())
            .compact();
}
public Claims extractClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}

public boolean isTokenValid(String token) {
    try {
        extractClaims(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        System.out.println("JWT validation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        
        return false;
    }
}

public String extractEmail(String token) {
    return extractClaims(token).getSubject();
}

public String extractRole(String token) {
    return extractClaims(token).get("role", String.class);
}

public String generateRefreshToken(Long userId, String email) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + refreshTokenExpirationMs);

    return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey())
            .compact();
}
public Long extractUserId(String token) {
    Object userId = extractClaims(token).get("userId");
    return userId != null ? Long.valueOf(userId.toString()) : null;
}
}