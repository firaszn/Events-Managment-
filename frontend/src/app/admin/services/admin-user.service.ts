import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface UserDetails {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles?: string[];
  role?: string;
  enabled: boolean;
  createdTimestamp?: number;
  created_at?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<UserDetails[]> {
    return this.http.get<UserDetails[]>(this.apiUrl).pipe(
      map(users => users.filter(user => {
        if (!user) return false;
        
        // Vérifier le format roles[] (Keycloak)
        if (user.roles && Array.isArray(user.roles)) {
          return !user.roles.some(role => role === 'ADMIN' || role === 'ROLE_ADMIN');
        }
        
        // Vérifier le format role (string from DB)
        if (user.role) {
          return user.role !== 'ADMIN' && user.role !== 'ROLE_ADMIN';
        }
        
        return true; // Inclure l'utilisateur si aucun rôle n'est défini
      }))
    );
  }

  updateUserStatus(userId: string, enabled: boolean): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${userId}/status`, { enabled });
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}`);
  }

  updateUser(userId: string, userData: Partial<UserDetails>): Observable<UserDetails> {
    return this.http.put<UserDetails>(`${this.apiUrl}/${userId}`, userData);
  }
} 