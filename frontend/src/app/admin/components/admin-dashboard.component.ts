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
    <div class="admin-dashboard" [class.dark-theme]="isDarkTheme" [class.compact]="isCompactMode">
      <app-notification></app-notification>

      <!-- Sidebar -->
      <div class="sidebar" [class.collapsed]="isSidebarCollapsed">
        <div class="sidebar-header">
          <div class="logo-container">
            <img class="brand-logo" src="assets/images/ai-summit.jpg" alt="Logo" (error)="hideBrandLogo($event)" />
            <i class="fas fa-shield-alt"></i>
            <h3>Event Manager</h3>
          </div>
          <button class="toggle-btn" (click)="toggleSidebar()">
            <i class="fas" [class.fa-chevron-left]="!isSidebarCollapsed" [class.fa-chevron-right]="isSidebarCollapsed"></i>
          </button>
        </div>
        <div class="sidebar-menu">
          <div class="menu-item" (click)="setActiveMenu('overview')" [class.active]="activeMenu === 'overview'">
            <i class="fas fa-home"></i>
            <span>Tableau de bord</span>
            <div class="menu-indicator"></div>
          </div>
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
          <div class="menu-item" (click)="setActiveMenu('waitlist')" [class.active]="activeMenu === 'waitlist'">
            <i class="fas fa-chair"></i>
            <span>Liste d'attente</span>
            <div class="menu-indicator"></div>
          </div>
          <div class="menu-item" (click)="setActiveMenu('analytics')" [class.active]="activeMenu === 'analytics'">
            <i class="fas fa-chart-line"></i>
            <span>Analytique</span>
            <div class="menu-indicator"></div>
          </div>
          <div class="menu-item" (click)="setActiveMenu('health')" [class.active]="activeMenu === 'health'">
            <i class="fas fa-heartbeat"></i>
            <span>Santé Système</span>
            <div class="menu-indicator"></div>
          </div>
          <div class="menu-item" (click)="setActiveMenu('settings')" [class.active]="activeMenu === 'settings'">
            <i class="fas fa-cog"></i>
            <span>Paramètres</span>
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
        <div class="bg-effects">
          <span class="orb o1"></span>
          <span class="orb o2"></span>
          <span class="orb o3"></span>
        </div>
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
            <button class="theme-btn" (click)="toggleTheme()" [attr.aria-pressed]="isDarkTheme" title="Basculer le thème">
              <i class="fas" [class.fa-moon]="!isDarkTheme" [class.fa-sun]="isDarkTheme"></i>
            </button>
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
              <i class="fas" [class.fa-tachometer-alt]="activeMenu === 'overview'" [class.fa-users]="activeMenu === 'users'" [class.fa-calendar]="activeMenu === 'events'" [class.fa-envelope]="activeMenu === 'invitations'"></i>
            </div>
            <div class="stat-info">
              <h3>{{ animatedStats.total }}</h3>
              <p>{{ getStatsTitle() }}</p>
              <span class="trend up">↑ 12% ce mois</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon active">
              <i class="fas" [class.fa-star]="activeMenu === 'overview'" [class.fa-user-check]="activeMenu === 'users'" [class.fa-calendar-check]="activeMenu === 'events'" [class.fa-envelope-open]="activeMenu === 'invitations'"></i>
            </div>
            <div class="stat-info">
              <h3>{{ animatedStats.active }}</h3>
              <p>{{ getActiveStatsTitle() }}</p>
              <span class="trend up">↑ 8% ce mois</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon inactive">
              <i class="fas" [class.fa-bell]="activeMenu === 'overview'" [class.fa-user-times]="activeMenu === 'users'" [class.fa-calendar-times]="activeMenu === 'events'" [class.fa-envelope-open-text]="activeMenu === 'invitations'"></i>
            </div>
            <div class="stat-info">
              <h3>{{ animatedStats.inactive }}</h3>
              <p>{{ getInactiveStatsTitle() }}</p>
              <span class="trend down">↓ 3% ce mois</span>
            </div>
          </div>
        </div>

        <!-- Overview Section -->
        <div class="overview-grid" *ngIf="activeMenu === 'overview'">
          <div class="kpi-card gradient-blue">
            <div class="kpi-content">
              <i class="fas fa-users"></i>
              <div>
                <h4>Utilisateurs</h4>
                <p>{{ animatedKpi.users }}</p>
              </div>
            </div>
          </div>
          <div class="kpi-card gradient-green">
            <div class="kpi-content">
              <i class="fas fa-calendar"></i>
              <div>
                <h4>Événements</h4>
                <p>{{ animatedKpi.events }}</p>
              </div>
            </div>
          </div>
          <div class="kpi-card gradient-orange">
            <div class="kpi-content">
              <i class="fas fa-envelope"></i>
              <div>
                <h4>Invitations</h4>
                <p>{{ animatedKpi.invitations }}</p>
              </div>
            </div>
          </div>
          <div class="kpi-card gradient-purple">
            <div class="kpi-content">
              <i class="fas fa-chair"></i>
              <div>
                <h4>Liste d'attente</h4>
                <p>{{ animatedKpi.waitlist }}</p>
              </div>
            </div>
          </div>
          <div class="card action-card">
            <h4>Actions rapides</h4>
            <div class="action-grid">
              <button class="action-tile primary" (click)="setActiveMenu('events'); onAdd()">
                <span class="icon"><i class="fas fa-calendar-plus"></i></span>
                <div class="meta">
                  <span class="title">Nouvel événement</span>
                  <small>Créer un événement</small>
                </div>
              </button>
              <button class="action-tile info" (click)="setActiveMenu('users')">
                <span class="icon"><i class="fas fa-user-shield"></i></span>
                <div class="meta">
                  <span class="title">Gérer utilisateurs</span>
                  <small>Rôles et permissions</small>
                </div>
              </button>
              <button class="action-tile warning" (click)="setActiveMenu('invitations')">
                <span class="icon"><i class="fas fa-paper-plane"></i></span>
                <div class="meta">
                  <span class="title">Voir invitations</span>
                  <small>Suivi des statuts</small>
                </div>
              </button>
              <button class="action-tile neutral" (click)="refreshData()">
                <span class="icon"><i class="fas fa-sync-alt"></i></span>
                <div class="meta">
                  <span class="title">Actualiser</span>
                  <small>Mise à jour des données</small>
                </div>
              </button>
            </div>
          </div>
          <div class="card activity">
            <h4>Activité récente</h4>
            <ul>
              <li *ngFor="let inv of invitations | slice:0:6">
                <i class="fas fa-bell"></i>
                <span>{{ inv.userEmail }}</span>
                <small>→ {{ inv.eventTitle }}</small>
              </li>
            </ul>
          </div>
        </div>

        <!-- Analytics Section -->
        <div class="analytics-grid" *ngIf="activeMenu === 'analytics'">
          <div class="chart-card">
            <h4>Répartition</h4>
            <div class="bar-chart">
              <div class="bar" *ngFor="let d of getAnalyticsData()" [style.height.%]="getPercent(d.value, getAnalyticsMax())">
                <span class="label">{{ d.label }}</span>
                <span class="value">{{ d.value }}</span>
              </div>
            </div>
          </div>
          <div class="chart-card gradient">
            <h4>Tendance générale</h4>
            <div class="sparkline">
              <span *ngFor="let n of getSparkline()" [style.height.%]="n"></span>
            </div>
          </div>
          <div class="chart-card donut-card">
            <h4>Statut des invitations</h4>
            <div class="donut" [style.background]="getDonutBackground()">
              <div class="center">
                <strong>{{ invitations.length }}</strong>
                <span>Total</span>
              </div>
            </div>
            <ul class="legend">
              <li><span class="dot confirmed"></span>Confirmées</li>
              <li><span class="dot pending"></span>En attente</li>
              <li><span class="dot cancelled"></span>Annulées</li>
              <li><span class="dot waitlist"></span>Liste attente</li>
            </ul>
          </div>
        </div>

        <!-- Settings Section -->
        <div class="settings-panel" *ngIf="activeMenu === 'settings'">
          <div class="setting-item">
            <div>
              <h4>Thème sombre</h4>
              <p>Améliore le contraste et l'esthétique nocturne.</p>
            </div>
            <button class="toggle-switch" (click)="toggleTheme()" [class.active]="isDarkTheme"><span></span></button>
          </div>
          <div class="setting-item">
            <div>
              <h4>Mode compact</h4>
              <p>Réduit l'espacement pour afficher plus d'informations à l'écran.</p>
            </div>
            <button class="toggle-switch" (click)="toggleCompact()" [class.active]="isCompactMode"><span></span></button>
          </div>
        </div>

        <!-- Health Section -->
        <div class="health-grid" *ngIf="activeMenu === 'health'">
          <div class="health-card" *ngFor="let s of servicesHealth">
            <div class="icon"><i class="fas" [class.fa-server]="true"></i></div>
            <div class="meta">
              <h5>{{ s.name }}</h5>
              <p>Port {{ s.port }}</p>
            </div>
            <span class="status" [class.up]="s.status === 'UP'" [class.down]="s.status !== 'UP'">{{ s.status }}</span>
          </div>
        </div>

        <!-- Waitlist Section (placeholder créatif) -->
        <div class="waitlist-card" *ngIf="activeMenu === 'waitlist'">
          <div class="illustration"><i class="fas fa-chair"></i></div>
          <div class="content">
            <h4>Gestion de la liste d'attente</h4>
            <p>Visualisez les promotions automatiques et la capacité par événement.</p>
            <button class="add-btn" (click)="setActiveMenu('events')"><i class="fas fa-calendar-plus"></i> Aller aux événements</button>
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
  activeMenu: 'overview' | 'users' | 'events' | 'invitations' | 'waitlist' | 'analytics' | 'health' | 'settings' = 'overview';
  isSidebarCollapsed = false;
  isDarkTheme = false;
  isCompactMode = false;
  users: UserDetails[] = [];
  events: EventDetails[] = [];
  invitations: InvitationDetails[] = [];
  private subscription: Subscription = new Subscription();
  animatedStats = { total: 0, active: 0, inactive: 0 };
  animatedKpi = { users: 0, events: 0, invitations: 0, waitlist: 0 };
  // Données de santé (mock initial, à brancher sur /actuator/health si voulu)
  servicesHealth: Array<{ name: string; port: number; status: 'UP' | 'DOWN' }> = [
    { name: 'API Gateway', port: 8093, status: 'UP' },
    { name: 'Eureka', port: 8761, status: 'UP' },
    { name: 'User Service', port: 8084, status: 'UP' },
    { name: 'Event Service', port: 8082, status: 'UP' },
    { name: 'Invitation Service', port: 8083, status: 'UP' },
    { name: 'Notification Service', port: 8085, status: 'UP' }
  ];

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

  hideBrandLogo(event: Event) {
    const el = event.target as HTMLElement;
    if (el && el.style) {
      el.style.display = 'none';
    }
  }

  ngOnInit() {
    this.loadInitialData();
    this.setupRealTimeInvitationUpdates();
    setTimeout(() => this.animateStatsAndKpis(), 0);
  }
  toggleTheme() {
    this.isDarkTheme = !this.isDarkTheme;
  }

  toggleCompact() {
    this.isCompactMode = !this.isCompactMode;
  }

  // Total liste d'attente réel (si disponible sur events.waitlistCount)
  getTotalWaitlistCount(): number {
    return this.events.reduce((sum, e: any) => sum + (Number((e as any).waitlistCount) || 0), 0);
  }

  // Animation helpers
  private runningRafs: number[] = [];
  private animateValue(start: number, end: number, durationMs: number, onUpdate: (v: number) => void) {
    const startTs = performance.now();
    const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);
    const step = (ts: number) => {
      const progress = Math.min((ts - startTs) / durationMs, 1);
      const eased = easeOut(progress);
      const value = Math.round(start + (end - start) * eased);
      onUpdate(value);
      if (progress < 1) {
        const raf = requestAnimationFrame(step);
        this.runningRafs.push(raf);
      }
    };
    const raf = requestAnimationFrame(step);
    this.runningRafs.push(raf);
  }

  private cancelAnimations() {
    this.runningRafs.forEach(id => cancelAnimationFrame(id));
    this.runningRafs = [];
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
          this.animateStatsAndKpis();
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
        this.animateStatsAndKpis();
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
        this.animateStatsAndKpis();
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

  // Lance/relance les animations pour stats et KPIs
  private animateStatsAndKpis() {
    // Valeurs cibles
    const totalTarget = this.getStatsValue();
    const activeTarget = this.getActiveStatsValue();
    const inactiveTarget = this.getInactiveStatsValue();

    // Stat cards
    this.animateValue(this.animatedStats.total, totalTarget, 600, v => (this.animatedStats.total = v));
    this.animateValue(this.animatedStats.active, activeTarget, 700, v => (this.animatedStats.active = v));
    this.animateValue(this.animatedStats.inactive, inactiveTarget, 800, v => (this.animatedStats.inactive = v));

    // KPIs (overview)
    this.animateValue(this.animatedKpi.users, this.users.length, 600, v => (this.animatedKpi.users = v));
    this.animateValue(this.animatedKpi.events, this.events.length, 600, v => (this.animatedKpi.events = v));
    this.animateValue(this.animatedKpi.invitations, this.invitations.length, 600, v => (this.animatedKpi.invitations = v));
    this.animateValue(this.animatedKpi.waitlist, this.getTotalWaitlistCount(), 600, v => (this.animatedKpi.waitlist = v));
  }

  getStatsTitle(): string {
    switch (this.activeMenu) {
      case 'overview':
        return 'Total éléments';
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
      case 'overview':
        return this.users.length + this.events.length + this.invitations.length;
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
      case 'overview':
        return 'Éléments actifs';
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
      case 'overview': {
        const activeUsers = this.users.filter(u => u.enabled).length;
        const upcomingEvents = this.events.filter(event => this.isUpcoming(event.eventDate)).length;
        const acceptedInvitations = this.invitations.filter(inv => inv.status === 'CONFIRMED').length;
        return activeUsers + upcomingEvents + acceptedInvitations;
      }
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
      case 'overview':
        return 'À traiter';
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
      case 'overview': {
        const inactiveUsers = this.users.filter(u => !u.enabled).length;
        const pastEvents = this.events.filter(event => this.isPast(event.eventDate)).length;
        const pendingInvitations = this.invitations.filter(inv => inv.status === 'PENDING').length;
        return inactiveUsers + pastEvents + pendingInvitations;
      }
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

  setActiveMenu(menu: 'overview' | 'users' | 'events' | 'invitations' | 'waitlist' | 'analytics' | 'health' | 'settings') {
    this.activeMenu = menu;

    // Réinitialiser le badge si on va sur invitations
    if (menu === 'invitations') {
      this.hasNewInvitations = false;
      this.newInvitationsCount = 0;
    }

    if (menu === 'users' || menu === 'overview' || menu === 'analytics') {
      this.loadUsers();
    }
    if (menu === 'events' || menu === 'overview' || menu === 'analytics') {
      this.loadEvents();
    }
    this.animateStatsAndKpis();
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
      case 'overview':
        return 'Aperçu Global';
      case 'users':
        return 'Gestion des Utilisateurs';
      case 'events':
        return 'Gestion des Événements';
      case 'invitations':
        return 'Gestion des Invitations';
      case 'waitlist':
        return "Liste d'attente";
      case 'analytics':
        return 'Analytique';
      case 'health':
        return 'Santé du Système';
      case 'settings':
        return 'Paramètres';
      default:
        return '';
    }
  }

  getContentSubtitle(): string {
    switch (this.activeMenu) {
      case 'overview':
        return 'Indicateurs clés et actions rapides';
      case 'users':
        return 'Gérez les utilisateurs, leurs rôles et leurs permissions';
      case 'events':
        return 'Créez et gérez les événements de votre plateforme';
      case 'invitations':
        return 'Suivez et gérez les invitations aux événements';
      case 'waitlist':
        return "Suivez la capacité et la redistribution automatique";
      case 'analytics':
        return 'Tendances et répartition';
      case 'health':
        return 'État des microservices et endpoints de santé';
      case 'settings':
        return 'Préférences d’affichage et thème';
      default:
        return '';
    }
  }

  getAddButtonText(): string {
    switch (this.activeMenu) {
      case 'overview':
        return 'Action';
      case 'events':
        return 'Événement';
      case 'invitations':
        return 'Invitation';
      default:
        return '';
    }
  }

  getEstimatedWaitlistCount(): number {
    // approximation simple basée sur les invitations en attente
    return this.invitations.filter(i => i.status === 'WAITLIST' || i.status === 'PENDING').length;
  }

  getAnalyticsData(): Array<{ label: string; value: number }> {
    return [
      { label: 'Users', value: this.users.length },
      { label: 'Events', value: this.events.length },
      { label: 'Invites', value: this.invitations.length }
    ];
  }

  getAnalyticsMax(): number {
    return Math.max(...this.getAnalyticsData().map(d => d.value), 1);
  }

  getPercent(value: number, max: number): number {
    return Math.round((value / max) * 100);
  }

  getSparkline(): number[] {
    const base = this.getAnalyticsData().map(d => d.value);
    const max = this.getAnalyticsMax();
    return base.map(v => Math.round((v / max) * 100));
  }

  // Donut chart CSS-only: calc des pourcentages et génération de conic-gradient
  getDonutBackground(): string {
    const total = Math.max(this.invitations.length, 1);
    const confirmed = this.invitations.filter(i => i.status === 'CONFIRMED').length;
    const pending = this.invitations.filter(i => i.status === 'PENDING').length;
    const cancelled = this.invitations.filter(i => i.status === 'CANCELLED').length;
    const waitlist = this.invitations.filter(i => i.status === 'WAITLIST').length;

    const pc = (n: number) => Math.round((n / total) * 100);
    const a = pc(confirmed);
    const b = a + pc(pending);
    const c = b + pc(cancelled);
    const d = 100; // waitlist reste

    // Couleurs cohérentes avec SCSS
    return `conic-gradient(#22c55e 0% ${a}%, #f59e0b ${a}% ${b}%, #ef4444 ${b}% ${c}%, #06b6d4 ${c}% ${d}%)`;
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
