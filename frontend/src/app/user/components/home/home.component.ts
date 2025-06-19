import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { KeycloakProfile } from 'keycloak-js';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatListModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  userProfile: KeycloakProfile | null = null;
  roles: string[] = [];
  isLoading = true;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  async ngOnInit() {
    try {
      const isLoggedIn = await this.authService.isLoggedIn();
      
      if (!isLoggedIn) {
        this.router.navigate(['/login']);
        return;
      }

      this.userProfile = await this.authService.loadUserProfile();
      this.roles = this.authService.getRoles();
    } catch (error) {
      console.error('Error loading user data:', error);
      this.showError('Erreur lors du chargement des données utilisateur');
      this.router.navigate(['/login']);
    } finally {
      this.isLoading = false;
    }
  }

  async logout() {
    try {
      this.isLoading = true;
      console.log('Initiating logout...');
      
      // Forcer la déconnexion complète avec redirection vers la page de connexion Keycloak
      const redirectUri = window.location.origin + '/login';
      const logoutSuccessful = await this.authService.logout(redirectUri);
      
      if (!logoutSuccessful) {
        // Si la déconnexion échoue, forcer le rechargement de la page
        window.location.href = '/login';
        return;
      }
      
      console.log('Logout successful, navigating to login');
      
      // S'assurer que l'utilisateur est bien déconnecté
      this.router.navigate(['/login']).then(() => {
        console.log('Navigation to login complete');
        // Rafraîchir la page pour s'assurer que tout est réinitialisé
        window.location.reload();
      });
    } catch (error) {
      console.error('Logout error:', error);
      this.showError('Erreur lors de la déconnexion');
      
      // En cas d'erreur, rediriger quand même vers la page de connexion
      window.location.href = '/login';
    } finally {
      this.isLoading = false;
    }
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
