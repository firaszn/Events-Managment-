import { KeycloakConfig, KeycloakInitOptions } from 'keycloak-js';

// Configuration de base pour Keycloak
const keycloakConfig: KeycloakConfig = {
  url: 'http://localhost:8080',
  realm: 'RepasKeycloak',
  clientId: 'repas-service'
};

// Options d'initialisation pour Keycloak
export const keycloakInitOptions: KeycloakInitOptions = {
  onLoad: 'check-sso',
  checkLoginIframe: false,
  enableLogging: true,
  flow: 'implicit',  // Utilisation du flux implicite
  responseMode: 'fragment',
  silentCheckSsoFallback: false,
  useNonce: true
};

export default keycloakConfig;
