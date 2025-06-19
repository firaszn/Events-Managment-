import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { Observable, from } from 'rxjs';
import { AuthManagerService } from '../services/auth-manager.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  constructor(
    private router: Router,
    private authManager: AuthManagerService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    // Allow access to register route without authentication
    if (route.routeConfig?.path === 'register') {
      return from(Promise.resolve(true));
    }

    // For all other routes, check authentication
    return from(this.authManager.isAuthenticated());
  }
}