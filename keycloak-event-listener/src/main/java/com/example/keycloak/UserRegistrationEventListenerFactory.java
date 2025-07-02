package com.example.keycloak;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Collections;
import java.util.Map;

public class UserRegistrationEventListenerFactory implements EventListenerProviderFactory, ServerInfoAwareProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserRegistrationEventListener(session);
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Initialisation si nécessaire
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post-initialisation si nécessaire
    }

    @Override
    public void close() {
        // Cleanup si nécessaire
    }

    @Override
    public String getId() {
        return "user-registration-event-listener";
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return Collections.singletonMap("version", "1.0");
    }
} 