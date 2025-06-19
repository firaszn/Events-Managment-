import { Routes } from '@angular/router';
import { HomeComponent } from './user/components/home/home.component';
import { RegisterComponent } from './user/components/register/register.component';
import { ProfileComponent } from './user/components/profile/profile.component';
import { AuthGuard } from '../app/user/core/guards/auth.guard';

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
    path: '**',
    redirectTo: 'home'
  }
];
