import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface Notification {
  message: string;
  type: 'success' | 'error' | 'info';
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationSubject = new Subject<Notification>();
  notifications$ = this.notificationSubject.asObservable();

  show(notification: Notification) {
    this.notificationSubject.next(notification);

    if (notification.duration !== undefined) {
      setTimeout(() => {
        this.hide();
      }, notification.duration);
    }
  }

  hide() {
    this.notificationSubject.next({ message: '', type: 'info', duration: 0 });
  }

  showSuccess(message: string, duration: number = 5000) {
    this.show({
      message,
      type: 'success',
      duration
    });
  }

  showError(message: string, duration: number = 5000) {
    this.show({
      message,
      type: 'error',
      duration
    });
  }
}
