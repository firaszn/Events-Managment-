import { KeycloakService } from 'keycloak-angular';
import { environment } from '../environments/environment';
import { keycloakInitOptions } from '../core/config/keycloak-config';
import { RoleService } from '../core/services/role.service';
import { keycloakConfig } from '../core/config/keycloak-config';

export function initializer(keycloak: KeycloakService, roleService: RoleService): () => Promise<boolean> {
  return (): Promise<boolean> => {
    return new Promise(async (resolve) => {
      // Vérifier si on est côté navigateur
      if (typeof window === 'undefined') {
        console.log('Server-side rendering detected, skipping Keycloak init');
        resolve(false);
        return;
      }

      console.log('Initializing Keycloak...');
      
      try {
        // Configuration de base de Keycloak
        const keycloakConfig = {
          url: environment.keycloak.url,
          realm: environment.keycloak.realm,
          clientId: environment.keycloak.clientId
        };

        // Utilisation des options d'initialisation depuis la configuration

        console.log('Keycloak config:', keycloakConfig);
        
        const initOptions = {
          ...keycloakInitOptions,
          redirectUri: window.location.origin
        };
        
        console.log('Keycloak init options:', initOptions);
        
        try {
          console.log('Initializing Keycloak with config:', {
            ...keycloakConfig,
            initOptions: {
              ...keycloakInitOptions,
              redirectUri: window.location.origin
            }
          });
          
          // Initialisation de Keycloak
          const authenticated = await keycloak.init({
            config: keycloakConfig,
            initOptions: {
              ...keycloakInitOptions,
              redirectUri: window.location.origin
            },
            loadUserProfileAtStartUp: true,
            enableBearerInterceptor: true,
            bearerExcludedUrls: [
              '/assets',
              '/clients/public',
              '/error',
              '/favicon.ico'
            ]
          }).catch(error => {
            console.error('Keycloak init error details:', {
              error,
              message: error.message,
              stack: error.stack
            });
            throw error;
          });

          console.log('Keycloak initialized, authenticated:', authenticated);
          
          if (authenticated) {
            try {
              // Chargement du profil utilisateur
              console.log('Loading user profile...');
              const profile = await keycloak.loadUserProfile();
              console.log('User profile loaded successfully:', profile);
              const roles = keycloak.getUserRoles();
              roleService.setRoles(roles);
              console.log('User roles:', roles);
              
              // Log token info (sans le token complet pour des raisons de sécurité)
              const token = keycloak.getKeycloakInstance().token;
              console.log('Token info:', {
                tokenType: typeof token,
                tokenLength: token?.length,
                tokenPreview: token ? `${token.substring(0, 10)}...` : 'No token'
              });
            } catch (error) {
              const profileError = error as Error;
              console.warn('Could not load user profile', {
                error: profileError,
                message: profileError.message,
                stack: profileError.stack
              });
            }
          }
          
          resolve(authenticated);
          
        } catch (initError) {
          console.error('Keycloak initialization error:', initError);
          
          // Vérifier si c'est une erreur de réseau
          if (initError instanceof Error && initError.message.includes('NetworkError')) {
            console.error('Network error - Please check if Keycloak server is running at:', keycloakConfig.url);
          }
          
          // Vérifier si c'est une erreur 401 (non autorisé)
          if (initError instanceof Error && initError.message.includes('401')) {
            console.error('Authentication failed - Please check client configuration in Keycloak');
            console.error('Make sure the client configuration matches:', {
              clientId: keycloakConfig.clientId,
              realm: keycloakConfig.realm,
              redirectUri: window.location.origin
            });
          }
          
          // Ne pas rejeter pour éviter de bloquer l'application
          resolve(false);
        }
        
      } catch (error) {
        console.error('Unexpected error during Keycloak initialization:', error);
        resolve(false);
      }
    });
  };
}
export function initializeKeycloak(keycloak: KeycloakService) {
  return () =>
    keycloak.init({
      config: keycloakConfig,
      initOptions: {
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html',
        checkLoginIframe: false
      },
      enableBearerInterceptor: true,
      bearerPrefix: 'Bearer',
      bearerExcludedUrls: ['/assets']
    });
}

