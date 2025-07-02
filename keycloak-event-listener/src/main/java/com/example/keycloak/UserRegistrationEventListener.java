package com.example.keycloak;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.jboss.logging.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UserRegistrationEventListener implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(UserRegistrationEventListener.class);
    private final KeycloakSession session;
    private final String userServiceUrl = "http://localhost:8093/auth/keycloak/sync-from-keycloak";
    private final HttpClient httpClient;
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 5000; // 5 secondes

    public UserRegistrationEventListener(KeycloakSession session) {
        this.session = session;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.REGISTER) {
            String userId = event.getUserId();
            log.info("Nouvel événement d'enregistrement détecté pour l'utilisateur ID: " + userId);
            
            // Tentatives de synchronisation avec délai exponentiel
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    if (attempt > 1) {
                        // Délai exponentiel : 5s, 10s, 20s, 40s, 80s
                        long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt - 2);
                        log.info("Attente de " + (delay/1000) + " secondes avant la tentative " + 
                               attempt + "/" + MAX_RETRIES);
                        Thread.sleep(delay);
                    }

                    RealmModel realm = session.realms().getRealm(event.getRealmId());
                    UserModel user = session.users().getUserById(realm, userId);
                    
                    if (user != null) {
                        String userEmail = user.getEmail();
                        log.info("Tentative de synchronisation pour l'utilisateur: " + userEmail);

                        // Construction de la requête HTTP
                        String syncUrl = userServiceUrl + "?email=" + userEmail;
                        log.info("URL de synchronisation: " + syncUrl);

                        HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(syncUrl))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .timeout(Duration.ofSeconds(30))
                            .build();

                        // Envoi de la requête
                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        
                        // Log des headers de la réponse pour le débogage
                        response.headers().map().forEach((key, values) -> 
                            log.info("Header " + key + ": " + String.join(", ", values))
                        );
                        
                        // Vérification du code de statut
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.info("Synchronisation réussie pour " + userEmail + ". Réponse: " + response.body());
                            return; // Sortir de la boucle si succès
                        } else {
                            log.error("Échec de la synchronisation pour " + userEmail + 
                                    ". Code: " + response.statusCode() + 
                                    ", Réponse: " + response.body());
                            if (attempt == MAX_RETRIES) {
                                log.error("Échec de toutes les tentatives de synchronisation pour " + userEmail);
                            }
                        }
                    } else {
                        log.error("Utilisateur non trouvé dans Keycloak avec l'ID: " + userId);
                        if (attempt == MAX_RETRIES) {
                            log.error("Impossible de trouver l'utilisateur après " + MAX_RETRIES + " tentatives");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interruption lors de l'attente entre les tentatives", e);
                    break;
                } catch (Exception e) {
                    log.error("Erreur lors de la tentative " + attempt + " de synchronisation: " + e.getMessage(), e);
                    if (attempt == MAX_RETRIES) {
                        log.error("Échec de toutes les tentatives de synchronisation", e);
                        // Log de la stack trace complète pour le débogage
                        for (StackTraceElement element : e.getStackTrace()) {
                            log.error(element.toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // Non utilisé pour cet exemple
    }

    @Override
    public void close() {
        // Cleanup si nécessaire
    }
} 