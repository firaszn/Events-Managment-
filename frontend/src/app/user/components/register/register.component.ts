import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { RegisterService } from '../../core/services/register.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  template: `
    <div class="register-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Inscription</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" #form="ngForm">
            <mat-form-field appearance="outline">
              <mat-label>Prénom</mat-label>
              <input matInput formControlName="firstName" required>
              <mat-error *ngIf="registerForm.get('firstName')?.touched && registerForm.get('firstName')?.invalid">
                Le prénom est requis
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Nom</mat-label>
              <input matInput formControlName="lastName" required>
              <mat-error *ngIf="registerForm.get('lastName')?.touched && registerForm.get('lastName')?.invalid">
                Le nom est requis
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" required>
              <mat-error *ngIf="registerForm.get('email')?.touched && registerForm.get('email')?.invalid">
                Un email valide est requis
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Mot de passe</mat-label>
              <input matInput type="password" formControlName="password" required>
              <mat-error *ngIf="registerForm.get('password')?.touched && registerForm.get('password')?.invalid">
                Le mot de passe doit contenir au moins 8 caractères
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Numéro de téléphone</mat-label>
              <input matInput formControlName="phoneNumber" required>
              <mat-error *ngIf="registerForm.get('phoneNumber')?.touched && registerForm.get('phoneNumber')?.invalid">
                Le numéro de téléphone est requis
              </mat-error>
            </mat-form-field>

            <div class="button-container">              <button mat-raised-button color="primary" type="submit">
                <div class="button-content">
                  <mat-spinner diameter="20" *ngIf="isLoading"></mat-spinner>
                  <span>S'inscrire </span>
                </div>
              </button>
             
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    /* Conteneur principal */
    .register-container {
      display: flex;
      justify-content: center;
      align-items: flex-start;
      min-height: calc(100vh - 64px); /* Hauteur totale - hauteur de la navbar */
      background-color: #f5f5f5;
      padding: 24px 16px;
    }
    
    /* Carte du formulaire */
    mat-card {
      width: 100%;
      max-width: 500px;
      margin: 0;
      padding: 32px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
      border-radius: 8px;
    }

    /* En-tête de la carte */
    mat-card-header {
      margin-bottom: 32px;
      padding: 0;
      text-align: center;
    }
    
    mat-card-title {
      font-size: 24px;
      font-weight: 500;
      color: #3f51b5;
      margin: 0;
    }

    /* Formulaire */
    form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    /* Champs de formulaire */
    mat-form-field {
      width: 100%;
      margin-bottom: 8px;
      
      /* Fond blanc pour les champs */
      ::ng-deep .mat-form-field-outline {
        background-color: white;
        border-radius: 4px;
      }
      
      /* Style du label */
      ::ng-deep .mat-form-field-label {
        color: rgba(0, 0, 0, 0.6);
      }
    }

    /* Conteneur du bouton */
    .button-container {
      margin-top: 24px;
      
      button[mat-raised-button] {
        width: 100%;
        height: 48px;
        font-size: 16px;
        font-weight: 500;
        letter-spacing: 0.5px;
        
        /* Désactiver l'effet de clic */
        &:active:not([disabled]) {
          transform: none;
        }
      }
    }

    /* Contenu du bouton avec spinner */
    .button-content {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    /* Messages d'erreur */
    mat-error {
      font-size: 12px;
      margin-top: 4px;
      line-height: 1.2;
    }

    /* Styles responsives */
    @media (max-width: 600px) {
      .register-container {
        padding: 16px;
        align-items: flex-start;
      }
      
      mat-card {
        padding: 24px 16px;
        margin-top: 0;
      }
      
      mat-card-header {
        margin-bottom: 24px;
      }
      
      .button-container button[mat-raised-button] {
        height: 44px;
      }
    }
  `]
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  isLoading = false;

  constructor(
    private formBuilder: FormBuilder,
    private registerService: RegisterService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.formBuilder.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      phoneNumber: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {}  onSubmit(): void {
    this.isLoading = true;
    const registerData = {
      ...this.registerForm.value,
      role: 'USER'
    };

    this.registerService.register(registerData).subscribe({
      next: (response) => {
        this.isLoading = false;
        console.log('Réponse du serveur:', response);
        if (response && response.keycloakLoginUrl) {
          // Utiliser l'URL de redirection fournie par le backend
          window.location.href = response.keycloakLoginUrl;
        } else {
          this.snackBar.open(
            'Erreur: URL de redirection manquante',
            'Fermer',
            { duration: 3000 }
          );
        }
      },      error: (error: any) => {
        console.log('Erreur:', error);
        this.isLoading = false;
        if (error.status === 0 && error.error instanceof ProgressEvent) {
          // Si c'est une erreur CORS mais que l'inscription a probablement réussi
          // On redirige directement vers Keycloak avec une URL générée
          this.registerService.getLoginUrl().subscribe({
            next: (loginUrl: string) => {
              if (loginUrl) {
                window.location.href = loginUrl;
              }
            },
            error: () => {
              this.snackBar.open(
                'Erreur lors de la redirection. Veuillez réessayer.',
                'Fermer',
                { duration: 3000 }
              );
            }
          });
        } else {
          this.snackBar.open(
            'Erreur lors de l\'inscription. Veuillez réessayer.',
            'Fermer',
            { duration: 3000 }
          );
        }
      }
    });
  }
}
