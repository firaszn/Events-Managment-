import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EventDetails {
  id: string;
  title: string;
  description: string;
  location: string;
  eventDate: string;
  organizer?: string;
  organizerId?: string;
  created_at?: string;
  updated_at?: string;
  userRegistered?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminEventService {
  private apiUrl = '/events';

  constructor(private http: HttpClient) {}

  getEvents(): Observable<EventDetails[]> {
    return this.http.get<EventDetails[]>(this.apiUrl);
  }

  deleteEvent(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}`);
  }

  createEvent(eventData: Partial<EventDetails>): Observable<EventDetails> {
    return this.http.post<EventDetails>(this.apiUrl, eventData);
  }
} 