package com.trackify.security;

import com.trackify.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtConfig jwtConfig;
    private final SecretKey key;

    @Autowired
    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());
        
        return Jwts.builder()
                .setSubject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("role", userPrincipal.getRole().name())
                .claim("firstName", userPrincipal.getFirstName())
                .claim("lastName", userPrincipal.getLastName())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String generateRefreshToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration());
        
        return Jwts.builder()
                .setSubject(userPrincipal.getId().toString())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String generateTokenFromOAuth2User(OAuth2UserPrincipal oauth2UserPrincipal) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());
        
        return Jwts.builder()
                .setSubject(oauth2UserPrincipal.getId().toString())
                .claim("email", oauth2UserPrincipal.getEmail())
                .claim("role", oauth2UserPrincipal.getRole().name())
                .claim("firstName", oauth2UserPrincipal.getFirstName())
                .claim("lastName", oauth2UserPrincipal.getLastName())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshTokenFromOAuth2User(OAuth2UserPrincipal oauth2UserPrincipal) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration());
        
        return Jwts.builder()
                .setSubject(oauth2UserPrincipal.getId().toString())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String generateTokenFromUserId(Long userId) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return Long.parseLong(claims.getSubject());
    }
    
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("email", String.class);
    }
    
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getExpiration();
    }
    
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT token validation error: {}", e.getMessage());
        }
        return false;
    }
    
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    public long getExpirationTime() {
        return jwtConfig.getExpiration();
    }
    
    public long getRefreshExpirationTime() {
        return jwtConfig.getRefreshExpiration();
    }
}