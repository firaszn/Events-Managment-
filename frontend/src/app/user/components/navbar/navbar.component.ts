import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { KeycloakProfile } from 'keycloak-js';
import { Subject, takeUntil, map } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { Router, RouterLink, RouterLinkActive, RouterModule } from '@angular/router';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    RouterModule,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {
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
    // Watch for screen size changes
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
      // Check if user is already authenticated
      const isAuthenticated = await this.authService.isLoggedIn();
      this.isLoggedIn = isAuthenticated;
      
      if (isAuthenticated) {
        try {
          // Load user profile
          this.userProfile = await this.authService.loadUserProfile();
          this.roles = this.authService.getRoles();
          
          // Redirect to /home if on login page
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

    // Subscribe to authentication state changes
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

  get userName(): string {
    if (!this.userProfile) return '';
    const { firstName, lastName, username } = this.userProfile;
    return firstName && lastName ? `${firstName} ${lastName}` : username || '';
  }

  get userInitials(): string {
    if (!this.userProfile) return '';
    const { firstName, lastName } = this.userProfile;
    return (firstName?.[0] || '') + (lastName?.[0] || '');
  }

  async login(): Promise<void> {
    if (this.isLoading) return;
    this.isLoading = true;
    try {
      await this.authService.login();
    } catch (error) {
      console.error('Login error:', error);
    } finally {
      this.isLoading = false;
    }
  }

  async logout(): Promise<void> {
    if (this.isLoading) return;
    this.isLoading = true;
    try {
      await this.authService.logout();
      this.router.navigate(['http://localhost:8080/realms/RepasKeycloak/protocol/openid-connect/auth?client_id=repas-service&redirect_uri=http%3A%2F%2Flocalhost%3A4200&state=b8d0d236-3616-46f9-a273-18c4d6080359&response_mode=fragment&response_type=id_token%20token&scope=openid&nonce=390d1600-508c-4dfd-ad4c-4898ed6a69ff&code_challenge=RZm5XHWMrfWOA9lLvOgMxviRVz0MyK1LVa2xhqjUxxY&code_challenge_method=S256']);
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      this.isLoading = false;
    }
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

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
