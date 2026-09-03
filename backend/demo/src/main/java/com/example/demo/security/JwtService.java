package com.example.demo.security;

import com.example.demo.entity.Administrateur;
import com.example.demo.entity.CentreDeDonnees;
import com.example.demo.entity.DashboardModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(Administrateur user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        List<String> accessList = user.getDashboardAccess() != null
                ? user.getDashboardAccess().stream().map(DashboardModule::name).collect(Collectors.toList())
                : List.of();

        List<Long> centreIds = user.getCentres() != null
                ? user.getCentres().stream().map(CentreDeDonnees::getIdCentre).collect(Collectors.toList())
                : List.of();

        List<String> centreNoms = user.getCentres() != null
                ? user.getCentres().stream().map(CentreDeDonnees::getNom).collect(Collectors.toList())
                : List.of();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getIdAdmin())
                .claim("role", user.getRole() != null ? user.getRole() : "ADMIN")
                .claim("fullName", user.getFullName())
                .claim("prenom", user.getPrenom())
                .claim("nom", user.getNom())
                .claim("dashboardAccess", accessList)
                .claim("centreIds", centreIds)
                .claim("centreNoms", centreNoms)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)  // ← ALGORITHME EXPLICITE
                .compact();
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)  // ← ALGORITHME EXPLICITE
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public Set<DashboardModule> extractDashboardAccess(String token) {
        Claims claims = extractAllClaims(token);
        @SuppressWarnings("unchecked")
        List<String> accessList = claims.get("dashboardAccess", List.class);
        if (accessList == null) return Set.of();
        return accessList.stream().map(DashboardModule::valueOf).collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    public List<Long> extractCentreIds(String token) {
        Claims claims = extractAllClaims(token);
        List<Number> ids = claims.get("centreIds", List.class);
        if (ids == null) return List.of();
        return ids.stream().map(Number::longValue).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractCentreNoms(String token) {
        Claims claims = extractAllClaims(token);
        List<String> noms = claims.get("centreNoms", List.class);
        return noms != null ? noms : List.of();
    }

    public boolean isTokenValid(String token, String email) {
        String extractedEmail = extractEmail(token);
        return extractedEmail.equals(email) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ============================================================
    // CORRECTION : la clé doit faire au moins 32 caractères (256 bits)
    // ============================================================
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}