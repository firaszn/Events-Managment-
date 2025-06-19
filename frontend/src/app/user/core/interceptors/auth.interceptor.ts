import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, from } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { KeycloakService } from 'keycloak-angular';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private keycloakService: KeycloakService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Exclure les requêtes vers Keycloak
    if (request.url.includes('keycloak') || request.url.includes('assets')) {
      return next.handle(request);
    }

    return from(this.keycloakService.getToken()).pipe(
      mergeMap(token => {
        if (token) {
          request = request.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });
        }
        return next.handle(request);
      })
    );
  }
}
