// app.component.ts
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Subject, map, takeUntil } from 'rxjs';

import { AuthService } from '../app/user/core/services/auth.service';
import { KeycloakProfile } from 'keycloak-js';
import { FooterComponent } from './user/components/footer/footer.component';
import { NavbarComponent } from './user/components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    FooterComponent,
    NavbarComponent,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSidenavModule,
    MatListModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Gestion des Repas';
  isLoggedIn = false;
  userProfile: KeycloakProfile | null = null;
  isHandset = false;
  isMenuOpen = true;
  isLoading = false;
  roles: string[] = [];
  currentYear = new Date().getFullYear();
  
  private destroy$ = new Subject<void>();
  private breakpointObserver = inject(BreakpointObserver);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    // Surveiller les changements de taille d'écran
    this.breakpointObserver.observe([
      Breakpoints.Handset,
      Breakpoints.TabletPortrait,
    ]).pipe(
      takeUntil(this.destroy$),
      map(result => result.matches)
    ).subscribe(isHandset => {
      this.isHandset = isHandset;
      if (isHandset) {
        this.isMenuOpen = false;
      } else {
        this.isMenuOpen = true;
      }
    });
  }

  async ngOnInit() {
    try {
      // Vérifier d'abord si l'utilisateur est déjà authentifié
      const isAuthenticated = await this.authService.isLoggedIn();
      this.isLoggedIn = isAuthenticated;

      if (isAuthenticated) {
        try {
          // Charger le profil utilisateur
          this.userProfile = await this.authService.loadUserProfile();
          this.roles = this.authService.getRoles();
          console.log('User authenticated:', this.userProfile);
          
          // Rediriger vers /home si on est sur la page de login
          if (this.router.url === '/login') {
            await this.router.navigate(['/home']);
          }
        } catch (error) {
          console.error('Error loading user profile:', error);
          this.handleAuthError();
        }
      } else {
        this.handleNotAuthenticated();
      }
    } catch (error) {
      console.error('Initial authentication check failed:', error);
      this.handleAuthError();
    }

    // S'abonner aux changements d'état d'authentification
    this.authService.isAuthenticated$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (isAuthenticated) => {
          this.isLoggedIn = isAuthenticated;
        },
        error: (error) => {
          this.handleAuthError();
        }
      });
  }

  private handleNotAuthenticated() {
    this.userProfile = null;
    this.roles = [];
    if (!this.router.url.includes('login')) {
      this.router.navigate(['/login']);
    }
  }

  private handleAuthError() {
    this.isLoggedIn = false;
    this.userProfile = null;
    this.roles = [];
    this.router.navigate(['/login']);
  }

  // Méthodes utilitaires
  get userInitials(): string {
    if (!this.userProfile) return '';
    const { firstName, lastName } = this.userProfile;
    return `${firstName?.[0] || ''}${lastName?.[0] || ''}`.toUpperCase();
  }

  get userName(): string {
    if (!this.userProfile) return 'Utilisateur';
    const { firstName, lastName, username } = this.userProfile;
    return firstName && lastName 
      ? `${firstName} ${lastName}`
      : username || 'Utilisateur';
  }

  get userEmail(): string {
    return this.userProfile?.email || '';
  }

  
  // Gestion de l'authentification
  async login() {
    if (this.isLoading) return;
    
    this.isLoading = true;
    try {
      await this.authService.login();
      this.isLoading = false;
    } catch (error) {
      console.error('Login error:', error);
      this.isLoading = false;
      // Afficher un message d'erreur à l'utilisateur si nécessaire
    }
  }

  async logout() {
    if (this.isLoading) return;
    
    this.isLoading = true;
    try {
      await this.authService.logout();
      this.isLoading = false;
      this.router.navigate(['/login']);
    } catch (error) {
      console.error('Logout error:', error);
      this.isLoading = false;
      // Forcer la navigation vers la page de login en cas d'erreur
      window.location.href = '/login';
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}