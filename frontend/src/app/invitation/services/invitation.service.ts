import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InvitationRequest {
  eventId: string;
  eventTitle: string;
  userEmail: string;
}

export interface InvitationResponse {
  id: string;
  eventId: string;
  eventTitle: string;
  userEmail: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class InvitationService {
  private apiUrl = '/api/invitations';

  constructor(private http: HttpClient) {}

  createInvitation(request: InvitationRequest): Observable<InvitationResponse> {
    return this.http.post<InvitationResponse>(this.apiUrl, request);
  }
} 