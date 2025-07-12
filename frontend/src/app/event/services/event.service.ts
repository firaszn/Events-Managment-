import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer, Subject, BehaviorSubject } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { OccupiedSeat } from '../models/seat.model';

export interface EventRequest {
  title: string;
  description?: string;
  location: string;
  eventDate: string;
}

export interface EventResponse {
  id: string;
  title: string;
  description?: string;
  location: string;
  eventDate: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  userRegistered: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private apiUrl = '/events';
  private destroy$ = new Subject<void>();
  private seatLockTimer: any;
  private currentLockedSeat: BehaviorSubject<any | null> = new BehaviorSubject<any | null>(null);

  constructor(private http: HttpClient) {}

  getAllEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(this.apiUrl);
  }

  getEventById(id: string): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.apiUrl}/${id}`);
  }

  createEvent(event: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(this.apiUrl, event);
  }

  updateEvent(id: string, event: EventRequest): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.apiUrl}/${id}`, event);
  }

  deleteEvent(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getOccupiedSeats(eventId: string): Observable<OccupiedSeat[]> {
    return this.http.get<OccupiedSeat[]>(`/invitations/event/${eventId}/occupied-seats`);
  }

  lockSeat(eventId: string, row: number, number: number): Observable<any> {
    const seatInfo = { row, number };
    return this.http.post(`/invitations/event/${eventId}/lock-seat`, seatInfo);
  }

  releaseSeat(eventId: string, row: number, number: number): Observable<any> {
    const seatInfo = { row, number };
    return this.http.delete(`/invitations/event/${eventId}/release-seat`, { body: seatInfo });
  }

  startSeatLockTimer(eventId: string, row: number, number: number): void {
    // Arrêter le timer précédent s'il existe
    this.stopSeatLockTimer();

    // Enregistrer le siège actuellement verrouillé
    this.currentLockedSeat.next({ eventId, row, number });

    // Démarrer un nouveau timer
    this.seatLockTimer = timer(0, 60000) // Vérifier toutes les minutes
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        const currentSeat = this.currentLockedSeat.value;
        if (currentSeat) {
          this.lockSeat(currentSeat.eventId, currentSeat.row, currentSeat.number).subscribe();
        }
      });
  }

  stopSeatLockTimer(): void {
    if (this.seatLockTimer) {
      this.seatLockTimer.unsubscribe();
      this.seatLockTimer = null;
    }

    const currentSeat = this.currentLockedSeat.value;
    if (currentSeat) {
      this.releaseSeat(currentSeat.eventId, currentSeat.row, currentSeat.number).subscribe();
      this.currentLockedSeat.next(null);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopSeatLockTimer();
  }
} 