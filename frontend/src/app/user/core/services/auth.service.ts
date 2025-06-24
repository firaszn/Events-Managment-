import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { KeycloakProfile, KeycloakTokenParsed } from 'keycloak-js';
import { BehaviorSubject, from, Observable, of } from 'rxjs';

// Suppress Keycloak-specific console errors
const originalConsoleError = console.error;
console.error = (...args: any[]) => {
  const errorMessage = args[0]?.message || args[0]?.toString() || '';
  if (!errorMessage.includes('resourceAccess') && !errorMessage.includes('Keycloak')) {
    originalConsoleError.apply(console, args);
  }
};

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private userProfile = new BehaviorSubject<KeycloakProfile | null>(null);
  userProfile$ = this.userProfile.asObservable();
  private isAuthenticated = new BehaviorSubject<boolean>(false);
  isAuthenticated$ = this.isAuthenticated.asObservable();

  constructor(private keycloakService: KeycloakService) {
    this.initialize();
  }

  private initialize(): void {
    const loginCheck = this.keycloakService.isLoggedIn();
    
    if (loginCheck === true || loginCheck === false) {
      this.handleLoginStatus(loginCheck);
    } else {
      (loginCheck as Promise<boolean>)
        .then((isLoggedIn: boolean) => this.handleLoginStatus(isLoggedIn))
        .catch((error: Error) => {
          console.error('Error checking login status', error);
          this.isAuthenticated.next(false);
        });
    }
  }

  private handleLoginStatus(isLoggedIn: boolean): void {
    this.isAuthenticated.next(isLoggedIn);
    if (isLoggedIn) {
      this.loadUserProfile().catch(console.error);
    }
  }

  public getLoggedUser(): KeycloakTokenParsed | undefined {
    try {
      return this.keycloakService.getKeycloakInstance().idTokenParsed || undefined;
    } catch (e) {
      console.error("Error getting user details", e);
      return undefined;
    }
  }

  public isLoggedIn(): Promise<boolean> | boolean {
    return this.keycloakService.isLoggedIn();
  }

  public loadUserProfile(): Promise<KeycloakProfile> {
    return this.keycloakService.loadUserProfile()
      .then(profile => {
        this.userProfile.next(profile);
        return profile;
      })
      .catch(error => {
        console.error('Error loading user profile', error);
        throw error;
      });
  }

  public login(redirectUri?: string): Promise<void> {
    return this.keycloakService.login({
      redirectUri: redirectUri || window.location.origin
    });
  }

  public async logout(): Promise<void> {
    try {
      console.log('Logging out and redirecting...');
      const redirectUri = window.location.origin; // e.g., 'http://localhost:4200'
      const logoutUrl = this.keycloakService.getKeycloakInstance().createLogoutUrl({
        redirectUri: redirectUri
      });
      
      // Force a full redirect
      window.location.href = logoutUrl;
    } catch (error) {
      console.error('Error during logout:', error);
    }
  }

  public getToken(): Promise<string> {
    return this.keycloakService.getToken();
  }

  public getRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  public refreshToken(minValidity = 5): Promise<boolean> {
    return this.keycloakService.updateToken(minValidity);
  }

  public getTokenParsed(): Promise<KeycloakTokenParsed | undefined> {
    return Promise.resolve(this.keycloakService.getKeycloakInstance().tokenParsed);
  }
}
