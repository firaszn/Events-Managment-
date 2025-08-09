import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileService } from '../../core/services/profile.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  isLoading = false;
  isSaving = false;
  // Avatar preview (local only)
  avatarPreviewUrl: string | null = null;
  avatarFile: File | null = null;
  // Password strength (0..4)
  passwordStrength = 0;
  // History metadata
  lastUpdated: string | null = null;

  constructor(
    private fb: FormBuilder,
    private profileService: ProfileService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: [''],
      phoneNumber: ['']
    });
  }

  ngOnInit() {
    this.loadUserProfile();
    // Mettre à jour la jauge de mot de passe en direct
    this.profileForm.get('password')?.valueChanges.subscribe((value: string) => {
      this.passwordStrength = this.computePasswordStrength(value || '');
    });
  }

  private loadUserProfile() {
    this.isLoading = true;
    console.log('Début du chargement du profil utilisateur...');
    
    this.profileService.getUserProfile().subscribe({
      next: (profile) => {
        console.log('Profil utilisateur chargé avec succès:', profile);
        try {
          this.profileForm.patchValue({
            firstName: profile.firstName || '',
            lastName: profile.lastName || '',
            email: profile.email || '',
            phoneNumber: profile.phoneNumber || ''
          });
          // Avatar et historique si disponibles
          if (profile.avatarUrl) {
            this.avatarPreviewUrl = profile.avatarUrl;
          }
          if (profile.updatedAt) {
            try {
              const d = new Date(profile.updatedAt);
              this.lastUpdated = d.toLocaleString('fr-FR');
            } catch {
              this.lastUpdated = String(profile.updatedAt);
            }
          }
        } catch (patchError) {
          console.error('Erreur lors de la mise à jour du formulaire:', patchError);
          this.showError('Erreur de format des données reçues');
        }
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erreur détaillée:', error);
        let errorMessage = 'Erreur lors du chargement du profil';
        
        if (error.status === 0) {
          errorMessage = 'Impossible de se connecter au serveur. Vérifiez votre connexion internet.';
        } else if (error.status === 401) {
          errorMessage = 'Veuillez vous connecter pour accéder à cette page';
        } else if (error.status === 403) {
          errorMessage = 'Vous n\'avez pas les droits pour accéder à cette ressource';
        } else if (error.status === 404) {
          errorMessage = 'Profil utilisateur non trouvé';
        } else if (error.status >= 500) {
          errorMessage = 'Erreur serveur. Veuillez réessayer plus tard.';
        }
        
        this.showError(errorMessage);
        this.isLoading = false;
      }
    });
  }
  
  private showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  // Gestion de l'avatar (aperçu local)
  onAvatarSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.avatarFile = file;
    const reader = new FileReader();
    reader.onload = () => {
      this.avatarPreviewUrl = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  removeAvatar() {
    this.avatarFile = null;
    this.avatarPreviewUrl = null;
  }

  // Calcul simple de force de mot de passe (0..4)
  private computePasswordStrength(pwd: string): number {
    let score = 0;
    if (!pwd) return 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    return Math.min(score, 4);
  }

  onSubmit() {
    if (this.profileForm.valid) {
      this.isSaving = true;
      const profileData = this.profileForm.value;
      
      // Ne pas envoyer le mot de passe s'il est vide
      if (!profileData.password) {
        delete profileData.password;
      }

      this.profileService.updateProfile(profileData).subscribe({
        next: () => {
          this.snackBar.open('Profil mis à jour avec succès', 'Fermer', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
          this.isSaving = false;
          this.lastUpdated = new Date().toLocaleString('fr-FR');
          // Redirection vers la page d'accueil après 1 seconde
          setTimeout(() => {
            this.router.navigate(['/home']);
          }, 1000);
        },
        error: (error: any) => {
          console.error('Error updating profile:', error);
          this.snackBar.open('Erreur lors de la mise à jour du profil', 'Fermer', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
          this.isSaving = false;
        }
      });
    }
  }
}
