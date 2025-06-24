import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface UserDetails {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  enabled: boolean;
  createdTimestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserDetails[]> {
    return this.http.get<UserDetails[]>(`${this.apiUrl}`);
  }

  updateUserStatus(userId: string, enabled: boolean): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${userId}/status`, { enabled });
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}`);
  }
} 