package com.example.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class KeycloakService {

    @Value("${keycloak.admin.server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.admin.realm:RepasKeycloak}")
    private String realm;

    @Value("${keycloak.admin.client-id:repas-service}")
    private String clientId;

    @Value("${keycloak.admin.client-secret:xELXqoDJ4DRmBxdlQqDn6a9trwNh8Wjq}")
    private String clientSecret;

    @Value("${keycloak.admin.grant-type:client_credentials}")
    private String grantType;

    private Keycloak getKeycloakInstance() {
        log.info("Configuration Keycloak - Server: {}, Client: {}, Realm: {}", serverUrl, clientId, realm);
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm) // Utiliser le realm RepasKeycloak directement
                .clientId(clientId) // Utiliser le client repas-service
                .clientSecret(clientSecret) // Le secret du client
                .grantType(grantType) // Service Account
                .build();
    }

    public String createUser(String email, String firstName, String lastName, String password, String role) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Créer la représentation de l'utilisateur
            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(false);

            // Créer l'utilisateur
            Response response = usersResource.create(user);
            
            if (response.getStatus() == 201) {
                // Récupérer l'ID de l'utilisateur créé
                String userId = extractUserIdFromResponse(response);
                
                // Définir le mot de passe
                setUserPassword(usersResource, userId, password);
                
                // Assigner le rôle
                assignRoleToUser(realmResource, userId, role);
                
                log.info("Utilisateur créé avec succès dans Keycloak: {}", email);
                return userId;
            } else {
                log.error("Erreur lors de la création de l'utilisateur: {}", response.getStatus());
                throw new RuntimeException("Erreur lors de la création de l'utilisateur dans Keycloak");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur dans Keycloak: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création de l'utilisateur dans Keycloak: " + e.getMessage());
        }
    }

    private String extractUserIdFromResponse(Response response) {
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void setUserPassword(UsersResource usersResource, String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        usersResource.get(userId).resetPassword(credential);
    }

    public void setUserPassword(String userId, String password) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            setUserPassword(usersResource, userId, password);
            log.info("Mot de passe mis à jour pour l'utilisateur: {}", userId);
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du mot de passe pour l'utilisateur {}: {}", userId, e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour du mot de passe: " + e.getMessage());
        }
    }

    private void assignRoleToUser(RealmResource realmResource, String userId, String roleName) {
        try {
            // Récupérer le rôle du realm
            var roleRepresentation = realmResource.roles().get(roleName.toUpperCase()).toRepresentation();

            // Assigner le rôle à l'utilisateur
            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(roleRepresentation));

            log.info("Rôle {} assigné à l'utilisateur {}", roleName, userId);
        } catch (Exception e) {
            log.warn("Impossible d'assigner le rôle {} à l'utilisateur {}: {}", roleName, userId, e.getMessage());
            // Ne pas faire échouer la création si l'assignation du rôle échoue
        }
    }

    public void assignRoleToUser(String userId, String roleName) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(realm);
            assignRoleToUser(realmResource, userId, roleName);
        } catch (Exception e) {
            log.error("Erreur lors de l'assignation du rôle {} à l'utilisateur {}: {}", roleName, userId, e.getMessage());
        }
    }

    public UserRepresentation getUserByEmail(String email) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users = usersResource.search(email, true);
            
            if (!users.isEmpty()) {
                return users.get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de l'utilisateur: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateUserCredentials(String email, String password) {
        try {
            // Essayer de créer une connexion Keycloak avec les credentials de l'utilisateur
            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId("repas-service") // Utiliser le client de l'application
                    .clientSecret(clientSecret) // Ajouter le client secret
                    .username(email)
                    .password(password)
                    .grantType("password")
                    .build();

            // Tester la connexion en récupérant le token
            userKeycloak.tokenManager().getAccessToken();
            return true;
        } catch (Exception e) {
            log.error("Échec de validation des credentials pour {}: {}", email, e.getMessage());
            return false;
        }
    }

    public String getKeycloakAccessToken(String email, String password) {
        try {
            // Créer une connexion Keycloak avec les credentials de l'utilisateur
            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId("repas-service")
                    .clientSecret(clientSecret)
                    .username(email)
                    .password(password)
                    .grantType("password")
                    .build();

            // Récupérer le token d'accès
            String accessToken = userKeycloak.tokenManager().getAccessToken().getToken();
            log.info("Token d'accès Keycloak récupéré avec succès pour: {}", email);
            return accessToken;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du token Keycloak pour {}: {}", email, e.getMessage());
            return null;
        }
    }

    public List<String> getUserRoles(String userId) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(realm);

            // Récupérer les rôles du realm assignés à l'utilisateur
            var userRoles = realmResource.users().get(userId).roles().realmLevel().listAll();

            List<String> roleNames = userRoles.stream()
                    .map(role -> role.getName())
                    .filter(roleName -> roleName.equals("USER") || roleName.equals("ADMIN"))
                    .collect(java.util.stream.Collectors.toList());

            log.info("Rôles trouvés pour l'utilisateur {}: {}", userId, roleNames);
            return roleNames;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des rôles pour l'utilisateur {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void updateUser(UserRepresentation userRepresentation) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(realm);

            realmResource.users().get(userRepresentation.getId()).update(userRepresentation);
            log.info("Utilisateur {} mis à jour dans Keycloak", userRepresentation.getEmail());
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'utilisateur {} dans Keycloak: {}",
                    userRepresentation.getEmail(), e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour dans Keycloak: " + e.getMessage());
        }
    }

    public void deleteUser(String userId) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(realm);

            realmResource.users().get(userId).remove();
            log.info("Utilisateur {} supprimé de Keycloak", userId);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'utilisateur {} de Keycloak: {}", userId, e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression de Keycloak: " + e.getMessage());
        }
    }
}
