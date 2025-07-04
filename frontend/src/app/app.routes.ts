import { Routes } from '@angular/router';
import { HomeComponent } from './user/components/home/home.component';
import { RegisterComponent } from './user/components/register/register.component';
import { ProfileComponent } from './user/components/profile/profile.component';
import { AuthGuard } from './user/core/guards/auth.guard';
import { AdminGuard } from './user/core/guards/admin.guard';
import { EventListComponent } from './event/components/event-list.component';
import { AdminDashboardComponent } from './admin/components/admin-dashboard.component';
import { SeatSelectionComponent } from './event/components/seat-selection.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'events',
    component: EventListComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'events/:id/select-seat',
    component: SeatSelectionComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'admin-dashboard',
    component: AdminDashboardComponent,
    canActivate: [AuthGuard, AdminGuard],
    data: { hideNavbar: true }
  },
  {
    path: '**',
    redirectTo: 'home'
  }
];
