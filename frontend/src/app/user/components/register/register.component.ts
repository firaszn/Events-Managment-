import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RegisterService } from '../../core/services/register.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatSnackBarModule
  ],
  template: `
    <div class="register-page">
      <div class="register-container">
        <div class="register-content">
          <h1 class="register-title">Inscription</h1>
          <p class="register-subtitle">Rejoignez-nous pour créer et participer à des événements exceptionnels</p>
          
          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="register-form">
            <div class="form-group">
              <label for="firstName">Prénom</label>
              <input 
                id="firstName"
                type="text" 
                formControlName="firstName" 
                [class.error]="registerForm.get('firstName')?.touched && registerForm.get('firstName')?.invalid"
                placeholder="Votre prénom"
              >
              <span class="error-message" *ngIf="registerForm.get('firstName')?.touched && registerForm.get('firstName')?.invalid">
                Le prénom est requis
              </span>
            </div>

            <div class="form-group">
              <label for="lastName">Nom</label>
              <input 
                id="lastName"
                type="text" 
                formControlName="lastName" 
                [class.error]="registerForm.get('lastName')?.touched && registerForm.get('lastName')?.invalid"
                placeholder="Votre nom"
              >
              <span class="error-message" *ngIf="registerForm.get('lastName')?.touched && registerForm.get('lastName')?.invalid">
                Le nom est requis
              </span>
            </div>

            <div class="form-group">
              <label for="email">Email</label>
              <input 
                id="email"
                type="email" 
                formControlName="email" 
                [class.error]="registerForm.get('email')?.touched && registerForm.get('email')?.invalid"
                placeholder="votre.email@exemple.com"
              >
              <span class="error-message" *ngIf="registerForm.get('email')?.touched && registerForm.get('email')?.invalid">
                Un email valide est requis
              </span>
            </div>

            <div class="form-group">
              <label for="password">Mot de passe</label>
              <input 
                id="password"
                type="password" 
                formControlName="password" 
                [class.error]="registerForm.get('password')?.touched && registerForm.get('password')?.invalid"
                placeholder="Votre mot de passe"
              >
              <span class="error-message" *ngIf="registerForm.get('password')?.touched && registerForm.get('password')?.invalid">
                Le mot de passe doit contenir au moins 8 caractères
              </span>
            </div>

            <div class="form-group">
              <label for="phoneNumber">Numéro de téléphone</label>
              <input 
                id="phoneNumber"
                type="tel" 
                formControlName="phoneNumber" 
                [class.error]="registerForm.get('phoneNumber')?.touched && registerForm.get('phoneNumber')?.invalid"
                placeholder="Votre numéro de téléphone"
              >
              <span class="error-message" *ngIf="registerForm.get('phoneNumber')?.touched && registerForm.get('phoneNumber')?.invalid">
                Le numéro de téléphone est requis
              </span>
            </div>

            <div class="form-actions">
              <button 
                type="submit" 
                class="submit-button" 
                [disabled]="registerForm.invalid || isLoading"
              >
                <div class="button-content">
                  <div class="spinner" *ngIf="isLoading"></div>
                  <span>S'inscrire</span>
                </div>
              </button>
              
             
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .register-page {
      min-height: calc(100vh - 64px);
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      padding: 40px 20px;
    }

    .register-container {
      max-width: 500px;
      margin: 0 auto;
      background: white;
      border-radius: 20px;
      box-shadow: 0 8px 30px rgba(0,0,0,0.1);
      overflow: hidden;
    }

    .register-content {
      padding: 40px;
    }

    .register-title {
      font-size: 2.5em;
      color: #2c3e50;
      margin: 0 0 10px;
      text-align: center;
      font-weight: 700;
    }

    .register-subtitle {
      color: #666;
      text-align: center;
      margin-bottom: 40px;
      font-size: 1.1em;
      line-height: 1.5;
    }

    .register-form {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    label {
      font-size: 0.95em;
      font-weight: 500;
      color: #2c3e50;
    }

    input {
      padding: 12px 16px;
      border: 2px solid #e1e8ed;
      border-radius: 10px;
      font-size: 1em;
      transition: all 0.3s ease;
      width: 100%;
      box-sizing: border-box;

      &:focus {
        outline: none;
        border-color: #2196F3;
        box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
      }

      &.error {
        border-color: #e74c3c;
      }

      &::placeholder {
        color: #95a5a6;
      }
    }

    .error-message {
      color: #e74c3c;
      font-size: 0.85em;
      margin-top: 4px;
    }

    .form-actions {
      margin-top: 32px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
    }

    .submit-button {
      width: 100%;
      padding: 14px 28px;
      background: linear-gradient(135deg, #2196F3, #1976D2);
      color: white;
      border: none;
      border-radius: 10px;
      font-size: 1.1em;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 4px 15px rgba(33, 150, 243, 0.3);
      }

      &:disabled {
        background: #ccc;
        cursor: not-allowed;
      }
    }

    .button-content {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
    }

    .spinner {
      width: 20px;
      height: 20px;
      border: 3px solid rgba(255,255,255,0.3);
      border-radius: 50%;
      border-top-color: white;
      animation: spin 1s linear infinite;
    }

    .login-link {
      text-align: center;
      color: #666;
      margin: 0;

      a {
        color: #2196F3;
        text-decoration: none;
        font-weight: 500;
        
        &:hover {
          text-decoration: underline;
        }
      }
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    @media (max-width: 600px) {
      .register-page {
        padding: 20px;
      }

      .register-content {
        padding: 30px 20px;
      }

      .register-title {
        font-size: 2em;
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

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.registerForm.invalid) {
      return;
    }

    this.isLoading = true;
    const registerData = {
      ...this.registerForm.value,
      role: 'USER'
    };

    this.registerService.register(registerData).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response && response.keycloakLoginUrl) {
          window.location.href = response.keycloakLoginUrl;
        } else {
          this.snackBar.open(
            'Erreur: URL de redirection manquante',
            'Fermer',
            { duration: 3000 }
          );
        }
      },
      error: (error: any) => {
        console.log('Erreur:', error);
        this.isLoading = false;
        if (error.status === 0 && error.error instanceof ProgressEvent) {
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

