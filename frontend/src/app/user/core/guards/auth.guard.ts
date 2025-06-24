import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { Observable, from, of } from 'rxjs';
import { AuthManagerService } from '../services/auth-manager.service';
import { isPlatformBrowser } from '@angular/common';
import { tap } from 'rxjs/operators';
import { RoleService } from '../services/role.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  constructor(
    private router: Router,
    private authManager: AuthManagerService,
    private roleService: RoleService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    // Si nous sommes sur le serveur, permettre le rendu initial
    if (!isPlatformBrowser(this.platformId)) {
      return of(true);
    }

    // Allow access to register route without authentication
    if (route.routeConfig?.path === 'register') {
      return of(true);
    }

    // Vérifier l'authentification
    return from(this.authManager.isAuthenticated()).pipe(
      tap(async authenticated => {
        if (!authenticated) {
          if (!state.url.includes('login') && !state.url.includes('register')) {
            console.log('[AuthGuard] Utilisateur non authentifié, redirection vers la connexion');
            this.authManager.redirectToLogin();
          }
          return false;
        }

        // Si l'utilisateur est authentifié, vérifier les accès spécifiques
        const isAdmin = this.roleService.hasRole('ADMIN');
        const currentPath = route.routeConfig?.path;

        if (currentPath === 'admin-dashboard' && !isAdmin) {
          console.log('[AuthGuard] Accès refusé au dashboard admin');
          this.router.navigate(['/home']);
          return false;
        }

        if (currentPath === 'home' && isAdmin) {
          console.log('[AuthGuard] Redirection admin vers son dashboard');
          this.router.navigate(['/admin-dashboard']);
          return false;
        }

        return true;
      })
    );
  }
}