package com.example.userservice.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakAuthResponse {
    private String message;
    private String email;
    private String keycloakUserId;
    private String role;
    private String keycloakLoginUrl; // URL pour se connecter directement à Keycloak
    private String accessToken; // Token JWT de Keycloak
    private String tokenType; // Type de token (Bearer)
    private Long expiresIn; // Durée de validité du token en secondes
}
