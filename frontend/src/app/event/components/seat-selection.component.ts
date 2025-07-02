import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from '../services/event.service';
import { InvitationService } from '../../invitation/services/invitation.service';
import { NotificationService } from '../../notification/services/notification.service';
import { AuthService } from '../../user/core/services/auth.service';
import { forkJoin } from 'rxjs';

interface Seat {
  id: number;
  row: number;
  number: number;
  isOccupied: boolean;
  isSelected: boolean;
}

interface OccupiedSeat {
  row: number;
  number: number;
}

@Component({
  selector: 'app-seat-selection',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="seat-selection-container">
      <h1>Sélectionnez vos places</h1>
      <div class="seats-info">
        <p>{{ availableSeats }} places libres</p>
      </div>

      <div class="screen">
        <div class="screen-text">Écran</div>
      </div>

      <div class="seating-layout">
        <div *ngFor="let row of seatingLayout" class="row">
          <div *ngFor="let seat of row" 
               class="seat" 
               [class.occupied]="seat.isOccupied"
               [class.selected]="seat.isSelected"
               (click)="toggleSeat(seat)">
            {{ seat.number }}
          </div>
        </div>
      </div>

      <div class="legend">
        <div class="legend-item">
          <div class="seat-example"></div>
          <span>Disponible</span>
        </div>
        <div class="legend-item">
          <div class="seat-example occupied"></div>
          <span>Occupé</span>
        </div>
        <div class="legend-item">
          <div class="seat-example selected"></div>
          <span>Sélectionné</span>
        </div>
      </div>

      <div class="actions">
        <button class="cancel-button" (click)="cancel()">Annuler</button>
        <button class="confirm-button" 
                [disabled]="!selectedSeat"
                (click)="confirmSelection()">
          Confirmer la sélection
        </button>
      </div>
    </div>
  `,
  styles: [`
    .seat-selection-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem;
    }

    h1 {
      text-align: center;
      color: #2c3e50;
      margin-bottom: 2rem;
    }

    .seats-info {
      text-align: center;
      margin-bottom: 2rem;
      font-size: 1.2rem;
      color: #666;
    }

    .screen {
      background: linear-gradient(to bottom, #e0e0e0, #f5f5f5);
      height: 50px;
      margin: 2rem auto;
      width: 80%;
      border-radius: 5px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }

    .screen-text {
      color: #666;
      font-weight: 500;
    }

    .seating-layout {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin: 2rem 0;
      align-items: center;
    }

    .row {
      display: flex;
      gap: 10px;
    }

    .seat {
      width: 40px;
      height: 40px;
      border: 2px solid #ffd700;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      background-color: #fff;
      transition: all 0.3s ease;
      color: #666;
      font-size: 0.9rem;
    }

    .seat:hover:not(.occupied) {
      transform: scale(1.1);
      background-color: #fff3b0;
    }

    .seat.occupied {
      background-color: #e0e0e0;
      border-color: #bdbdbd;
      cursor: not-allowed;
    }

    .seat.selected {
      background-color: #4CAF50;
      border-color: #388E3C;
      color: white;
    }

    .legend {
      display: flex;
      justify-content: center;
      gap: 2rem;
      margin: 2rem 0;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .seat-example {
      width: 20px;
      height: 20px;
      border: 2px solid #ffd700;
      border-radius: 4px;
      background-color: #fff;
    }

    .seat-example.occupied {
      background-color: #e0e0e0;
      border-color: #bdbdbd;
    }

    .seat-example.selected {
      background-color: #4CAF50;
      border-color: #388E3C;
    }

    .actions {
      display: flex;
      justify-content: center;
      gap: 1rem;
      margin-top: 2rem;
    }

    button {
      padding: 12px 24px;
      border-radius: 5px;
      border: none;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .cancel-button {
      background-color: #e0e0e0;
      color: #666;
    }

    .confirm-button {
      background-color: #2196F3;
      color: white;
    }

    .confirm-button:disabled {
      background-color: #bdbdbd;
      cursor: not-allowed;
    }

    button:hover:not(:disabled) {
      transform: translateY(-2px);
    }
  `]
})
export class SeatSelectionComponent implements OnInit {
  seatingLayout: Seat[][] = [];
  selectedSeat: Seat | null = null;
  availableSeats: number = 0;
  eventId: string = '';
  eventTitle: string = '';
  userEmail: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private invitationService: InvitationService,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  async ngOnInit() {
    const profile = await this.authService.loadUserProfile();
    this.userEmail = profile?.email || '';

    this.route.params.subscribe(params => {
      this.eventId = params['id'];
      this.loadEventDetailsAndSeats();
    });
  }

  loadEventDetailsAndSeats() {
    // Charger les détails de l'événement et les places occupées en parallèle
    forkJoin({
      event: this.eventService.getEventById(this.eventId),
      occupiedSeats: this.invitationService.getOccupiedSeats(this.eventId)
    }).subscribe({
      next: (result) => {
        this.eventTitle = result.event.title;
        this.loadSeatingLayout(result.occupiedSeats);
      },
      error: (error) => {
        console.error('Error loading event details or seats:', error);
        this.notificationService.show({
          message: 'Erreur lors du chargement des informations',
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  loadSeatingLayout(occupiedSeats: OccupiedSeat[]) {
    const rows = 10;
    const seatsPerRow = 12;
    this.seatingLayout = [];
    let seatId = 1;

    for (let i = 0; i < rows; i++) {
      const row: Seat[] = [];
      for (let j = 0; j < seatsPerRow; j++) {
        const currentRow = i + 1;
        const currentNumber = j + 1;
        
        // Vérifier si le siège est occupé
        const isOccupied = occupiedSeats.some(
          seat => seat.row === currentRow && seat.number === currentNumber
        );

        row.push({
          id: seatId++,
          row: currentRow,
          number: currentNumber,
          isOccupied: isOccupied,
          isSelected: false
        });
      }
      this.seatingLayout.push(row);
    }

    this.updateAvailableSeats();
  }

  toggleSeat(seat: Seat) {
    if (seat.isOccupied) {
      this.notificationService.show({
        message: 'Cette place est déjà occupée',
        type: 'error',
        duration: 3000
      });
      return;
    }

    // Désélectionner le siège précédemment sélectionné
    if (this.selectedSeat) {
      this.selectedSeat.isSelected = false;
    }

    // Sélectionner le nouveau siège
    seat.isSelected = !seat.isSelected;
    this.selectedSeat = seat.isSelected ? seat : null;
  }

  updateAvailableSeats() {
    this.availableSeats = this.seatingLayout.reduce((total, row) => {
      return total + row.filter(seat => !seat.isOccupied).length;
    }, 0);
  }

  cancel() {
    this.router.navigate(['/events']);
  }

  confirmSelection() {
    if (!this.selectedSeat || !this.userEmail) return;

    const request = {
      eventId: this.eventId,
      eventTitle: this.eventTitle,
      userEmail: this.userEmail,
      seatInfo: {
        row: this.selectedSeat.row,
        number: this.selectedSeat.number
      }
    };

    this.invitationService.createInvitation(request).subscribe({
      next: () => {
        this.notificationService.show({
          message: `Inscription confirmée pour l'événement : ${this.eventTitle} - Place : Rangée ${request.seatInfo.row}, Siège ${request.seatInfo.number}`,
          type: 'success',
          duration: 5000
        });

        this.router.navigate(['/events']);
      },
      error: (error) => {
        console.error('Error registering for event:', error);
        let errorMessage = 'Une erreur est survenue lors de l\'inscription.';
        
        if (error.error?.message) {
          errorMessage = error.error.message;
        }
        
        this.notificationService.show({
          message: errorMessage,
          type: 'error',
          duration: 5000
        });

        // Recharger les places pour avoir l'état à jour
        this.loadEventDetailsAndSeats();
      }
    });
  }
} 