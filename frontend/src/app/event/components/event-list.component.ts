import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService, EventResponse } from '../services/event.service';
import { InvitationService } from '../../invitation/services/invitation.service';
import { WaitlistService } from '../services/waitlist.service';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../user/core/services/auth.service';
import { NotificationService } from '../../notification/services/notification.service';
import { NotificationComponent } from '../../notification/components/notification.component';
import { Router, NavigationEnd } from '@angular/router';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NotificationComponent],
  template: `
    <div class="events-page">
      <app-notification></app-notification>

      <!-- Loading Spinner -->
      <div class="spinner-container" *ngIf="isLoading">
        <div class="spinner"></div>
        <p>Chargement des événements...</p>
      </div>

      <!-- Content (hidden while loading) -->
      <div *ngIf="!isLoading">
        <!-- Header Section -->
        <div class="events-header">
          <h1>Découvrez nos événements</h1>
          <p class="subtitle">Rejoignez-nous pour des moments inoubliables</p>
        </div>

      <!-- Search and Filter Section -->
      <div class="search-section">
        <div class="search-bar">
          <i class="fas fa-search"></i>
          <input
            type="text"
            [(ngModel)]="searchTerm"
            (input)="filterEvents()"
            placeholder="Rechercher un événement..."
          >
        </div>
        <div class="filter-chips">
          <span
            *ngFor="let filter of filters"
            class="custom-chip"
            [class.selected]="filter.active"
            (click)="toggleFilter(filter)">
            {{filter.label}}
          </span>
        </div>
      </div>

      <!-- Events Grid -->
      <div class="events-grid" *ngIf="filteredEvents.length > 0">
        <div *ngFor="let event of filteredEvents"
             class="event-card hover-lift fade-in">
          <div class="event-card-header">
            <div class="event-status"
                 [class.status-upcoming]="isUpcoming(event.eventDate)"
                 [class.status-ongoing]="isOngoing(event.eventDate)"
                 [class.status-past]="isPast(event.eventDate)">
              {{ getEventStatus(event.eventDate) }}
            </div>
            <div class="event-date">
              <div class="date-day">{{ formatDateDay(event.eventDate) }}</div>
              <div class="date-month">{{ formatDateMonth(event.eventDate) }}</div>
            </div>
          </div>

          <div class="event-content">
            <h3 class="event-title" title="{{ event.title }}">{{ event.title }}</h3>
            <p class="event-description" *ngIf="event.description">{{ event.description }}</p>

            <div class="event-details">
              <div class="detail-item">
                <i class="fas fa-map-marker-alt"></i>
                <span>{{ event.location }}</span>
              </div>
              <div class="detail-item">
                <i class="fas fa-clock"></i>
                <span>{{ formatTime(event.eventDate) }}</span>
              </div>
              <!-- Informations de capacité et liste d'attente -->
              <div class="detail-item" *ngIf="event.maxCapacity">
                <i class="fas fa-users"></i>
                <span>{{ event.confirmedParticipants || 0 }}/{{ event.maxCapacity }} places</span>
              </div>
              <div class="detail-item" *ngIf="event.waitlistEnabled && event.waitlistCount && event.waitlistCount > 0">
                <i class="fas fa-list-alt"></i>
                <span>{{ event.waitlistCount }} en attente</span>
              </div>
              <!-- Position dans la liste d'attente pour l'utilisateur -->
              <div class="detail-item waitlist-position" *ngIf="event.userWaitlistPosition">
                <i class="fas fa-hourglass-half"></i>
                <span>Position {{ event.userWaitlistPosition }} en liste d'attente</span>
              </div>
            </div>

            <div class="event-footer">
              <!-- Bouton d'inscription normal -->
              <button
                class="custom-button"
                [class.registered]="event.userRegistered || event.userHasPendingInvitation"
                [disabled]="isPast(event.eventDate) || event.userRegistered || event.userHasPendingInvitation || isEventFull(event)"
                (click)="registerForEvent(event)"
                *ngIf="isLoggedIn && !isAdmin && !event.userWaitlistPosition && !isEventFull(event) && event.userStatus !== 'CANCELLED'">
                <i class="fas" [class.fa-calendar-check]="!event.userRegistered && !event.userHasPendingInvitation" [class.fa-check-circle]="event.userRegistered || event.userHasPendingInvitation"></i>
                <span *ngIf="event.userRegistered">Déjà inscrit</span>
                <span *ngIf="event.userHasPendingInvitation && !event.userRegistered">Déjà inscrit</span>
                <span *ngIf="!event.userRegistered && !event.userHasPendingInvitation">S'inscrire</span>
              </button>

              <!-- Bouton d'annulation d'inscription -->
              <button
                class="custom-button cancel-button"
                [disabled]="isPast(event.eventDate)"
                (click)="cancelRegistration(event)"
                *ngIf="isLoggedIn && !isAdmin && (event.userRegistered || event.userHasPendingInvitation) && event.userStatus !== 'CANCELLED'">
                <i class="fas fa-times-circle"></i>
                <span>Annuler inscription</span>
              </button>

              <!-- Bouton pour rejoindre la liste d'attente -->
              <button
                class="custom-button waitlist-button"
                [disabled]="isPast(event.eventDate)"
                (click)="joinWaitlist(event)"
                *ngIf="isLoggedIn && !isAdmin && !event.userRegistered && !event.userHasPendingInvitation && !event.userWaitlistPosition && isEventFull(event) && event.waitlistEnabled && event.userStatus !== 'CANCELLED'">
                <i class="fas fa-list-alt"></i>
                <span>Rejoindre la liste d'attente</span>
              </button>

              <!-- Affichage état de la liste d'attente -->
              <button
                class="custom-button waitlist-status"
                [disabled]="true"
                *ngIf="isLoggedIn && !isAdmin && event.userWaitlistPosition && !isWaitlistNotified(event) && event.userStatus !== 'CANCELLED'">
                <i class="fas fa-hourglass-half"></i>
                <span>En liste d'attente ({{ event.userWaitlistPosition }}ème)</span>
              </button>

              <!-- Bouton de confirmation de place depuis la liste d'attente -->
              <button
                class="custom-button confirm-waitlist-button"
                [disabled]="isPast(event.eventDate)"
                (click)="confirmWaitlistSpot(event)"
                *ngIf="isLoggedIn && !isAdmin && isWaitlistNotified(event) && event.userStatus !== 'CANCELLED'">
                <i class="fas fa-check-circle"></i>
                <span>Confirmer ma place</span>
              </button>

              <!-- Bouton pour les événements complets sans liste d'attente -->
              <button
                class="custom-button"
                [disabled]="true"
                *ngIf="isLoggedIn && !isAdmin && !event.userRegistered && !event.waitlistEnabled && isEventFull(event) && event.userStatus !== 'CANCELLED'">
                <i class="fas fa-ban"></i>
                <span>Événement complet</span>
              </button>

              <!-- Bouton inscription annulée -->
              <button
                class="custom-button cancelled-button"
                [disabled]="true"
                *ngIf="isLoggedIn && !isAdmin && event.userStatus === 'CANCELLED'">
                <i class="fas fa-ban"></i>
                <span>Inscription annulée</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div class="empty-state" *ngIf="filteredEvents.length === 0">
        <i class="fas fa-calendar-times"></i>
        <h3>Aucun événement trouvé</h3>
        <p>Essayez de modifier vos critères de recherche</p>
      </div>
      </div>

      <div *ngIf="error" class="error-message">
        <i class="fas fa-exclamation-circle"></i>
        {{ error }}
      </div>
    </div>
  `,
  styles: [`
    .events-page {
      padding: 40px;
      max-width: 1400px;
      margin: 0 auto;
      min-height: 100vh;
      background-color: #f8f9fa;
    }

    /* Spinner Styles */
    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 60vh;
      color: #667eea;
    }

    .spinner {
      width: 50px;
      height: 50px;
      border: 4px solid rgba(102, 126, 234, 0.3);
      border-top: 4px solid #667eea;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 1rem;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .spinner-container p {
      font-size: 1.2rem;
      margin: 0;
      opacity: 0.8;
      font-weight: 500;
    }

    .events-header {
      text-align: center;
      margin-bottom: 40px;
    }

    .events-header h1 {
      font-size: 2.5em;
      color: #2c3e50;
      margin-bottom: 10px;
      font-weight: 700;
    }

    .subtitle {
      color: #666;
      font-size: 1.2em;
    }

    .search-section {
      margin-bottom: 30px;
    }

    .search-bar {
      display: flex;
      align-items: center;
      background: white;
      padding: 10px 20px;
      border-radius: 50px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
      margin-bottom: 20px;
    }

    .search-bar i {
      color: #666;
      margin-right: 10px;
    }

    .search-bar input {
      border: none;
      outline: none;
      width: 100%;
      font-size: 1.1em;
      color: #333;
    }

    .filter-chips {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      margin-bottom: 20px;
    }

    .custom-chip {
      padding: 8px 16px;
      border-radius: 20px;
      background: white;
      color: #666;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.3s ease;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      user-select: none;
    }

    .custom-chip:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 8px rgba(0,0,0,0.15);
    }

    .custom-chip.selected {
      background: #2196F3;
      color: white;
    }

    .events-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: 30px;
      padding: 20px 0;
    }

    .event-card {
      background: white;
      border-radius: 15px;
      overflow: hidden;
      box-shadow: 0 4px 15px rgba(0,0,0,0.1);
      transition: transform 0.3s ease, box-shadow 0.3s ease;
    }

    .event-card:hover {
      transform: translateY(-5px);
      box-shadow: 0 8px 25px rgba(0,0,0,0.15);
    }

    .event-card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 20px;
      background: linear-gradient(135deg, #2196F3, #1976D2);
      color: white;
    }

    .event-status {
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 0.9em;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .status-upcoming {
      background-color: #4CAF50;
    }

    .status-ongoing {
      background-color: #FF9800;
    }

    .status-past {
      background-color: #9E9E9E;
    }

    .event-date {
      text-align: center;
      background: rgba(255,255,255,0.2);
      padding: 10px 15px;
      border-radius: 10px;
      backdrop-filter: blur(5px);
    }

    .date-day {
      font-size: 1.8em;
      font-weight: 700;
      line-height: 1;
    }

    .date-month {
      font-size: 1em;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .event-content {
      padding: 20px;
    }

    .event-title {
      color: #2c3e50;
      font-size: 1.4em;
      margin: 0 0 15px 0;
      font-weight: 600;
      line-height: 1.3;
    }

    .event-description {
      color: #666;
      margin-bottom: 20px;
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .event-details {
      margin: 20px 0;
    }

    .detail-item {
      display: flex;
      align-items: center;
      margin-bottom: 10px;
      color: #666;
    }

    .detail-item i {
      margin-right: 10px;
      color: #2196F3;
      font-size: 16px;
      width: 20px;
      text-align: center;
    }

    .event-footer {
      margin-top: 20px;
      padding-top: 20px;
      border-top: 1px solid #eee;
    }

    .custom-button {
      width: 100%;
      padding: 12px 24px;
      border: none;
      border-radius: 25px;
      font-weight: 600;
      font-size: 1rem;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      background: linear-gradient(135deg, #2196F3, #1976D2);
      color: white;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      box-shadow: 0 4px 15px rgba(33, 150, 243, 0.3);
    }

    .custom-button:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(33, 150, 243, 0.4);
      background: linear-gradient(135deg, #1E88E5, #1565C0);
    }

    .custom-button:disabled {
      background: #E0E0E0;
      cursor: not-allowed;
      box-shadow: none;
      color: #9E9E9E;
    }

    .custom-button.registered {
      background: linear-gradient(135deg, #4CAF50, #43A047);
      box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
      cursor: default;
    }

    .custom-button.registered:hover {
      transform: none;
      box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
    }

    .custom-button.waitlist-button {
      background: linear-gradient(135deg, #FF9800, #F57C00);
      box-shadow: 0 4px 15px rgba(255, 152, 0, 0.3);
    }

    .custom-button.waitlist-button:hover:not(:disabled) {
      background: linear-gradient(135deg, #F57C00, #EF6C00);
      box-shadow: 0 6px 20px rgba(255, 152, 0, 0.4);
    }

    .custom-button.waitlist-status {
      background: linear-gradient(135deg, #9C27B0, #7B1FA2);
      box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
    }

    .custom-button.confirm-waitlist-button {
      background: linear-gradient(135deg, #4CAF50, #388E3C);
      box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
      animation: pulse 2s infinite;
    }

    .custom-button.confirm-waitlist-button:hover:not(:disabled) {
      background: linear-gradient(135deg, #388E3C, #2E7D32);
      box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4);
    }

    @keyframes pulse {
      0% {
        box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
      }
      50% {
        box-shadow: 0 6px 25px rgba(76, 175, 80, 0.6);
      }
      100% {
        box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
      }
    }

    .custom-button.cancel-button {
      background: linear-gradient(135deg, #f44336, #d32f2f);
      box-shadow: 0 4px 15px rgba(244, 67, 54, 0.3);
    }

    .custom-button.cancel-button:hover:not(:disabled) {
      background: linear-gradient(135deg, #d32f2f, #c62828);
      box-shadow: 0 6px 20px rgba(244, 67, 54, 0.4);
    }

    .custom-button i {
      font-size: 1.2em;
    }

    .detail-item.waitlist-position {
      color: #9C27B0;
      font-weight: 600;
    }

    .detail-item.waitlist-position i {
      color: #9C27B0;
    }

    .empty-state {
      text-align: center;
      padding: 60px 20px;
      background: white;
      border-radius: 15px;
      box-shadow: 0 4px 15px rgba(0,0,0,0.1);
    }

    .empty-state i {
      font-size: 48px;
      color: #2196F3;
      margin-bottom: 20px;
    }

    .empty-state h3 {
      color: #2c3e50;
      font-size: 1.5em;
      margin-bottom: 10px;
    }

    .empty-state p {
      color: #666;
      font-size: 1.1em;
    }

    .error-message {
      display: flex;
      align-items: center;
      color: #e74c3c;
      padding: 15px 20px;
      margin-top: 20px;
      background: #fde2e2;
      border-radius: 10px;
      font-weight: 500;
    }

    .error-message i {
      margin-right: 10px;
      font-size: 1.2em;
    }

    @media (max-width: 768px) {
      .events-page {
        padding: 20px;
      }

      .events-grid {
        grid-template-columns: 1fr;
      }

      .events-header h1 {
        font-size: 2em;
    }

    .custom-button {
      padding: 10px 20px;
        font-size: 0.9rem;
      }
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(20px);
    }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .fade-in {
      animation: fadeIn 0.5s ease-out;
    }

    .hover-lift {
      transition: transform 0.3s ease, box-shadow 0.3s ease;
    }

    .hover-lift:hover {
      transform: translateY(-5px);
      box-shadow: 0 8px 25px rgba(0,0,0,0.15);
    }
  `]
})
export class EventListComponent implements OnInit {
  events: EventResponse[] = [];
  filteredEvents: EventResponse[] = [];
  error: string = '';
  isLoggedIn = false;
  isAdmin = false;
  userEmail: string = '';
  searchTerm: string = '';
  isLoading = true;

