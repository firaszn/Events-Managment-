import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService, EventResponse } from '../services/event.service';
import { InvitationService } from '../../invitation/services/invitation.service';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../user/core/services/auth.service';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  template: `
    <div class="events-container">
      <h2>Liste des Événements</h2>
      
      <div class="events-grid">
        <div *ngFor="let event of events" class="event-card">
          <h3>{{ event.title }}</h3>
          <p *ngIf="event.description">{{ event.description }}</p>
          <div class="event-details">
            <p><i class="fas fa-map-marker-alt"></i> {{ event.location }}</p>
            <p><i class="fas fa-calendar-alt"></i> {{ formatDate(event.eventDate) }}</p>
          </div>
          <p class="event-creator">Créé par: {{ event.createdBy }}</p>
          
          <button mat-raised-button 
                  color="primary" 
                  class="register-button"
                  (click)="registerForEvent(event)"
                  *ngIf="isLoggedIn && !isAdmin">
            S'inscrire
          </button>
        </div>
      </div>

      <div *ngIf="error" class="error-message">
        {{ error }}
      </div>
    </div>
  `,
  styles: [`
    .events-container {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }

    h2 {
      color: #2c3e50;
      margin-bottom: 20px;
    }

    .events-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;
    }

    .event-card {
      background: white;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      transition: transform 0.2s ease;
      position: relative;
    }

    .event-card:hover {
      transform: translateY(-5px);
    }

    .event-card h3 {
      color: #2c3e50;
      margin: 0 0 10px 0;
    }

    .event-details {
      margin: 15px 0;
      color: #666;
    }

    .event-details p {
      margin: 5px 0;
    }

    .event-details i {
      margin-right: 8px;
      color: #3498db;
    }

    .event-creator {
      font-size: 0.9em;
      color: #7f8c8d;
      margin-top: 10px;
    }

    .error-message {
      color: #e74c3c;
      padding: 10px;
      margin-top: 20px;
      background: #fde2e2;
      border-radius: 4px;
    }

    .register-button {
      width: 100%;
      margin-top: 15px;
    }
  `]
})
export class EventListComponent implements OnInit {
  events: EventResponse[] = [];
  error: string = '';
  isLoggedIn = false;
  isAdmin = false;
  userEmail: string = '';

  constructor(
    private eventService: EventService,
    private invitationService: InvitationService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  async ngOnInit() {
    this.loadEvents();
    this.isLoggedIn = await this.authService.isLoggedIn();
    if (this.isLoggedIn) {
      const profile = await this.authService.loadUserProfile();
      this.userEmail = profile?.email || '';
      this.isAdmin = this.authService.getRoles().includes('ADMIN');
    }
  }

  loadEvents() {
    this.eventService.getAllEvents().subscribe({
      next: (events) => {
        this.events = events;
      },
      error: (error) => {
        console.error('Error loading events:', error);
        this.error = 'Une erreur est survenue lors du chargement des événements.';
      }
    });
  }

  registerForEvent(event: EventResponse) {
    if (!this.isLoggedIn || !this.userEmail) {
      this.snackBar.open('Veuillez vous connecter pour vous inscrire à un événement.', 'Fermer', {
        duration: 3000
      });
      return;
    }

    const request = {
      eventId: event.id,
      eventTitle: event.title,
      userEmail: this.userEmail
    };

    this.invitationService.createInvitation(request).subscribe({
      next: (response) => {
        this.snackBar.open('Inscription réussie !', 'Fermer', {
          duration: 3000
        });
      },
      error: (error) => {
        console.error('Error registering for event:', error);
        let errorMessage = 'Une erreur est survenue lors de l\'inscription.';
        
        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.status === 200 && !error.ok) {
          errorMessage = 'Erreur de communication avec le serveur. Veuillez réessayer.';
        }
        
        this.snackBar.open(errorMessage, 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
} 