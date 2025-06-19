import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phoneNumber: string;
  role?: string;
}

export interface RegisterResponse {
  message: string;
  email: string;
  keycloakUserId: string;
  role: string;
  keycloakLoginUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class RegisterService {
  private apiUrl = 'http://localhost:8093/auth/keycloak';

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register`, request, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    });
  }
  getLoginUrl(): Observable<string> {
    return new Observable<string>(observer => {
      // Créer une URL par défaut
      const defaultUrl = new URL('http://localhost:8080/realms/RepasKeycloak/protocol/openid-connect/auth');
      defaultUrl.searchParams.set('client_id', 'repas-service');
      defaultUrl.searchParams.set('redirect_uri', 'http://localhost:4200');
      defaultUrl.searchParams.set('response_mode', 'fragment');
      defaultUrl.searchParams.set('response_type', 'id_token token');
      defaultUrl.searchParams.set('scope', 'openid');
      defaultUrl.searchParams.set('state', crypto.randomUUID());
      defaultUrl.searchParams.set('nonce', crypto.randomUUID());
      
      // Générer un code challenge aléatoire
      const codeVerifier = crypto.randomUUID() + crypto.randomUUID();
      const encoder = new TextEncoder();
      const data = encoder.encode(codeVerifier);
      
      crypto.subtle.digest('SHA-256', data).then((hash) => {
        const codeChallenge = btoa(String.fromCharCode(...new Uint8Array(hash)))
          .replace(/\+/g, '-')
          .replace(/\//g, '_')
          .replace(/=/g, '');
        defaultUrl.searchParams.set('code_challenge', codeChallenge);
        defaultUrl.searchParams.set('code_challenge_method', 'S256');
        
        observer.next(defaultUrl.toString());
        observer.complete();
      }).catch(error => {
        observer.error(error);
      });
    });
  }
}
