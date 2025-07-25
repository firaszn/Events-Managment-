import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AdminUserService, UserDetails } from '../services/admin-user.service';
import { AuthManagerService } from '../../user/core/services/auth-manager.service';
import { AdminEventService, EventDetails } from '../services/admin-event.service';
import { AdminInvitationService, InvitationDetails } from '../services/admin-invitation.service';
import { Subscription } from 'rxjs';
import { NotificationService } from '../../notification/services/notification.service';
import { NotificationComponent } from '../../notification/components/notification.component';
import { InvitationService } from '../../invitation/services/invitation.service';
import { WaitlistService } from '../../event/services/waitlist.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NotificationComponent],
  styleUrls: ['./admin-dashboard.component.scss'],
  template: `
    <div class="admin-dashboard">
      <app-notification></app-notification>

      <!-- Sidebar -->
      <div class="sidebar" [class.collapsed]="isSidebarCollapsed">
        <div class="sidebar-header">
          <div class="logo-container">
            <i class="fas fa-shield-alt"></i>
            <h3>Event Manager</h3>
          </div>
          <button class="toggle-btn" (click)="toggleSidebar()">
            <i class="fas" [class.fa-chevron-left]="!isSidebarCollapsed" [class.fa-chevron-right]="isSidebarCollapsed"></i>
          </button>
        </div>
        <div class="sidebar-menu">
          <div class="menu-item" (click)="setActiveMenu('users')" [class.active]="activeMenu === 'users'">
            <i class="fas fa-users"></i>
            <span>Utilisateurs</span>
            <div class="menu-indicator"></div>
          </div>
          <div class="menu-item" (click)="setActiveMenu('events')" [class.active]="activeMenu === 'events'">
            <i class="fas fa-calendar"></i>
            <span>Événements</span>
            <div class="menu-indicator"></div>
          </div>
          <div class="menu-item" (click)="setActiveMenu('invitations')" [class.active]="activeMenu === 'invitations'">
            <i class="fas fa-envelope"></i>
            <span>Invitations</span>
            <span class="notification-badge" *ngIf="hasNewInvitations && activeMenu !== 'invitations'">{{ newInvitationsCount > 99 ? '99+' : newInvitationsCount }}</span>
            <div class="menu-indicator"></div>
          </div>
          <div class="menu-item logout" (click)="logout()">
            <i class="fas fa-sign-out-alt"></i>
            <span>Déconnexion</span>
          </div>
        </div>
      </div>

      <!-- Main Content -->
      <div class="main-content" [class.expanded]="isSidebarCollapsed">
        <div class="content-header">
          <div class="header-left">
            <h2>{{ getContentTitle() }}</h2>
            <p class="subtitle">{{ getContentSubtitle() }}</p>
          </div>
          <div class="header-actions">
            <div class="search-box">
              <i class="fas fa-search"></i>
              <input type="text" placeholder="Rechercher..." (input)="onSearch($event)">
            </div>
            <button class="refresh-btn" (click)="refreshData()" title="Rafraîchir">
              <i class="fas fa-sync-alt"></i>
            </button>
            <button class="add-btn" *ngIf="activeMenu !== 'users'" (click)="onAdd()">
              <i class="fas fa-plus"></i>
              <span>Nouveau {{ getAddButtonText() }}</span>
            </button>
          </div>
        </div>

        <!-- Stats Section -->
        <div class="stats-container">
          <div class="stat-card">
            <div class="stat-icon total">
              <i class="fas" [class.fa-users]="activeMenu === 'users'" [class.fa-calendar]="activeMenu === 'events'" [class.fa-envelope]="activeMenu === 'invitations'"></i>
            </div>
            <div class="stat-info">
              <h3>{{ getStatsValue() }}</h3>
              <p>{{ getStatsTitle() }}</p>
              <span class="trend up">↑ 12% ce mois</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon active">
              <i class="fas" [class.fa-user-check]="activeMenu === 'users'" [class.fa-calendar-check]="activeMenu === 'events'" [class.fa-envelope-open]="activeMenu === 'invitations'"></i>
            </div>
            <div class="stat-info">
              <h3>{{ getActiveStatsValue() }}</h3>
              <p>{{ getActiveStatsTitle() }}</p>
              <span class="trend up">↑ 8% ce mois</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon inactive">
              <i class="fas" [class.fa-user-times]="activeMenu === 'users'" [class.fa-calendar-times]="activeMenu === 'events'" [class.fa-envelope-open-text]="activeMenu === 'invitations'"></i>
            </div>
            <div class="stat-info">
              <h3>{{ getInactiveStatsValue() }}</h3>
              <p>{{ getInactiveStatsTitle() }}</p>
              <span class="trend down">↓ 3% ce mois</span>
            </div>
          </div>
        </div>

        <!-- Table Section -->
        <div class="table-container">
          <!-- Users Table -->
          <table *ngIf="activeMenu === 'users'">
            <thead>
              <tr>
                <th>Utilisateur</th>
                <th>Email</th>
                <th>Nom</th>
                <th>Prénom</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let user of users">
                <td>{{ user.username }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.lastName }}</td>
                <td>{{ user.firstName }}</td>
                <td>
                  <span class="status" [class.active]="isUserEnabled(user)" [class.inactive]="!isUserEnabled(user)">
                    {{ isUserEnabled(user) ? 'Actif' : 'Inactif' }}
                  </span>
                </td>
                <td class="actions">
                  <button class="action-btn" (click)="toggleUserStatus(user)" title="{{ isUserEnabled(user) ? 'Désactiver' : 'Activer' }}">
                    <i class="fas" [class.fa-toggle-on]="isUserEnabled(user)" [class.fa-toggle-off]="!isUserEnabled(user)"></i>
                  </button>
                  <button class="action-btn edit" (click)="editUser(user)" title="Modifier">
                    <i class="fas fa-edit"></i>
                  </button>
                  <button class="action-btn delete" (click)="deleteUser(user)" title="Supprimer">
                    <i class="fas fa-trash"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Events Table -->
          <table *ngIf="activeMenu === 'events'">
            <thead>
              <tr>
                <th>Titre</th>
                <th>Description</th>
                <th>Lieu</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let event of events">
                <td>{{ event.title }}</td>
                <td>{{ event.description }}</td>
                <td>{{ event.location }}</td>
                <td>{{ event.eventDate | date:'dd/MM/yyyy HH:mm' }}</td>
                <td class="actions">
                  <button class="action-btn edit" (click)="editEvent(event)" title="Modifier">
                    <i class="fas fa-edit"></i>
                  </button>
                  <button class="action-btn delete" (click)="deleteEvent(event)" title="Supprimer">
                    <i class="fas fa-trash"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Invitations Table -->
          <div class="table-container" *ngIf="activeMenu === 'invitations'">
            <table>
              <thead>
                <tr>
                  <th>ÉVÉNEMENT</th>
                  <th>UTILISATEUR</th>
                  <th>STATUT</th>
                  <th>PLACE</th>
                  <th>DATE D'ENVOI</th>
                  <th>ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let invitation of invitations">
                  <td>{{ invitation.eventTitle }}</td>
                  <td>{{ invitation.userEmail }}</td>
                  <td>
                    <span class="status"
                          [class.pending]="invitation.status === 'PENDING'"
                          [class.confirmed]="invitation.status === 'CONFIRMED'"
                          [class.cancelled]="invitation.status === 'CANCELLED'"
                          [class.waitlist]="invitation.status === 'WAITLIST'">
                      {{ getStatusDisplayText(invitation.status) }}
                    </span>
                  </td>
                  <td>{{ invitation.seatInfo ? 'Rangée ' + invitation.seatInfo.row + ', Place ' + invitation.seatInfo.number : 'N/A' }}</td>
                  <td>{{ invitation.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td class="actions">
                    <button class="action-btn confirm"
                            *ngIf="invitation.status === 'PENDING'"
                            (click)="confirmInvitation(invitation.id)"
                            title="Confirmer et envoyer email">
                      <i class="fas fa-check"></i>
                    </button>
                    <span class="auto-managed"
                          *ngIf="invitation.status === 'WAITLIST'"
                          title="Gestion automatique - sera confirmé automatiquement lors d'une place libre">
                      <i class="fas fa-robot"></i> Auto
                    </span>
                    <span class="confirmed-badge"
                          *ngIf="invitation.status === 'CONFIRMED'"
                          title="Invitation confirmée">
                      <i class="fas fa-check-circle"></i> Confirmé
                    </span>
                    <button class="action-btn delete"
                            (click)="deleteInvitation(invitation.id)"
                            title="Supprimer">
                      <i class="fas fa-trash"></i>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal de création d'événement -->
    <div class="modal-overlay" *ngIf="showCreateEventModal" (click)="closeCreateEventModal()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Créer un Nouvel Événement</h3>
          <button class="close-btn" (click)="closeCreateEventModal()">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <form (ngSubmit)="createEvent()" #eventForm="ngForm">
          <div class="form-group">
            <label for="title">Titre de l'événement *</label>
            <input
              type="text"
              id="title"
              name="title"
              [(ngModel)]="newEvent.title"
              required
              maxlength="100"
              placeholder="Entrez le titre de l'événement"
              class="form-control">
          </div>

          <div class="form-group">
            <label for="description">Description</label>
            <textarea
              id="description"
              name="description"
              [(ngModel)]="newEvent.description"
              rows="4"
              maxlength="500"
              placeholder="Décrivez votre événement"
              class="form-control"></textarea>
          </div>

          <div class="form-group">
            <label for="location">Lieu *</label>
            <input
              type="text"
              id="location"
              name="location"
              [(ngModel)]="newEvent.location"
              required
              maxlength="200"
              placeholder="Entrez le lieu de l'événement"
              class="form-control">
          </div>

          <div class="form-group">
            <label for="eventDate">Date et heure *</label>
            <input
              type="datetime-local"
              id="eventDate"
              name="eventDate"
              [(ngModel)]="newEvent.eventDate"
              required
              [min]="getMinDate()"
              class="form-control">
          </div>

          <!-- Capacité fixée à 5 places et liste d'attente activée automatiquement -->

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" (click)="closeCreateEventModal()" [disabled]="isCreatingEvent">
              Annuler
            </button>
            <button type="submit" class="btn btn-primary" [disabled]="!eventForm.form.valid || isCreatingEvent">
              <i class="fas fa-spinner fa-spin" *ngIf="isCreatingEvent"></i>
              <i class="fas fa-plus" *ngIf="!isCreatingEvent"></i>
              Créer l'événement
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal de modification d'événement -->
    <div class="modal-overlay" *ngIf="showEditEventModal" (click)="closeEditEventModal()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Modifier l'Événement</h3>
          <button class="close-btn" (click)="closeEditEventModal()">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <form (ngSubmit)="updateEvent()" #editEventForm="ngForm" *ngIf="editingEvent">
          <div class="form-group">
            <label for="editTitle">Titre de l'événement *</label>
            <input
              type="text"
              id="editTitle"
              name="editTitle"
              [(ngModel)]="editingEvent.title"
              required
              maxlength="100"
              placeholder="Entrez le titre de l'événement"
              class="form-control">
          </div>

          <div class="form-group">
            <label for="editDescription">Description</label>
            <textarea
              id="editDescription"
              name="editDescription"
              [(ngModel)]="editingEvent.description"
              rows="4"
              maxlength="500"
              placeholder="Décrivez votre événement"
              class="form-control"></textarea>
          </div>

          <div class="form-group">
            <label for="editLocation">Lieu *</label>
            <input
              type="text"
              id="editLocation"
              name="editLocation"
              [(ngModel)]="editingEvent.location"
              required
              maxlength="200"
              placeholder="Entrez le lieu de l'événement"
              class="form-control">
          </div>

          <div class="form-group">
            <label for="editEventDate">Date et heure *</label>
            <input
              type="datetime-local"
              id="editEventDate"
              name="editEventDate"
              [(ngModel)]="editingEvent.eventDate"
              required
              [min]="getMinDate()"
              class="form-control">
          </div>

          <!-- Capacité fixée à 5 places et liste d'attente activée automatiquement -->

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" (click)="closeEditEventModal()" [disabled]="isUpdatingEvent">
              Annuler
            </button>
            <button type="submit" class="btn btn-primary" [disabled]="!editEventForm.form.valid || isUpdatingEvent">
              <i class="fas fa-spinner fa-spin" *ngIf="isUpdatingEvent"></i>
              <i class="fas fa-save" *ngIf="!isUpdatingEvent"></i>
               Modifier l'événement'
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  activeMenu: 'users' | 'events' | 'invitations' = 'users';
  isSidebarCollapsed = false;
  users: UserDetails[] = [];
  events: EventDetails[] = [];
  invitations: InvitationDetails[] = [];
  private subscription: Subscription = new Subscription();

  // Propriétés pour la création d'événement
  showCreateEventModal = false;
  newEvent: Partial<EventDetails> = {};
  isCreatingEvent = false;

  // Propriétés pour la modification d'événement
  showEditEventModal = false;
  editingEvent: EventDetails | null = null;
  isUpdatingEvent = false;

  // Propriétés pour les notifications en temps réel
  hasNewInvitations = false;
  newInvitationsCount = 0;

  constructor(
    private router: Router,
    private adminUserService: AdminUserService,
    private adminEventService: AdminEventService,
    private adminInvitationService: AdminInvitationService,
    private authManager: AuthManagerService,
    private notificationService: NotificationService,
    private invitationService: InvitationService,
    private waitlistService: WaitlistService
  ) {}

  ngOnInit() {
    this.loadInitialData();
    this.setupRealTimeInvitationUpdates();
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  setupRealTimeInvitationUpdates() {
    // Écouter les créations d'invitation en temps réel
    this.subscription.add(
      this.invitationService.invitationCreated$.subscribe({
        next: (newInvitation) => {
          console.log('Nouvelle invitation créée:', newInvitation);

          // Rafraîchir immédiatement les invitations
          this.adminInvitationService.triggerRefresh();

          // Si l'admin n'est pas déjà sur la section invitations, lui suggérer de voir
          if (this.activeMenu !== 'invitations') {
            // Marquer qu'il y a de nouvelles invitations
            this.hasNewInvitations = true;
            this.newInvitationsCount++;

            this.notificationService.show({
              message: `Nouvelle inscription : ${newInvitation.userEmail} pour "${newInvitation.eventTitle}" - Place: Rangée ${newInvitation.seatInfo.row}, Siège ${newInvitation.seatInfo.number} | Cliquez pour voir les invitations`,
              type: 'info',
              duration: 10000
            });
          } else {
            // Si déjà sur la section invitations, notification plus courte
            this.notificationService.show({
              message: `✅ ${newInvitation.userEmail} s'est inscrit - Rangée ${newInvitation.seatInfo.row}, Siège ${newInvitation.seatInfo.number}`,
              type: 'success',
              duration: 5000
            });
          }
        },
        error: (error) => {
          console.error('Erreur dans l\'écoute des événements d\'invitation:', error);
        }
      })
    );
  }

  loadInitialData() {
    this.loadUsers();
    this.loadEvents();
    this.subscribeToInvitations();
  }

  private subscribeToInvitations() {
    this.subscription.add(
      this.adminInvitationService.getRefreshObservable().subscribe({
        next: (invitations) => {
          const previousCount = this.invitations.length;

          this.invitations = invitations.sort((a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );

          // Si c'est pas la première charge et qu'il y a de nouvelles invitations
          const newCount = this.invitations.length;
          if (previousCount > 0 && newCount > previousCount) {
            const addedCount = newCount - previousCount;
            console.log(`${addedCount} nouvelle(s) invitation(s) détectée(s) via polling`);
          }
        },
        error: (error) => {
          console.error('Error loading invitations:', error);
          this.notificationService.show({
            message: 'Erreur lors du chargement des invitations',
            type: 'error',
            duration: 5000
          });
        }
      })
    );
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  loadUsers() {
    this.adminUserService.getUsers().subscribe({
      next: (users: UserDetails[]) => {
        this.users = users;
      },
      error: (error: Error) => {
        console.error('Erreur lors du chargement des utilisateurs:', error);
      }
    });
  }

  loadEvents() {
    this.adminEventService.getEvents().subscribe({
      next: (events: EventDetails[]) => {
        // Log temporaire pour débogage - à supprimer en production
        console.log('📊 Événements chargés pour débogage:', events.map(e => ({
          title: e.title,
          confirmedParticipants: e.confirmedParticipants,
          maxCapacity: e.maxCapacity,
          waitlistCount: e.waitlistCount
        })));

        this.events = events;
      },
      error: (error: Error) => {
        console.error('Erreur lors du chargement des événements:', error);
      }
    });
  }

  refreshData() {
    switch (this.activeMenu) {
      case 'users':
    this.loadUsers();
        break;
      case 'events':
    this.loadEvents();
        break;
  }
  }

  onSearch(event: any) {
    const searchTerm = event.target.value.toLowerCase();

    switch (this.activeMenu) {
      case 'users':
    if (searchTerm) {
      this.users = this.users.filter(user =>
        user.username.toLowerCase().includes(searchTerm) ||
        user.email.toLowerCase().includes(searchTerm) ||
        user.firstName.toLowerCase().includes(searchTerm) ||
        user.lastName.toLowerCase().includes(searchTerm)
      );
    } else {
      this.loadUsers();
        }
        break;

      case 'events':
        if (searchTerm) {
          this.events = this.events.filter(event =>
            event.title.toLowerCase().includes(searchTerm) ||
            event.description.toLowerCase().includes(searchTerm) ||
            event.location.toLowerCase().includes(searchTerm) ||
            event.organizer?.toLowerCase().includes(searchTerm)
          );
        } else {
          this.loadEvents();
        }
        break;

      case 'invitations':
        if (searchTerm) {
          this.invitations = this.invitations.filter(invitation =>
            invitation.eventTitle.toLowerCase().includes(searchTerm) ||
            invitation.userEmail.toLowerCase().includes(searchTerm) ||
            invitation.status.toLowerCase().includes(searchTerm)
          );
        } else {
          this.subscribeToInvitations();
        }
        break;
    }
  }

  onAdd() {
    if (this.activeMenu === 'events') {
      this.openCreateEventModal();
    }
    // Implémenter la logique d'ajout selon le menu actif pour les autres cas
  }

  editUser(user: UserDetails) {
    // Implémenter la logique de modification
    console.log('Édition de l\'utilisateur:', user);
  }

  getTotalUsers(): number {
    return this.users.length;
  }

  getActiveUsers(): number {
    return this.users.filter(user => user.enabled).length;
  }

  getInactiveUsers(): number {
    return this.users.filter(user => !user.enabled).length;
  }

  getStatsTitle(): string {
    switch (this.activeMenu) {
      case 'users':
        return 'Total Utilisateurs';
      case 'events':
        return 'Total Événements';
      case 'invitations':
        return 'Total Invitations';
      default:
        return '';
    }
  }

  getStatsValue(): number {
    switch (this.activeMenu) {
      case 'users':
        return this.users.length;
      case 'events':
        return this.events.length;
      case 'invitations':
        return this.invitations.length;
      default:
        return 0;
    }
  }

  getActiveStatsTitle(): string {
    switch (this.activeMenu) {
      case 'users':
        return 'Utilisateurs Actifs';
      case 'events':
        return 'Événements à venir';
      case 'invitations':
        return 'Invitations Acceptées';
      default:
        return '';
    }
  }

  getActiveStatsValue(): number {
    switch (this.activeMenu) {
      case 'users':
        return this.users.filter(user => user.enabled).length;
      case 'events':
        return this.events.filter(event => this.isUpcoming(event.eventDate)).length;
      case 'invitations':
        return this.invitations.filter(inv => inv.status === 'CONFIRMED').length;
      default:
        return 0;
    }
  }

  getInactiveStatsTitle(): string {
    switch (this.activeMenu) {
      case 'users':
        return 'Utilisateurs Inactifs';
      case 'events':
        return 'Événements passés';
      case 'invitations':
        return 'Invitations En attente';
      default:
        return '';
    }
  }

  getInactiveStatsValue(): number {
    switch (this.activeMenu) {
      case 'users':
        return this.users.filter(user => !user.enabled).length;
      case 'events':
        return this.events.filter(event => this.isPast(event.eventDate)).length;
      case 'invitations':
        return this.invitations.filter(inv => inv.status === 'PENDING').length;
      default:
        return 0;
    }
  }

  isUserEnabled(user: UserDetails): boolean {
    return user.enabled;
  }

  getUserInitials(user: UserDetails): string {
    const firstName = user?.firstName || '';
    const lastName = user?.lastName || '';
    return `${firstName.charAt(0)}${lastName.charAt(0)}`;
  }

  setActiveMenu(menu: 'users' | 'events' | 'invitations') {
    this.activeMenu = menu;

    // Réinitialiser le badge si on va sur invitations
    if (menu === 'invitations') {
      this.hasNewInvitations = false;
      this.newInvitationsCount = 0;
    }

    if (menu === 'users') {
      this.loadUsers();
    } else if (menu === 'events') {
      this.loadEvents();
    }
    // No need to load invitations here as they are automatically updated through polling
  }

  toggleUserStatus(user: UserDetails) {
    const newStatus = !user.enabled;
    this.adminUserService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        user.enabled = newStatus;
        // Rafraîchir la liste des utilisateurs
        this.loadUsers();
      },
      error: (error) => {
        console.error('Erreur lors de la modification du statut:', error);
      }
    });
  }

  deleteUser(user: UserDetails) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer l'utilisateur ${user.username} ?`)) {
      this.adminUserService.deleteUser(user.id).subscribe({
        next: () => {
          this.users = this.users.filter(u => u.id !== user.id);
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
        }
      });
    }
  }

  createEvent() {
    if (!this.newEvent.title || !this.newEvent.location || !this.newEvent.eventDate) {
      this.notificationService.show({
        message: 'Veuillez remplir tous les champs obligatoires',
        type: 'error',
        duration: 5000
      });
      return;
    }

    this.isCreatingEvent = true;

    // Convertir la date en format ISO en préservant l'heure locale
    const eventData = {
      title: this.newEvent.title,
      description: this.newEvent.description || '',
      location: this.newEvent.location,
      eventDate: this.formatDateToLocalISO(this.newEvent.eventDate!),
      maxCapacity: this.newEvent.maxCapacity || undefined,
      waitlistEnabled: this.newEvent.waitlistEnabled || false
    };

    this.adminEventService.createEvent(eventData).subscribe({
      next: (createdEvent) => {
        this.isCreatingEvent = false;
        this.showCreateEventModal = false;
        this.loadEvents(); // Recharger la liste des événements
        this.newEvent = {}; // Réinitialiser le formulaire

        this.notificationService.show({
          message: 'Événement créé avec succès',
          type: 'success',
          duration: 5000
        });
        },
        error: (error) => {
        this.isCreatingEvent = false;
          console.error('Erreur lors de la création de l\'événement:', error);

        let errorMessage = 'Erreur lors de la création de l\'événement';

        // Gérer les erreurs spécifiques du backend
        if (error.status === 400 && error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.status === 403) {
          errorMessage = 'Vous n\'avez pas les permissions pour créer un événement';
        } else if (error.status === 401) {
          errorMessage = 'Votre session a expiré, veuillez vous reconnecter';
    }

        this.notificationService.show({
          message: errorMessage,
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  deleteEvent(event: EventDetails) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer l'événement "${event.title}" ?`)) {
      this.adminEventService.deleteEvent(event.id).subscribe({
        next: () => {
          this.events = this.events.filter(e => e.id !== event.id);
        },
        error: (error) => {
          console.error('Erreur lors de la suppression de l\'événement:', error);
        }
      });
    }
  }

  deleteInvitation(invitationId: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette invitation ?')) {
      this.adminInvitationService.deleteInvitation(invitationId).subscribe({
        next: () => {
          this.invitations = this.invitations.filter(i => i.id !== invitationId);
          this.notificationService.show({
            message: 'Invitation supprimée avec succès',
            type: 'success',
            duration: 5000
          });
          this.adminInvitationService.triggerRefresh();
        },
        error: (error) => {
          console.error('Error deleting invitation:', error);
          this.notificationService.show({
            message: 'Erreur lors de la suppression de l\'invitation',
            type: 'error',
            duration: 5000
          });
        }
      });
    }
  }

  confirmInvitation(invitationId: number) {
    this.adminInvitationService.confirmInvitation(invitationId).subscribe({
      next: (updatedInvitation) => {
        const index = this.invitations.findIndex(inv => inv.id === invitationId);
        if (index !== -1) {
          this.invitations[index] = updatedInvitation;
        }
        this.notificationService.showSuccess('Invitation confirmée avec succès');
      },
      error: (error) => {
        console.error('Erreur lors de la confirmation de l\'invitation:', error);
        this.notificationService.showError('Erreur lors de la confirmation de l\'invitation');
      }
    });
  }



  getStatusDisplayText(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'EN ATTENTE';
      case 'CONFIRMED':
        return 'CONFIRMÉ';
      case 'CANCELLED':
        return 'ANNULÉ';
      case 'WAITLIST':
        return 'LISTE D\'ATTENTE';
      default:
        return status;
    }
  }

  getContentTitle(): string {
    switch (this.activeMenu) {
      case 'users':
        return 'Gestion des Utilisateurs';
      case 'events':
        return 'Gestion des Événements';
      case 'invitations':
        return 'Gestion des Invitations';
      default:
        return '';
    }
  }

  getContentSubtitle(): string {
    switch (this.activeMenu) {
      case 'users':
        return 'Gérez les utilisateurs, leurs rôles et leurs permissions';
      case 'events':
        return 'Créez et gérez les événements de votre plateforme';
      case 'invitations':
        return 'Suivez et gérez les invitations aux événements';
      default:
        return '';
    }
  }

  getAddButtonText(): string {
    switch (this.activeMenu) {
      case 'events':
        return 'Événement';
      case 'invitations':
        return 'Invitation';
      default:
        return '';
    }
  }

  async logout() {
    try {
      await this.authManager.logout();
      this.router.navigate(['/login']);
    } catch (error) {
      console.error('Erreur lors de la déconnexion:', error);
      // Rediriger quand même en cas d'erreur
      this.router.navigate(['/login']);
    }
  }

  editEvent(event: EventDetails) {
    // Créer une copie de l'événement pour éviter de modifier l'original
    this.editingEvent = {
      ...event,
      eventDate: this.formatDateForInputLocal(event.eventDate)
    };
    this.showEditEventModal = true;
    this.isUpdatingEvent = false;
  }

  updateEvent() {
    if (!this.editingEvent?.title || !this.editingEvent?.location || !this.editingEvent?.eventDate) {
      this.notificationService.show({
        message: 'Veuillez remplir tous les champs obligatoires',
        type: 'error',
        duration: 5000
      });
      return;
    }

    this.isUpdatingEvent = true;

    const eventData = {
      title: this.editingEvent.title,
      description: this.editingEvent.description || '',
      location: this.editingEvent.location,
      eventDate: this.formatDateToLocalISO(this.editingEvent.eventDate!),
      maxCapacity: this.editingEvent.maxCapacity || undefined,
      waitlistEnabled: this.editingEvent.waitlistEnabled || false
    };

    this.adminEventService.updateEvent(this.editingEvent.id, eventData).subscribe({
      next: (updatedEvent) => {
        this.isUpdatingEvent = false;
        this.showEditEventModal = false;
        this.loadEvents(); // Recharger la liste des événements
        this.editingEvent = null; // Réinitialiser l'événement en cours de modification

        this.notificationService.show({
          message: 'Événement modifié avec succès',
          type: 'success',
          duration: 5000
        });
      },
      error: (error) => {
        this.isUpdatingEvent = false;
        console.error('Erreur lors de la modification de l\'événement:', error);

        let errorMessage = 'Erreur lors de la modification de l\'événement';

        // Gérer les erreurs spécifiques du backend
        if (error.status === 400 && error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.status === 403) {
          errorMessage = 'Vous n\'avez pas les permissions pour modifier cet événement';
        } else if (error.status === 401) {
          errorMessage = 'Votre session a expiré, veuillez vous reconnecter';
        }

        this.notificationService.show({
          message: errorMessage,
          type: 'error',
          duration: 5000
        });
      }
    });
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

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatDateForInput(dateString: string): string {
    const date = new Date(dateString);
    return date.toISOString().slice(0, 16); // YYYY-MM-DDTHH:MM
  }

  /**
   * Redistribuer manuellement des places depuis la liste d'attente
   */
  redistributeSlots(event: EventDetails, slots: number) {
    this.waitlistService.redistributeSlots(Number(event.id), slots).subscribe({
      next: () => {
        this.notificationService.show({
          message: `${slots} place(s) redistribuée(s) avec succès pour "${event.title}"`,
          type: 'success',
          duration: 5000
        });
        // Rafraîchir les données
        this.loadEvents();
      },
      error: (error) => {
        console.error('Erreur lors de la redistribution:', error);
        this.notificationService.show({
          message: 'Erreur lors de la redistribution des places',
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  /**
   * Convertit une date locale en format ISO en préservant l'heure locale
   * au lieu de convertir en UTC
   */
  private formatDateToLocalISO(dateString: string): string {
    const date = new Date(dateString);
    const timezoneOffset = date.getTimezoneOffset() * 60000; // Offset en millisecondes
    const localISOTime = new Date(date.getTime() - timezoneOffset).toISOString();
    return localISOTime;
  }

  /**
   * Formate une date pour l'input datetime-local en préservant l'heure locale
   */
  private formatDateForInputLocal(dateString: string): string {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  getMinDate(): string {
    const now = new Date();
    return now.toISOString().slice(0, 16); // YYYY-MM-DDTHH:MM
  }

  openCreateEventModal() {
    this.showCreateEventModal = true;
    this.newEvent = {
      maxCapacity: 5,
      waitlistEnabled: true
    }; // Valeurs par défaut
    this.isCreatingEvent = false;
  }

  closeCreateEventModal() {
    this.showCreateEventModal = false;
  }

  closeEditEventModal() {
    this.showEditEventModal = false;
    this.editingEvent = null; // Clear editing event data
    this.isUpdatingEvent = false;
  }
}
