import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { from, Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { RoleService } from './role.service';

@Injectable({
  providedIn: 'root'
})
export class AuthManagerService {
  constructor(
    private keycloak: KeycloakService,
    private router: Router,
    private roleService: RoleService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  private get isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  private getBaseUrl(): string {
    if (!this.isBrowser) {
      return '/';
    }
    return window.location.origin;
  }

  private async redirectBasedOnRole(): Promise<void> {
    const roles = this.keycloak.getUserRoles();
    console.log('[AuthManagerService] Redirection basée sur les rôles:', roles);
    
    if (roles.includes('ADMIN')) {
      console.log('[AuthManagerService] Utilisateur admin, redirection vers /admin-dashboard');
      await this.router.navigate(['/admin-dashboard']);
    } else {
      console.log('[AuthManagerService] Utilisateur standard, redirection vers /home');
      await this.router.navigate(['/home']);
    }
  }

  public async isAuthenticated(): Promise<boolean> {
    if (!this.isBrowser) {
      return false;
    }
    try {
      const result = await this.keycloak.isLoggedIn();
      if (result) {
        const roles = this.keycloak.getUserRoles();
        this.roleService.setRoles(roles);
        console.log('[AuthManagerService] Rôles mis à jour:', roles);
      } else {
        this.roleService.clearRoles();
      }
      return result;
    } catch (error) {
      console.error('Error checking authentication:', error);
      this.roleService.clearRoles();
      return false;
    }
  }

  public async login(redirectUri?: string): Promise<void> {
    if (!this.isBrowser) {
      return Promise.resolve();
    }
    
    // Si aucun URI de redirection n'est spécifié, on laisse Keycloak gérer la redirection
    // et on s'occupera de la redirection après l'authentification
    if (!redirectUri) {
      await this.keycloak.login({
        redirectUri: `${this.getBaseUrl()}`
      });
    } else {
      await this.keycloak.login({
        redirectUri: redirectUri
      });
    }
  }

  public async logout(redirectUri?: string): Promise<void> {
    if (!this.isBrowser) {
      return Promise.resolve();
    }
    this.roleService.clearRoles();
    return this.keycloak.logout(redirectUri || `${this.getBaseUrl()}/login`);
  }

  public getToken(): Promise<string> {
    if (!this.isBrowser) {
      return Promise.resolve('');
    }
    return this.keycloak.getToken();
  }

  public hasRole(role: string): boolean {
    if (!this.isBrowser) {
      return false;
    }
    const hasRole = this.keycloak.isUserInRole(role);
    console.log(`[AuthManagerService] Vérification du rôle '${role}':`, hasRole);
    return hasRole;
  }

  public redirectToLogin(): void {
    if (!this.isBrowser) {
      return;
    }
    this.login();
  }

  public checkAuthentication(): Observable<boolean> {
    if (!this.isBrowser) {
      return of(false);
    }
    return from(this.isAuthenticated()).pipe(
      tap(async authenticated => {
        if (authenticated) {
          // Si l'utilisateur est authentifié, rediriger en fonction du rôle
          await this.redirectBasedOnRole();
        } else if (!window.location.pathname.includes('/register')) {
          this.redirectToLogin();
        }
      })
    );
  }

  public async updateRoles(): Promise<void> {
    if (this.isBrowser && await this.isAuthenticated()) {
      const roles = this.keycloak.getUserRoles();
      this.roleService.setRoles(roles);
      console.log('[AuthManagerService] Rôles mis à jour manuellement:', roles);
      await this.redirectBasedOnRole();
    }
  }
}
