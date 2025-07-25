import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface WaitlistPosition {
  id: number;
  eventId: number;
  eventTitle: string;
  userEmail: string;
  position: number;
  status: string;
  notificationSent: boolean;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class WaitlistService {
  private apiUrl = '/events';
  
  // Subject pour les événements de liste d'attente en temps réel
  private waitlistUpdateSubject = new Subject<WaitlistUpdate>();
  public waitlistUpdate$ = this.waitlistUpdateSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Rejoindre la liste d'attente pour un événement
   */
  joinWaitlist(eventId: number): Observable<WaitlistPosition> {
    return this.http.post<WaitlistPosition>(`${this.apiUrl}/${eventId}/waitlist/join`, {}).pipe(
      tap((response) => {
        this.waitlistUpdateSubject.next({
          type: 'joined',
          eventId: eventId,
          position: response.position
        });
      })
    );
  }

  /**
   * Quitter la liste d'attente pour un événement
   */
  leaveWaitlist(eventId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${eventId}/waitlist/leave`).pipe(
      tap(() => {
        this.waitlistUpdateSubject.next({
          type: 'left',
          eventId: eventId
        });
      })
    );
  }

  /**
   * Obtenir la position dans la liste d'attente
   */
  getWaitlistPosition(eventId: number): Observable<WaitlistPosition> {
    return this.http.get<WaitlistPosition>(`${this.apiUrl}/${eventId}/waitlist/position`);
  }

  /**
   * Confirmer une place depuis la liste d'attente
   */
  confirmWaitlistSpot(eventId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${eventId}/waitlist/confirm`, {}).pipe(
      tap(() => {
        this.waitlistUpdateSubject.next({
          type: 'confirmed',
          eventId: eventId
        });
      })
    );
  }

  /**
   * Obtenir le nombre de personnes en liste d'attente (admin)
   */
  getWaitlistCount(eventId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/${eventId}/waitlist/count`);
  }

  /**
   * Déclencher manuellement la redistribution (admin)
   */
  redistributeSlots(eventId: number, slots: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${eventId}/waitlist/redistribute/${slots}`, {});
  }
}

export interface WaitlistUpdate {
  type: 'joined' | 'left' | 'confirmed' | 'notified';
  eventId: number;
  position?: number;
  message?: string;
} 