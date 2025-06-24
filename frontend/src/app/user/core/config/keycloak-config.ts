import { KeycloakConfig, KeycloakInitOptions } from 'keycloak-js';
import { environment } from '@env/environment';

// Configuration de base pour Keycloak
export const keycloakConfig: KeycloakConfig = {
  url: environment.keycloak.url,
  realm: environment.keycloak.realm,
  clientId: environment.keycloak.clientId
};

// Options d'initialisation pour Keycloak
export const keycloakInitOptions: KeycloakInitOptions = {
  onLoad: 'login-required',
  checkLoginIframe: false,
  enableLogging: true,
  flow: 'implicit',  // Utilisation du flux implicite
  responseMode: 'fragment',
  silentCheckSsoFallback: false,
  useNonce: true
};

export default keycloakConfig;
