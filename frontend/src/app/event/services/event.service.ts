import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
} 