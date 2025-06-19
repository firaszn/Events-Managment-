import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { from, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthManagerService {
  constructor(
    private keycloak: KeycloakService,
    private router: Router
  ) {}
  public async isAuthenticated(): Promise<boolean> {
    const result = await this.keycloak.isLoggedIn();
    return result;
  }

  public login(redirectUri?: string): Promise<void> {
    return this.keycloak.login({
      redirectUri: redirectUri || window.location.origin + '/home'
    });
  }

  public logout(redirectUri?: string): Promise<void> {
    return this.keycloak.logout(redirectUri || window.location.origin + '/login');
  }

  public getToken(): Promise<string> {
    return this.keycloak.getToken();
  }

  public redirectToLogin(): void {
    this.login(window.location.origin + '/home');
  }

  public checkAuthentication(): Observable<boolean> {
    return from(this.isAuthenticated()).pipe(
      tap(authenticated => {
        if (!authenticated && !window.location.pathname.includes('/register')) {
          this.redirectToLogin();
        }
      })
    );
  }
}
