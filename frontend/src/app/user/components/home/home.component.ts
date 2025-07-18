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
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

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
    MatSnackBarModule,
    RouterModule,
    FormsModule
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  userProfile: KeycloakProfile | null = null;
  roles: string[] = [];
  isLoading = true;
  private destroy$ = new Subject<void>();
  newsletterEmail: string = '';

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

    // Add animation classes after component loads
    setTimeout(() => {
      const heroContent = document.querySelector('.hero-content');
      if (heroContent) {
        heroContent.classList.add('animate__animated', 'animate__fadeIn');
      }
    }, 100);
  }

  async logout() {
      this.isLoading = true;
    try {
      await this.authService.logout();
    } catch (error) {
      console.error('Logout error:', error);
      this.showError('Erreur lors de la déconnexion');
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

  onNewsletterSubmit() {
    if (this.newsletterEmail) {
      // TODO: Implement newsletter subscription logic
      console.log('Newsletter subscription for:', this.newsletterEmail);
      alert('Merci de votre inscription à notre newsletter !');
      this.newsletterEmail = '';
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
