import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private userRoles = new BehaviorSubject<string[]>([]);
  private readonly ADMIN_ROLE = 'ADMIN';

  setRoles(roles: string[]): void {
    console.log('[RoleService] Mise à jour des rôles:', roles);
    this.userRoles.next(roles);
  }

  getRoles(): string[] {
    return this.userRoles.getValue();
  }

  getRoles$(): Observable<string[]> {
    return this.userRoles.asObservable();
  }

  hasRole(role: string): boolean {
    const currentRoles = this.userRoles.getValue();
    const hasRole = currentRoles.includes(role);
    console.log(`[RoleService] Vérification du rôle '${role}':`, {
      hasRole,
      currentRoles,
      requestedRole: role
    });
    return hasRole;
  }

  hasRole$(role: string): Observable<boolean> {
    return this.userRoles.pipe(
      map(roles => roles.includes(role))
    );
  }

  isAdmin(): boolean {
    return this.hasRole(this.ADMIN_ROLE);
  }

  isAdmin$(): Observable<boolean> {
    return this.hasRole$(this.ADMIN_ROLE);
  }

  clearRoles(): void {
    console.log('[RoleService] Suppression des rôles');
    this.userRoles.next([]);
  }
} 