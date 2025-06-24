import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AdminUserService, UserDetails } from '../../user/core/services/admin-user.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="admin-dashboard">
      <!-- Sidebar -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h3>Admin Panel</h3>
        </div>
        <div class="sidebar-menu">
          <div class="menu-item" (click)="setActiveMenu('users')" [class.active]="activeMenu === 'users'">
            <i class="fas fa-users"></i>
            <span>Utilisateurs</span>
          </div>
          <div class="menu-item" (click)="setActiveMenu('events')" [class.active]="activeMenu === 'events'">
            <i class="fas fa-calendar-alt"></i>
            <span>Événements</span>
          </div>
          <div class="menu-item" (click)="setActiveMenu('invitations')" [class.active]="activeMenu === 'invitations'">
            <i class="fas fa-envelope"></i>
            <span>Invitations</span>
          </div>
        </div>
      </div>

      <!-- Main Content -->
      <div class="main-content">
        <div class="content-header">
          <h2>{{ getContentTitle() }}</h2>
        </div>
        <div class="content-body">
          <!-- Users Section -->
          <div *ngIf="activeMenu === 'users'" class="content-section">
            <div class="table-container">
              <table class="users-table">
                <thead>
                  <tr>
                    <th>Nom d'utilisateur</th>
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
                      <span class="status-badge" [class.active]="user.enabled">
                        {{ user.enabled ? 'Actif' : 'Inactif' }}
                      </span>
                    </td>
                    <td class="actions">
                      <button class="action-btn" (click)="toggleUserStatus(user)">
                        <i class="fas" [class.fa-toggle-on]="user.enabled" [class.fa-toggle-off]="!user.enabled"></i>
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
            <h3>Gestion des Événements</h3>
            <div class="event-form">
              <input type="text" placeholder="Titre de l'événement" class="form-input">
              <textarea placeholder="Description" class="form-input"></textarea>
              <input type="text" placeholder="Lieu" class="form-input">
              <input type="datetime-local" class="form-input">
              <button class="create-btn">Créer l'événement</button>
            </div>
          </div>

          <!-- Invitations Section -->
          <div *ngIf="activeMenu === 'invitations'" class="content-section">
            <h3>Gestion des Invitations</h3>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-dashboard {
      display: flex;
      height: 100vh;
      background-color: #f5f5f5;
      margin-top: -64px; /* Pour compenser la hauteur de la navbar */
      padding-top: 64px; /* Pour que le contenu ne soit pas caché derrière la navbar */
    }

    /* Sidebar Styles */
    .sidebar {
      width: 250px;
      background-color: #2c3e50;
      color: white;
      padding: 20px 0;
      box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
      height: 100%;
      position: sticky;
      top: 64px;
    }

    .sidebar-header {
      padding: 0 20px 20px;
      border-bottom: 1px solid #34495e;
    }

    .sidebar-header h3 {
      margin: 0;
      font-size: 1.5em;
      color: #ecf0f1;
    }

    .sidebar-menu {
      margin-top: 20px;
    }

    .menu-item {
      padding: 15px 20px;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .menu-item:hover {
      background-color: #34495e;
    }

    .menu-item.active {
      background-color: #3498db;
    }

    .menu-item i {
      width: 20px;
    }

    /* Main Content Styles */
    .main-content {
      flex: 1;
      padding: 20px;
      overflow-y: auto;
    }

    .content-header {
      margin-bottom: 20px;
      padding-bottom: 10px;
      border-bottom: 1px solid #ddd;
    }

    .content-header h2 {
      margin: 0;
      color: #2c3e50;
    }

    .content-section {
      background-color: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    /* Table Styles */
    .table-container {
      overflow-x: auto;
    }

    .users-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 20px;
    }

    .users-table th,
    .users-table td {
      padding: 12px;
      text-align: left;
      border-bottom: 1px solid #ddd;
    }

    .users-table th {
      background-color: #f8f9fa;
      color: #2c3e50;
      font-weight: 600;
    }

    .users-table tr:hover {
      background-color: #f8f9fa;
    }

    /* Badge Styles */
    .role-badge {
      display: inline-block;
      padding: 4px 8px;
      margin: 2px;
      background-color: #3498db;
      color: white;
      border-radius: 12px;
      font-size: 0.85em;
    }

    .status-badge {
      display: inline-block;
      padding: 6px 12px;
      border-radius: 12px;
      font-weight: 500;
      background-color: #e74c3c;
      color: white;
    }

    .status-badge.active {
      background-color: #2ecc71;
    }

    /* Action Buttons */
    .actions {
      white-space: nowrap;
    }

    .action-btn {
      padding: 6px 10px;
      margin: 0 2px;
      border: none;
      border-radius: 4px;
      background: none;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .action-btn i {
      font-size: 1.1em;
    }

    .action-btn:hover {
      background-color: #f0f0f0;
    }

    .action-btn.delete:hover {
      background-color: #fee;
      color: #e74c3c;
    }

    /* Form Styles */
    .event-form {
      display: flex;
      flex-direction: column;
      gap: 15px;
      max-width: 600px;
    }

    .form-input {
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
    }

    textarea.form-input {
      min-height: 100px;
      resize: vertical;
    }

    .create-btn {
      padding: 12px 20px;
      background-color: #3498db;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: bold;
      transition: background-color 0.3s ease;
    }

    .create-btn:hover {
      background-color: #2980b9;
    }
  `]
})
export class AdminDashboardComponent implements OnInit {
  activeMenu: 'users' | 'events' | 'invitations' = 'users';
  users: UserDetails[] = [];

  constructor(
    private router: Router,
    private adminUserService: AdminUserService
  ) {}

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.adminUserService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        console.log('Users loaded:', users);
      },
      error: (error) => {
        console.error('Error loading users:', error);
      }
    });
  }

  setActiveMenu(menu: 'users' | 'events' | 'invitations') {
    this.activeMenu = menu;
    if (menu === 'users') {
      this.loadUsers();
    }
  }

  toggleUserStatus(user: UserDetails) {
    const newStatus = !user.enabled;
    this.adminUserService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        user.enabled = newStatus;
        console.log(`User ${user.username} status updated to ${newStatus}`);
      },
      error: (error) => {
        console.error('Error updating user status:', error);
      }
    });
  }

  deleteUser(user: UserDetails) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer l'utilisateur ${user.username} ?`)) {
      this.adminUserService.deleteUser(user.id).subscribe({
        next: () => {
          this.users = this.users.filter(u => u.id !== user.id);
          console.log(`User ${user.username} deleted`);
        },
        error: (error) => {
          console.error('Error deleting user:', error);
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
        return 'Dashboard Administrateur';
    }
  }
} 