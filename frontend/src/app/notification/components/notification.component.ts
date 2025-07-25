import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, Notification } from '../services/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="notification"
         class="notification"
         [class.success]="notification.type === 'success'"
         [class.error]="notification.type === 'error'"
         [class.info]="notification.type === 'info'">
      <div class="notification-content">
        <i class="fas"
           [class.fa-check-circle]="notification.type === 'success'"
           [class.fa-exclamation-circle]="notification.type === 'error'"
           [class.fa-info-circle]="notification.type === 'info'">
        </i>
        <span>{{ notification.message }}</span>
      </div>
      <button class="close-button" (click)="closeNotification()">
        <i class="fas fa-times"></i>
      </button>
    </div>
  `,
  styles: [`
    .notification {
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 15px 20px;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      z-index: 1000;
      min-width: 300px;
      max-width: 400px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      animation: slideIn 0.3s ease-out;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    .notification-content {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .success {
      background-color: #4CAF50;
      color: white;
    }

    .error {
      background-color: #f44336;
      color: white;
    }

    .info {
      background-color: #2196F3;
      color: white;
    }

    .close-button {
      background: none;
      border: none;
      color: white;
      cursor: pointer;
      padding: 0;
      margin-left: 10px;
      opacity: 0.8;
      transition: opacity 0.2s;
    }

    .close-button:hover {
      opacity: 1;
    }

    i {
      font-size: 1.2em;
    }
  `]
})
export class NotificationComponent implements OnInit, OnDestroy {
  notification: Notification | null = null;
  private subscription: Subscription | null = null;

  constructor(private notificationService: NotificationService) {}

  ngOnInit() {
    this.subscription = this.notificationService.notifications$.subscribe(
      notification => {
        this.notification = notification.message ? notification : null;
      }
    );
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  closeNotification() {
    this.notificationService.hide();
  }
}
