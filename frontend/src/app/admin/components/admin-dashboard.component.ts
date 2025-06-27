import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AdminUserService, UserDetails } from '../services/admin-user.service';
import { AuthManagerService } from '../../user/core/services/auth-manager.service';
import { AdminEventService, EventDetails } from '../services/admin-event.service';
import { AdminInvitationService, InvitationDetails } from '../services/admin-invitation.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  styleUrls: ['./admin-dashboard.component.scss'],
  template: `
    <div class="admin-dashboard">
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
              <i class="fas fa-users"></i>
            </div>
            <div class="stat-info">
              <h3>{{ getTotalUsers() }}</h3>
              <p>Total Utilisateurs</p>
              <span class="trend up">↑ 12% ce mois</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon active">
              <i class="fas fa-user-check"></i>
            </div>
            <div class="stat-info">
              <h3>{{ getActiveUsers() }}</h3>
              <p>Utilisateurs Actifs</p>
              <span class="trend up">↑ 8% ce mois</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon inactive">
              <i class="fas fa-user-times"></i>
            </div>
            <div class="stat-info">
              <h3>{{ getInactiveUsers() }}</h3>
              <p>Utilisateurs Inactifs</p>
              <span class="trend down">↓ 3% ce mois</span>
            </div>
          </div>
        </div>

        <!-- Table Section -->
        <div class="table-container">
          <table>
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
        </div>
      </div>
    </div>
  `
})
export class AdminDashboardComponent implements OnInit {
  activeMenu: 'users' | 'events' | 'invitations' = 'users';
  isSidebarCollapsed = false;
  users: UserDetails[] = [];
  events: EventDetails[] = [];
  invitations: InvitationDetails[] = [];
  newEvent: Partial<EventDetails> = {};

  constructor(
    private router: Router,
    private adminUserService: AdminUserService,
    private authManager: AuthManagerService,
    private adminEventService: AdminEventService,
    private adminInvitationService: AdminInvitationService
  ) {}

  ngOnInit() {
    this.loadInitialData();
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  loadInitialData() {
    switch (this.activeMenu) {
      case 'users':
        this.loadUsers();
        break;
      case 'events':
        this.loadEvents();
        break;
      case 'invitations':
        this.loadInvitations();
        break;
    }
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
        this.events = events;
      },
      error: (error: Error) => {
        console.error('Erreur lors du chargement des événements:', error);
      }
    });
  }

  loadInvitations() {
    this.adminInvitationService.getInvitations().subscribe({
      next: (invitations) => {
        this.invitations = invitations;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des invitations:', error);
      }
    });
  }

  refreshData() {
    this.loadUsers();
  }

  refreshEvents() {
    this.loadEvents();
  }

  refreshInvitations() {
    this.loadInvitations();
  }

  onSearch(event: any) {
    const searchTerm = event.target.value.toLowerCase();
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
  }

  onAdd() {
    // Implémenter la logique d'ajout selon le menu actif
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
    if (this.newEvent.title && this.newEvent.description && this.newEvent.location && this.newEvent.date) {
      this.adminEventService.createEvent(this.newEvent).subscribe({
        next: () => {
          this.loadEvents();
          this.newEvent = {};
        },
        error: (error) => {
          console.error('Erreur lors de la création de l\'événement:', error);
        }
      });
    }
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

  deleteInvitation(invitation: InvitationDetails) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer cette invitation ?`)) {
      this.adminInvitationService.deleteInvitation(invitation.id).subscribe({
        next: () => {
          this.invitations = this.invitations.filter(i => i.id !== invitation.id);
        },
        error: (error) => {
          console.error('Erreur lors de la suppression de l\'invitation:', error);
        }
      });
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
}