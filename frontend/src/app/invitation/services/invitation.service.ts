import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { OccupiedSeat } from '../../event/models/seat.model';

export interface SeatInfo {
  row: number;
  number: number;
}

export interface InvitationRequest {
  eventId: string;
  eventTitle: string;
  userEmail: string;
  seatInfo: {
    row: number;
    number: number;
  };
}

export interface InvitationResponse {
  id: number;
  eventId: number;
  eventTitle: string;
  userEmail: string;
  status: string;
  seatInfo: {
    row: number;
    number: number;
  };
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class InvitationService {
  private apiUrl = '/invitations';
  
  // Sujet pour les événements d'invitation en temps réel
  private invitationCreatedSubject = new Subject<InvitationResponse>();
  public invitationCreated$ = this.invitationCreatedSubject.asObservable();

  constructor(private http: HttpClient) {}

  createInvitation(request: InvitationRequest): Observable<InvitationResponse> {
    return this.http.post<InvitationResponse>(this.apiUrl, request).pipe(
      tap((response) => {
        // Émettre l'événement de création d'invitation
        this.invitationCreatedSubject.next(response);
      })
    );
  }

  getOccupiedSeats(eventId: string): Observable<OccupiedSeat[]> {
    return this.http.get<OccupiedSeat[]>(`${this.apiUrl}/event/${eventId}/occupied-seats`);
  }

  isUserRegistered(eventId: string, userEmail: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/check/${eventId}/${userEmail}`);
  }
}
