import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { RoleService } from '../services/role.service';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(
    private router: Router,
    private roleService: RoleService,
    private snackBar: MatSnackBar
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    console.log('[AdminGuard] Vérification des droits administrateur...');
    const currentRoles = this.roleService.getRoles();
    console.log('[AdminGuard] Rôles actuels:', currentRoles);
    
    const isAdmin = currentRoles.includes('ADMIN');
    console.log('[AdminGuard] Est admin:', isAdmin);
    
    if (!isAdmin) {
      console.error('[AdminGuard] Accès refusé - Utilisateur non administrateur');
      this.snackBar.open(
        'Accès refusé. Vous devez être administrateur pour accéder à cette page.',
        'Fermer',
        {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'top'
        }
      );

      // Si nous sommes déjà sur /home, ne pas rediriger pour éviter une boucle
      if (state.url !== '/home') {
        this.router.navigate(['/events']);
      }
    }

    return of(isAdmin);
  }
} 