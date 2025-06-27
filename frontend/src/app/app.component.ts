// app.component.ts
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet, ActivatedRoute, NavigationEnd } from '@angular/router';
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
import { Subject, map, takeUntil, filter } from 'rxjs';

import { AuthService } from '../app/user/core/services/auth.service';
import { KeycloakProfile } from 'keycloak-js';
import { FooterComponent } from './user/components/footer/footer.component';
import { NavbarComponent } from './user/components/navbar/navbar.component';
import { NotificationComponent } from './notification/components/notification.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSidenavModule,
    MatListModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    FooterComponent,
    NavbarComponent,
    NotificationComponent
  ],
  template: `
    <div class="app-container">
      <app-navbar *ngIf="!hideNavbar"></app-navbar>
      <app-notification></app-notification>
      <main [class.with-navbar]="!hideNavbar">
        <router-outlet></router-outlet>
      </main>
      <app-footer *ngIf="!hideNavbar"></app-footer>
    </div>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    main {
      flex: 1;
      width: 100%;
      &.with-navbar {
        margin-top: 64px;
      }
    }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  hideNavbar = false;
  private destroy$ = new Subject<void>();

  constructor(private router: Router, private activatedRoute: ActivatedRoute) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      let route = this.activatedRoute;
      while (route.firstChild) {
        route = route.firstChild;
      }
      this.hideNavbar = route.snapshot.data['hideNavbar'] === true;
    });
  }

  ngOnInit() {}

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}