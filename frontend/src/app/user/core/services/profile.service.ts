import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserProfile {
  firstName: string;
  lastName: string;
  email: string;
  password?: string;
  phoneNumber: string;
  // Champs facultatifs côté UI/API
  avatarUrl?: string;
  updatedAt?: string | number | Date;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private apiUrl = '/api/users/profile';

  constructor(private http: HttpClient) {}

  // Récupérer le profil de l'utilisateur
  getUserProfile(): Observable<UserProfile> {
    console.log('Tentative de récupération du profil depuis:', this.apiUrl);
    return new Observable(observer => {
      this.http.get<UserProfile>(this.apiUrl, {
        withCredentials: true,
        observe: 'response'
      }).subscribe({
        next: (response) => {
          console.log('Réponse du serveur:', response);
          if (response.body) {
            observer.next(response.body);
            observer.complete();
          } else {
            observer.error(new Error('Réponse vide du serveur'));
          }
        },
        error: (error) => {
          console.error('Erreur lors de la récupération du profil:', {
            status: error.status,
            statusText: error.statusText,
            error: error.error,
            headers: error.headers
          });
          observer.error(error);
        }
      });
    });
  }

  // Mettre à jour le profil
  updateProfile(profile: UserProfile): Observable<UserProfile> {
    return this.http.put<UserProfile>(this.apiUrl, profile, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }
}
