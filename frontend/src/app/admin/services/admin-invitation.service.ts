import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer, Subject } from 'rxjs';
import { switchMap, share } from 'rxjs/operators';

export interface SeatInfo {
  row: number;
  number: number;
}

export interface InvitationDetails {
  id: number;
  eventId: number;
  eventTitle: string;
  userEmail: string;
  status: string;
  seatInfo: SeatInfo | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminInvitationService {
  private apiUrl = '/invitations';
  private refreshInterval = 5000; // 5 seconds pour une meilleure réactivité
  private refreshSubject = new Subject<void>();
  private refreshObservable: Observable<InvitationDetails[]>;

  constructor(private http: HttpClient) {
    this.refreshObservable = this.refreshSubject.pipe(
      switchMap(() => this.getAllInvitations()),
      share()
    );

    // Set up periodic refresh
    timer(0, this.refreshInterval).subscribe(() => {
      this.refreshSubject.next();
    });
  }

  getAllInvitations(): Observable<InvitationDetails[]> {
    return this.http.get<InvitationDetails[]>(`${this.apiUrl}`);
  }

  deleteInvitation(invitationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${invitationId}`);
  }

  confirmInvitation(invitationId: number): Observable<InvitationDetails> {
    return this.http.patch<InvitationDetails>(`${this.apiUrl}/${invitationId}/confirm`, {});
  }

  getRefreshObservable(): Observable<InvitationDetails[]> {
    return this.refreshObservable;
  }

  triggerRefresh(): void {
    this.refreshSubject.next();
  }
}
