import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer, Subject, BehaviorSubject } from 'rxjs';
import { takeUntil, switchMap, catchError, tap } from 'rxjs/operators';
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
  maxCapacity?: number;
  waitlistEnabled?: boolean;
  confirmedParticipants?: number;
  waitlistCount?: number;
  userWaitlistPosition?: number;
  userWaitlistStatus?: string; // WAITING, NOTIFIED, CONFIRMED, EXPIRED, CANCELLED
  userHasPendingInvitation?: boolean; // L'utilisateur a une invitation en attente
  userStatus?: string; // Ajouté pour gérer l'état CANCELLED
}

@Injectable({
  providedIn: 'root'
})
export class EventService implements OnDestroy {
  private apiUrl = '/events';
  private destroy$ = new Subject<void>();
  private seatLockTimer: any;
  private currentLockedSeat: BehaviorSubject<any | null> = new BehaviorSubject<any | null>(null);
  private readonly LOCK_CHECK_INTERVAL = 30000; // 30 secondes

  // Observable pour les places verrouillées
  private lockedSeatsSubject = new BehaviorSubject<{eventId: string, seats: any[]}>({ eventId: '', seats: [] });
  public lockedSeats$ = this.lockedSeatsSubject.asObservable();

  constructor(private http: HttpClient) {
    // Vérifier les verrous existants au démarrage
    this.checkStoredLock();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopSeatLockTimer();
    this.lockedSeatsSubject.complete();
  }

  checkStoredLock(): void {
    const storedLock = localStorage.getItem('seatLock');
    if (storedLock) {
      try {
        const lock = JSON.parse(storedLock);
        const now = Date.now();
        const lockTime = new Date(lock.timestamp).getTime();
        const timeLeft = 5 * 60 * 1000 - (now - lockTime); // 5 minutes en millisecondes

        if (timeLeft > 0) {
          // Réactiver le verrou pour le temps restant
          this.startSeatLockTimer(lock.eventId, lock.row, lock.number);
          // Mettre à jour les places verrouillées
          this.updateLockedSeats(lock.eventId, [{
            row: lock.row,
            number: lock.number
          }]);
        } else {
          // Le verrou a expiré, le nettoyer
          this.stopSeatLockTimer();
          this.clearLockedSeats(lock.eventId);
        }
      } catch (e) {
        console.error('Error parsing stored lock:', e);
        localStorage.removeItem('seatLock');
      }
    }
  }

  private updateLockedSeats(eventId: string, seats: any[]) {
    const currentState = this.lockedSeatsSubject.value;
    if (currentState.eventId === eventId) {
      // Mettre à jour les places pour le même événement
      this.lockedSeatsSubject.next({
        eventId,
        seats: [...currentState.seats, ...seats]
      });
    } else {
      // Nouvel événement
      this.lockedSeatsSubject.next({
        eventId,
        seats
      });
    }
  }

  private clearLockedSeats(eventId: string) {
    const currentState = this.lockedSeatsSubject.value;
    if (currentState.eventId === eventId) {
      this.lockedSeatsSubject.next({
        eventId,
        seats: []
      });
    }
  }

  lockSeat(eventId: string, row: number, number: number): Observable<any> {
    const seatInfo = { row, number };
    return this.http.post(`/invitations/event/${eventId}/lock-seat`, seatInfo).pipe(
      tap(() => {
        // Mettre à jour les places verrouillées après un verrouillage réussi
        this.updateLockedSeats(eventId, [seatInfo]);
      })
    );
  }

  releaseSeat(eventId: string, row: number, number: number): Observable<any> {
    const seatInfo = { row, number };
    return this.http.delete(`/invitations/event/${eventId}/release-seat`, { body: seatInfo }).pipe(
      tap(() => {
        // Mettre à jour les places verrouillées après une libération réussie
        const currentState = this.lockedSeatsSubject.value;
        if (currentState.eventId === eventId) {
          this.lockedSeatsSubject.next({
            eventId,
            seats: currentState.seats.filter(s =>
              !(s.row === row && s.number === number)
            )
          });
        }
      })
    );
  }

  startSeatLockTimer(eventId: string, row: number, number: number): void {
    // Arrêter le timer existant s'il y en a un
    this.stopSeatLockTimer();

    // Sauvegarder le verrou dans le localStorage
    const lock = {
      eventId,
      row,
      number,
      timestamp: new Date().toISOString()
    };
    localStorage.setItem('seatLock', JSON.stringify(lock));

    // Mettre à jour les places verrouillées
    this.updateLockedSeats(eventId, [{
      row,
      number
    }]);

    // Démarrer un timer pour vérifier périodiquement le verrou
    this.seatLockTimer = timer(0, this.LOCK_CHECK_INTERVAL).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const storedLock = localStorage.getItem('seatLock');
      if (storedLock) {
        try {
          const lock = JSON.parse(storedLock);
          const now = Date.now();
          const lockTime = new Date(lock.timestamp).getTime();
          const timeLeft = 5 * 60 * 1000 - (now - lockTime); // 5 minutes

          if (timeLeft <= 0) {
            // Le verrou a expiré
            this.stopSeatLockTimer();
            this.clearLockedSeats(eventId);
            localStorage.removeItem('seatLock');
          }
        } catch (e) {
          console.error('Error checking lock:', e);
          this.stopSeatLockTimer();
          localStorage.removeItem('seatLock');
        }
      }
    });
  }

  stopSeatLockTimer(): void {
    if (this.seatLockTimer) {
      this.seatLockTimer.unsubscribe();
      this.seatLockTimer = null;
    }
    localStorage.removeItem('seatLock');
    const currentState = this.lockedSeatsSubject.value;
    this.lockedSeatsSubject.next({
      eventId: currentState.eventId,
      seats: []
    });
  }

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

  /**
   * Annuler l'inscription d'un utilisateur à un événement
   */
  cancelUserRegistration(eventId: string, userEmail: string): Observable<void> {
    return this.http.patch<void>(`/invitations/cancel/${eventId}/${userEmail}`, {});
  }
}
