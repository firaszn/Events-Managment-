import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SeatInfo {
  row: number;
  number: number;
}

export interface InvitationRequest {
  eventId: string;
  eventTitle: string;
  userEmail: string;
  seatInfo?: SeatInfo;
}

export interface InvitationResponse {
  id: string;
  eventId: string;
  eventTitle: string;
  userEmail: string;
  status: string;
  seatInfo?: SeatInfo;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class InvitationService {
  private apiUrl = '/invitations';

  constructor(private http: HttpClient) {}

  createInvitation(request: InvitationRequest): Observable<InvitationResponse> {
    return this.http.post<InvitationResponse>(this.apiUrl, request);
  }

  getOccupiedSeats(eventId: string): Observable<SeatInfo[]> {
    return this.http.get<SeatInfo[]>(`${this.apiUrl}/event/${eventId}/occupied-seats`);
  }
} 