package com.example.userservice.service;

import com.example.userservice.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.GrantedAuthority;

@Service
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private SecretKey getSigningKey() {
        System.out.println("Getting signing key for JWT");
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);

        // Try to get role from direct claim
        String role = claims.get("role", String.class);
        if (role != null) {
            System.out.println("Found role in direct claim: " + role);
            return role;
        }

        // Try to get role from realm_access
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = claims.get("realm_access", Map.class);
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (!roles.isEmpty()) {
                System.out.println("Found roles in realm_access: " + roles);
                return roles.get(0);
            }
        }

        System.out.println("No role found in token, defaulting to USER");
        return "USER";
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        System.out.println("Generating token for user: " + userDetails.getUsername());
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String tokenRole = extractRole(token);

            System.out.println("Validating token for user: " + username);
            System.out.println("Token role: " + tokenRole);
            System.out.println("User authorities: " + userDetails.getAuthorities());

            // Check token expiration
            if (isTokenExpired(token)) {
                System.out.println("Token is expired");
                return false;
            }

            // Check username
            if (!username.equals(userDetails.getUsername())) {
                System.out.println("Username mismatch");
                return false;
            }

            // Check role if it's a User
            if (userDetails instanceof UserEntity && tokenRole != null) {
                UserEntity user = (UserEntity) userDetails;
                boolean hasRole = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority ->
                        authority.equals(tokenRole) ||
                        authority.equals("ROLE_" + tokenRole));

                if (!hasRole) {
                    System.out.println("Role mismatch - Token role: " + tokenRole +
                                     ", User authorities: " + user.getAuthorities());
                    return false;
                }
            }

            System.out.println("Token is valid");
            return true;
        } catch (Exception e) {
            System.out.println("Token validation error: " + e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.out.println("Error extracting claims from token: " + e.getMessage());
            throw e;
        }
    }
}