import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InvitationDetails {
  id: number;
  eventId: number;
  eventTitle: string;
  userEmail: string;
  status: string;
  created_at: string;
  updated_at: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminInvitationService {
  private apiUrl = '/invitations';

  constructor(private http: HttpClient) {}

  getInvitations(): Observable<InvitationDetails[]> {
    return this.http.get<InvitationDetails[]>(this.apiUrl);
  }

  deleteInvitation(invitationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${invitationId}`);
  }
} 