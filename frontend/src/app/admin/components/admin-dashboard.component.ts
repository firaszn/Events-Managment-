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
  template: `
    <div class="admin-dashboard">
      <!-- Sidebar -->
      <div class="sidebar">
        <div class="sidebar-header">
          <div class="logo-container">
            <i class="fas fa-shield-alt"></i>
            <h3>Event Manager</h3>
          </div>
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
      <div class="main-content">
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

        <div class="content-body">
          <!-- Users Section -->
          <div *ngIf="activeMenu === 'users'" class="content-section">
            <div class="section-header">
              <h2>Gestion des Utilisateurs</h2>
              <div class="search-container">
                <input type="text" placeholder="Rechercher..." (input)="onSearch($event)">
                <button class="refresh-btn" (click)="refreshData()">
                  <i class="fas fa-sync-alt"></i>
                </button>
              </div>
            </div>

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
                      <button class="action-btn" (click)="toggleUserStatus(user)">
                        <i class="fas" [class.fa-toggle-on]="isUserEnabled(user)" [class.fa-toggle-off]="!isUserEnabled(user)"></i>
                      </button>
                      <button class="action-btn edit" (click)="editUser(user)">
                        <i class="fas fa-edit"></i>
                      </button>
                      <button class="action-btn delete" (click)="deleteUser(user)">
                        <i class="fas fa-trash"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Events Section -->
          <div *ngIf="activeMenu === 'events'" class="content-section">
            <div class="section-header">
              <h2>Gestion des Événements</h2>
              <button class="refresh-btn" (click)="refreshEvents()">
                <i class="fas fa-sync-alt"></i>
              </button>
            </div>

            <div class="event-form">
              <h3>Créer un Événement</h3>
              <form (ngSubmit)="createEvent()">
                <div class="form-group">
                  <input type="text" [(ngModel)]="newEvent.title" name="title" placeholder="Titre de l'événement" required>
                </div>
                <div class="form-group">
                  <textarea [(ngModel)]="newEvent.description" name="description" placeholder="Description" required></textarea>
                </div>
                <div class="form-group">
                  <input type="text" [(ngModel)]="newEvent.location" name="location" placeholder="Lieu" required>
                </div>
                <div class="form-group">
                  <input type="datetime-local" [(ngModel)]="newEvent.date" name="date" required>
                </div>
                <button type="submit" class="create-btn">Créer l'événement</button>
              </form>
            </div>

            <div class="table-container">
              <table>
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
                    <td>{{ event.date | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td class="actions">
                      <button class="action-btn delete" (click)="deleteEvent(event)">
                        <i class="fas fa-trash"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Invitations Section -->
          <div *ngIf="activeMenu === 'invitations'" class="content-section">
            <div class="section-header">
              <h2>Gestion des Invitations</h2>
              <button class="refresh-btn" (click)="refreshInvitations()">
                <i class="fas fa-sync-alt"></i>
              </button>
            </div>

            <div class="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Événement</th>
                    <th>Utilisateur</th>
                    <th>Email</th>
                    <th>Statut</th>
                    <th>Date d'invitation</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let invitation of invitations">
                    <td>
                      <div class="event-info">
                        <span class="event-title">{{ invitation.eventTitle }}</span>
                      </div>
                    </td>
                    <td>{{ invitation.userEmail }}</td>
                    <td>{{ invitation.userEmail }}</td>
                    <td>
                      <span class="invitation-status" [ngClass]="invitation.status.toLowerCase()">
                        {{ invitation.status }}
                      </span>
                    </td>
                    <td>{{ invitation.created_at | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td class="actions">
                      <button class="action-btn delete" (click)="deleteInvitation(invitation)">
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
    </div>
  `,
  styles: [`
    .admin-dashboard {
      display: flex;
      min-height: 100vh;
      background-color: #f8fafc;
      margin: 0;
      padding: 0;
    }

    /* Sidebar Styles */
    .sidebar {
      width: 280px;
      background: #1e293b;
      color: white;
      padding: 20px 0;
      height: 100vh;
      position: sticky;
      top: 0;
      display: flex;
      flex-direction: column;
    }

    .logo-container {
      display: flex;
      align-items: center;
      gap: 15px;
      padding: 0 25px 20px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .logo-container i {
      font-size: 2em;
      color: #38bdf8;
    }

    .logo-container h3 {
      margin: 0;
      font-size: 1.5em;
      font-weight: 600;
      background: linear-gradient(45deg, #38bdf8, #818cf8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .sidebar-menu {
      margin-top: 30px;
      padding: 0 15px;
      display: flex;
      flex-direction: column;
      gap: 5px;
    }

    .menu-item {
      position: relative;
      padding: 15px 20px;
      cursor: pointer;
      transition: all 0.3s ease;
      border-radius: 8px;
      display: flex;
      align-items: center;
      gap: 15px;
      color: #94a3b8;
    }

    .menu-item:hover {
      background: rgba(255, 255, 255, 0.1);
      color: white;
    }

    .menu-item.active {
      background: #0284c7;
      color: white;
    }

    .menu-item i {
      font-size: 1.2em;
      width: 24px;
      text-align: center;
    }

    .menu-indicator {
      position: absolute;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 0;
      background: #38bdf8;
      border-radius: 2px;
      transition: height 0.3s ease;
    }

    .menu-item.active .menu-indicator {
      height: 20px;
    }

    .menu-item.logout {
      margin-top: 15px;
      color: #ef4444;
      background: rgba(239, 68, 68, 0.1);
    }

    .menu-item.logout:hover {
      background: rgba(239, 68, 68, 0.2);
      color: #ef4444;
    }

    @media (max-width: 1024px) {
      .sidebar {
        width: 80px;
      }

      .logo-container h3,
      .menu-item span {
        display: none;
      }

      .menu-item {
        justify-content: center;
        padding: 15px;
      }

      .menu-item i {
        margin: 0;
      }
    }

    /* Main Content Styles */
    .main-content {
      flex: 1;
      padding: 30px;
      overflow-y: auto;
    }

    .content-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 30px;
      padding: 20px;
      background: white;
      border-radius: 15px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    }

    .header-left h2 {
      margin: 0;
      color: #0f172a;
      font-size: 1.8em;
      font-weight: 600;
    }

    .subtitle {
      margin: 5px 0 0;
      color: #64748b;
      font-size: 0.9em;
    }

    .header-actions {
      display: flex;
      gap: 15px;
      align-items: center;
    }

    .search-box {
      position: relative;
      width: 300px;
    }

    .search-box input {
      width: 100%;
      padding: 10px 40px 10px 15px;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      background: #f8fafc;
      font-size: 0.9em;
      transition: all 0.3s ease;
    }

    .search-box input:focus {
      outline: none;
      border-color: #38bdf8;
      box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.1);
    }

    .search-box i {
      position: absolute;
      right: 15px;
      top: 50%;
      transform: translateY(-50%);
      color: #64748b;
    }

    .refresh-btn, .add-btn {
      padding: 10px;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .refresh-btn {
      background: #f8fafc;
      color: #64748b;
    }

    .refresh-btn:hover {
      background: #f1f5f9;
      color: #0f172a;
    }

    .add-btn {
      background: #0284c7;
      color: white;
      padding: 10px 20px;
    }

    .add-btn:hover {
      background: #0369a1;
      transform: translateY(-1px);
    }

    /* Stats Cards */
    .stats-container {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 20px;
      margin-bottom: 30px;
    }

    .stat-card {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      display: flex;
      align-items: center;
    }

    .stat-icon {
      width: 50px;
      height: 50px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 15px;
    }

    .stat-icon.total { background-color: #e6f7ff; color: #0095e8; }
    .stat-icon.active { background-color: #f6ffed; color: #52c41a; }
    .stat-icon.inactive { background-color: #fff2f0; color: #ff4d4f; }

    .stat-info h3 {
      margin: 0;
      font-size: 24px;
      line-height: 1;
    }

    .stat-info p {
      margin: 5px 0;
      color: #8c8c8c;
    }

    .trend {
      font-size: 12px;
      display: block;
    }

    .trend.up { color: #52c41a; }
    .trend.down { color: #ff4d4f; }

    /* Table Styles */
    .table-container {
      background: white;
      border-radius: 15px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      overflow: hidden;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th, td {
      padding: 12px 15px;
      text-align: left;
      border-bottom: 1px solid #f0f0f0;
    }

    th {
      background-color: #fafafa;
      font-weight: 600;
    }

    .status {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
    }

    .status.active {
      background-color: #f6ffed;
      color: #52c41a;
    }

    .status.inactive {
      background-color: #fff2f0;
      color: #ff4d4f;
    }

    .actions {
      display: flex;
      gap: 8px;
    }

    .action-btn {
      padding: 6px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s ease;
      background: none;
    }

    .action-btn i {
      font-size: 16px;
    }

    .action-btn:hover { background-color: #f5f5f5; }
    .action-btn.edit:hover { color: #0095e8; }
    .action-btn.delete:hover { color: #ff4d4f; }

    .event-form {
      background-color: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      margin-bottom: 30px;
    }

    .event-form h3 {
      margin: 0 0 20px 0;
      color: #1e1e2d;
    }

    .form-group {
      margin-bottom: 15px;
    }

    .form-group input,
    .form-group textarea {
      width: 100%;
      padding: 8px 12px;
      border: 1px solid #e1e1e1;
      border-radius: 4px;
      font-size: 14px;
    }

    .form-group textarea {
      height: 100px;
      resize: vertical;
    }

    .create-btn {
      background-color: #0095e8;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 4px;
      cursor: pointer;
      transition: background-color 0.3s ease;
    }

    .create-btn:hover {
      background-color: #0077cc;
    }

    .event-info {
      display: flex;
      flex-direction: column;
    }

    .event-title {
      font-weight: 500;
      color: #1e1e2d;
    }

    .event-date {
      font-size: 12px;
      color: #8c8c8c;
      margin-top: 4px;
    }

    .user-info {
      display: flex;
      flex-direction: column;
    }

    .user-name {
      font-weight: 500;
      color: #1e1e2d;
    }

    .user-username {
      font-size: 12px;
      color: #8c8c8c;
      margin-top: 4px;
    }

    .invitation-status {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;
    }

    .invitation-status.pending {
      background-color: #fff7e6;
      color: #fa8c16;
    }

    .invitation-status.accepted {
      background-color: #f6ffed;
      color: #52c41a;
    }

    .invitation-status.declined {
      background-color: #fff2f0;
      color: #ff4d4f;
    }
  `]
})
export class AdminDashboardComponent implements OnInit {
  activeMenu: 'users' | 'events' | 'invitations' = 'users';
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
    this.loadUsers();
    this.loadEvents();
    this.loadInvitations();
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