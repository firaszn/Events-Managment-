import { ApplicationConfig, APP_INITIALIZER, ErrorHandler, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';

// Keycloak
import { KeycloakService, KeycloakAngularModule } from 'keycloak-angular';
import { initializer } from './user/utils/app-init';
import { AuthService } from '../app/user/core/services/auth.service';
import { AuthGuard } from '../app/user/core/guards/auth.guard';
import { AuthInterceptor } from '../app/user/core/interceptors/auth.interceptor';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }), 
    provideRouter(routes), 
    provideClientHydration(withEventReplay()),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializer,
      multi: true,
      deps: [KeycloakService]
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    KeycloakService,
    AuthService,
    AuthGuard
  ]
};
