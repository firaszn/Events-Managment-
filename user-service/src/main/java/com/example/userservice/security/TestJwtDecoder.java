package com.example.userservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Décodeur JWT de test qui ignore la validation de signature
 * ATTENTION: À utiliser uniquement pour les tests !
 */
public class TestJwtDecoder implements JwtDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            System.out.println("Décodage du token JWT sans validation de signature (MODE TEST)");
            
            // Séparer les parties du JWT
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new JwtException("Token JWT invalide - doit avoir 3 parties");
            }

            // Décoder le header
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> headers = objectMapper.readValue(headerJson, Map.class);

            // Décoder le payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

            // Extraire les timestamps
            Instant issuedAt = Instant.ofEpochSecond(((Number) claims.get("iat")).longValue());
            Instant expiresAt = Instant.ofEpochSecond(((Number) claims.get("exp")).longValue());

            // Créer le JWT
            Jwt jwt = new Jwt(token, issuedAt, expiresAt, headers, claims);

            System.out.println("Token décodé avec succès (MODE TEST):");
            System.out.println("- Issuer: " + jwt.getIssuer());
            System.out.println("- Subject: " + jwt.getSubject());
            System.out.println("- Email: " + jwt.getClaimAsString("email"));
            System.out.println("- Roles: " + jwt.getClaimAsMap("realm_access"));

            return jwt;

        } catch (Exception e) {
            System.err.println("Erreur lors du décodage du token: " + e.getMessage());
            throw new JwtException("Impossible de décoder le token JWT", e);
        }
    }
}