  filters = [
    { label: 'Tous', active: true, type: 'all' },
    { label: 'À venir', active: false, type: 'upcoming' },
    { label: 'En cours', active: false, type: 'ongoing' },
    { label: 'Passés', active: false, type: 'past' }
  ];

  constructor(
    private eventService: EventService,
    private invitationService: InvitationService,
    private waitlistService: WaitlistService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {
    // Recharge la liste à chaque navigation sur /events
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd && this.router.url.startsWith('/events')) {
        this.loadEvents();
      }
    });
  }

  async ngOnInit() {
    // Afficher le spinner pendant 2 secondes minimum
    setTimeout(() => {
      this.loadEvents();
    }, 2000);

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
        // Pour chaque événement, vérifier si l'utilisateur est inscrit
        if (this.isLoggedIn && this.userEmail) {
          events.forEach(event => {
            this.invitationService.isUserRegistered(event.id, this.userEmail).subscribe({
              next: (isRegistered) => {
                event.userRegistered = isRegistered;
              },
              error: (error) => {
                console.error(`Error checking registration for event ${event.id}:`, error);
                event.userRegistered = false;
              }
            });
            // Correction : forcer le statut annulé si présent dans la réponse
            if (event.userStatus === 'CANCELLED') {
              // Rien à faire, déjà bon
            } else if (event.userWaitlistStatus === 'CANCELLED') {
              event.userStatus = 'CANCELLED';
            }
          });
        }

        this.events = events;
        this.filterEvents();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading events:', error);
        this.isLoading = false;
        this.notificationService.show({
          message: 'Une erreur est survenue lors du chargement des événements.',
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  filterEvents() {
    let filtered = this.events;

    // Apply search filter
    if (this.searchTerm) {
      const search = this.searchTerm.toLowerCase();
      filtered = filtered.filter(event =>
        event.title.toLowerCase().includes(search) ||
        event.description?.toLowerCase().includes(search) ||
        event.location.toLowerCase().includes(search)
      );
    }

    // Apply status filter
    const activeFilter = this.filters.find(f => f.active);
    if (activeFilter && activeFilter.type !== 'all') {
      filtered = filtered.filter(event => {
        switch (activeFilter.type) {
          case 'upcoming':
            return this.isUpcoming(event.eventDate);
          case 'ongoing':
            return this.isOngoing(event.eventDate);
          case 'past':
            return this.isPast(event.eventDate);
          default:
            return true;
        }
      });
    }

    this.filteredEvents = filtered;
  }

  toggleFilter(filter: any) {
    this.filters.forEach(f => f.active = f === filter);
    this.filterEvents();
  }

  registerForEvent(event: EventResponse) {
    if (!this.isLoggedIn || !this.userEmail) {
      this.notificationService.show({
        message: 'Veuillez vous connecter pour vous inscrire à un événement.',
        type: 'error',
        duration: 5000
      });
      return;
    }

    // Rediriger vers la page de sélection des places
    this.router.navigate(['/events', event.id, 'select-seat']);
  }

  /**
   * Rejoindre la liste d'attente pour un événement
   */
  joinWaitlist(event: EventResponse) {
    if (!this.isLoggedIn || !this.userEmail) {
      this.notificationService.show({
        message: 'Veuillez vous connecter pour rejoindre la liste d\'attente.',
        type: 'error',
        duration: 5000
      });
      return;
    }

    console.log('=== DEBUT joinWaitlist ===');
    console.log('Event ID:', event.id);
    console.log('User Email:', this.userEmail);
    console.log('Event complet:', this.isEventFull(event));
    console.log('Liste d\'attente activée:', event.waitlistEnabled);

    this.waitlistService.joinWaitlist(Number(event.id)).subscribe({
      next: (response) => {
        console.log('=== SUCCÈS joinWaitlist ===');
        console.log('Réponse reçue:', response);

        this.notificationService.show({
          message: `Vous avez rejoint la liste d'attente ! Position : ${response.position}`,
          type: 'success',
          duration: 5000
        });

        // Mettre à jour l'événement localement
        event.userWaitlistPosition = response.position;
        event.waitlistCount = (event.waitlistCount || 0) + 1;

        console.log('Rechargement des événements...');
        // Recharger les événements pour avoir les données à jour
        this.loadEvents();
      },
      error: (error) => {
        console.error('=== ERREUR joinWaitlist ===');
        console.error('Erreur lors de l\'ajout à la liste d\'attente:', error);
        console.error('Détails de l\'erreur:', error.error);

        this.notificationService.show({
          message: error.error || 'Erreur lors de l\'ajout à la liste d\'attente.',
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  /**
   * Vérifier si un événement est complet
   */
  isEventFull(event: EventResponse): boolean {
    if (!event.maxCapacity) {
      console.log(`Événement ${event.title}: pas de maxCapacity définie`);
      return false;
    }
    const isFull = (event.confirmedParticipants || 0) >= event.maxCapacity;
    console.log(`Événement ${event.title}: ${event.confirmedParticipants || 0}/${event.maxCapacity} - Complet: ${isFull}`);
    return isFull;
  }

  /**
   * Vérifier si l'utilisateur a été notifié depuis la liste d'attente
   */
  isWaitlistNotified(event: EventResponse): boolean {
    return event.userWaitlistStatus === 'NOTIFIED';
  }

  /**
   * Confirmer une place depuis la liste d'attente
   */
  confirmWaitlistSpot(event: EventResponse) {
    if (!this.isLoggedIn || !this.userEmail) {
      this.notificationService.show({
        message: 'Veuillez vous connecter pour confirmer votre place.',
        type: 'error',
        duration: 5000
      });
      return;
    }

    this.waitlistService.confirmWaitlistSpot(Number(event.id)).subscribe({
      next: () => {
        this.notificationService.show({
          message: 'Votre place a été confirmée avec succès !',
          type: 'success',
          duration: 5000
        });

        // Mettre à jour l'événement localement
        event.userRegistered = true;
        event.userWaitlistPosition = undefined;
        (event as any).userWaitlistStatus = 'CONFIRMED';

        // Recharger les événements pour avoir les données à jour
        this.loadEvents();
      },
      error: (error) => {
        console.error('Erreur lors de la confirmation:', error);
        this.notificationService.show({
          message: error.error || 'Erreur lors de la confirmation de votre place.',
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  /**
   * Annuler l'inscription à un événement
   */
  cancelRegistration(event: EventResponse) {
    if (!this.isLoggedIn || !this.userEmail) {
      this.notificationService.show({
        message: 'Veuillez vous connecter pour annuler votre inscription.',
        type: 'error',
        duration: 5000
      });
      return;
    }

    if (confirm('Êtes-vous sûr de vouloir annuler votre inscription à cet événement ?')) {
      this.eventService.cancelUserRegistration(event.id, this.userEmail!).subscribe({
        next: () => {
          this.notificationService.show({
            message: 'Votre inscription a été annulée avec succès.',
            type: 'success',
            duration: 5000
          });

          event.userRegistered = false;
          event.userHasPendingInvitation = false;
          event.userStatus = 'CANCELLED'; // Affiche immédiatement le bouton bloqué

          // Optionnel : tu peux aussi recharger la liste si tu veux synchroniser avec le backend
          // this.loadEvents();
        },
        error: (error) => {
          console.error('Erreur lors de l\'annulation:', error);
          this.notificationService.show({
            message: error.error || 'Erreur lors de l\'annulation de votre inscription.',
            type: 'error',
            duration: 5000
          });
        }
      });
    }
  }

  isUpcoming(dateString: string): boolean {
    const eventDate = new Date(dateString);
    const now = new Date();
    // Réinitialiser les heures pour comparer uniquement les dates
    const eventDateOnly = new Date(eventDate.getFullYear(), eventDate.getMonth(), eventDate.getDate());
    const nowDateOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    return eventDateOnly > nowDateOnly;
  }

  isOngoing(dateString: string): boolean {
    const eventDate = new Date(dateString);
    const now = new Date();

    // Vérifier si c'est le même jour en comparant année, mois et jour
    return eventDate.getFullYear() === now.getFullYear() &&
           eventDate.getMonth() === now.getMonth() &&
           eventDate.getDate() === now.getDate();
  }

  isPast(dateString: string): boolean {
    const eventDate = new Date(dateString);
    const now = new Date();
    // Réinitialiser les heures pour comparer uniquement les dates
    const eventDateOnly = new Date(eventDate.getFullYear(), eventDate.getMonth(), eventDate.getDate());
    const nowDateOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    return eventDateOnly < nowDateOnly;
  }

  getEventStatus(dateString: string): string {
    if (this.isUpcoming(dateString)) return 'À venir';
    if (this.isOngoing(dateString)) return 'En cours';
    return 'Passé';
  }

  formatDateDay(dateString: string): string {
    const date = new Date(dateString);
    return date.getDate().toString();
  }

  formatDateMonth(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', { month: 'short' });
  }

  formatTime(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
