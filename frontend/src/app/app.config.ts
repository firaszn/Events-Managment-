import { ApplicationConfig, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideClientHydration } from '@angular/platform-browser';

// Keycloak
import { KeycloakService } from 'keycloak-angular';
import { initializer } from './user/utils/app-init';
import { AuthService } from '../app/user/core/services/auth.service';
import { AuthGuard } from '../app/user/core/guards/auth.guard';
import { AuthInterceptor } from '../app/user/core/interceptors/auth.interceptor';
import { RoleService } from './user/core/services/role.service';
import { AdminGuard } from './user/core/guards/admin.guard';
import { EventService } from '../app/event/services/event.service';
import { MatSnackBar } from '@angular/material/snack-bar';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializer,
      multi: true,
      deps: [KeycloakService, RoleService]
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    KeycloakService,
    AuthService,
    AuthGuard,
    RoleService,
    AdminGuard,
    EventService,
    MatSnackBar
  ]
};
