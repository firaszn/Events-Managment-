import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from '../services/event.service';
import { InvitationService } from '../../invitation/services/invitation.service';
import { NotificationService } from '../../notification/services/notification.service';
import { AuthService } from '../../user/core/services/auth.service';
import { AuthManagerService } from '../../user/core/services/auth-manager.service';
import { forkJoin } from 'rxjs';
import { Seat, OccupiedSeat } from '../models/seat.model';
import { interval, Subscription, takeUntil, take } from 'rxjs';
import { Subject } from 'rxjs';
import { filter } from 'rxjs/operators';

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

      <div class="seats-container">
        <div *ngFor="let row of seatingLayout; let i = index" class="row">
          <div class="row-number">{{i + 1}}</div>
          <div *ngFor="let seat of row; let j = index"
               class="seat"
               [class.occupied]="seat.isOccupied"
               [class.selected]="seat.isSelected"
               [class.locked]="seat.isLocked"
               (click)="onSeatClick(seat)">
            {{j + 1}}
          </div>
        </div>
      </div>

      <div class="legend">
        <div class="legend-item">
          <div class="seat"></div>
          <span>Disponible</span>
        </div>
        <div class="legend-item">
          <div class="seat occupied"></div>
          <span>Occupé</span>
        </div>
        <div class="legend-item">
          <div class="seat selected"></div>
          <span>Sélectionné</span>
        </div>

      </div>

      <div class="actions">
        <button class="btn btn-confirm" (click)="confirmSelection()" [disabled]="!selectedSeat">
          <i class="fas fa-check"></i>
          Confirmer la sélection
        </button>
        <button class="btn btn-cancel" (click)="cancelSelection()" [disabled]="!selectedSeat">
          <i class="fas fa-times"></i>
          Annuler la sélection
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
      width: 60%;
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

    .seats-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 400px;
      margin: 0 auto;
      padding: 20px;
      background: #f8f9fa;
      border-radius: 10px;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    }

    .row {
      display: flex;
      gap: 8px;
      align-items: center;
      justify-content: center;
    }

    .row-number {
      width: 30px;
      text-align: center;
      font-weight: bold;
      color: #666;
      background: #e9ecef;
      border-radius: 4px;
      padding: 8px 4px;
      margin-right: 10px;
    }

    .seat {
      width: 40px;
      height: 40px;
      border: 2px solid #dee2e6;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s ease;
      background: #ffffff;
      border-radius: 8px;
      font-weight: 600;
      font-size: 12px;
      color: #495057;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .seat:hover:not(.occupied):not(.locked) {
      background-color: #e3f2fd;
      border-color: #2196f3;
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0,0,0,0.15);
    }

    .seat.occupied {
      background-color: #dc3545;
      color: white;
      cursor: not-allowed;
      border-color: #dc3545;
      font-weight: bold;
      box-shadow: 0 2px 4px rgba(220, 53, 69, 0.3);
    }



    .seat.selected {
      background-color: #4CAF50;
      color: white;
      border-color: #28a745;
      font-weight: bold;
      box-shadow: 0 4px 8px rgba(40, 167, 69, 0.4);
      transform: scale(1.05);
    }



    .legend {
      display: flex;
      justify-content: center;
      gap: 40px;
      margin: 30px 0;
      padding: 20px;
      background: #ffffff;
      border-radius: 10px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 500;
      color: #495057;
    }

    .legend-item .seat {
      width: 24px;
      height: 24px;
      cursor: default;
      font-size: 10px;
    }

    .actions {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-top: 30px;
      padding: 20px;
    }

    .btn {
      padding: 12px 24px;
      border: none;
      border-radius: 8px;
      font-weight: 600;
      font-size: 16px;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 180px;
      justify-content: center;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .btn:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0,0,0,0.15);
    }

    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      transform: none;
      box-shadow: 0 1px 2px rgba(0,0,0,0.1);
    }

    .btn-confirm {
      background: linear-gradient(135deg, #28a745, #20c997);
      color: white;
      border: 2px solid transparent;
    }

    .btn-confirm:hover:not(:disabled) {
      background: linear-gradient(135deg, #218838, #1ea080);
      box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
    }

    .btn-cancel {
      background: linear-gradient(135deg, #6c757d, #5a6268);
      color: white;
      border: 2px solid transparent;
    }

    .btn-cancel:hover:not(:disabled) {
      background: linear-gradient(135deg, #5a6268, #495057);
      box-shadow: 0 4px 12px rgba(108, 117, 125, 0.3);
    }

  `]
})
export class SeatSelectionComponent implements OnInit, OnDestroy {
  seatingLayout: Seat[][] = [];
  selectedSeat: Seat | null = null;
  availableSeats: number = 0;
  eventId: string = '';
  eventTitle: string = '';
  userEmail: string = '';
  occupiedSeats: OccupiedSeat[] = [];
  lockedSeats: OccupiedSeat[] = [];
  private refreshSubscription?: Subscription;
  private readonly ROWS = 1;
  private readonly SEATS_PER_ROW = 5;
  private isLoading: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private invitationService: InvitationService,
    private notificationService: NotificationService,
    private authService: AuthService,
    private authManagerService: AuthManagerService
  ) {}

  async ngOnInit() {
    try {
      const profile = await this.authService.loadUserProfile();
      this.userEmail = profile?.email || '';

      this.route.params.subscribe(params => {
        this.eventId = params['id'];
        this.loadEventDetailsAndSeats();
      });

      // Vérifier s'il y a un verrou existant
      this.eventService.checkStoredLock();

      // S'abonner aux changements des places verrouillées
      this.eventService.lockedSeats$
        .pipe(
          takeUntil(this.destroy$),
          filter(state => state.eventId === this.eventId)
        )
        .subscribe(state => {
          this.updateLockedSeats(state.seats);
        });

      // Rafraîchir les places occupées toutes les 3 secondes
      this.refreshSubscription = interval(3000)
        .pipe(
          takeUntil(this.destroy$)
        )
        .subscribe(() => {
          if (!this.isLoading) {
            this.loadOccupiedSeats();
          }
        });

      // Écouter les événements de fermeture de page
      window.addEventListener('beforeunload', this.onBeforeUnload);
    } catch (error) {
      console.error('Error initializing component:', error);
      this.notificationService.show({
        message: 'Erreur lors de l\'initialisation.',
        type: 'error',
        duration: 5000
      });
    }
  }

  private destroy$ = new Subject<void>();

  ngOnDestroy() {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
    this.eventService.stopSeatLockTimer();
    window.removeEventListener('beforeunload', this.onBeforeUnload);
  }

  private onBeforeUnload = () => {
    this.eventService.stopSeatLockTimer();
  }

  loadOccupiedSeats() {
    this.invitationService.getOccupiedSeats(this.eventId).subscribe({
      next: (occupiedSeats) => {
        this.occupiedSeats = occupiedSeats;
        this.updateOccupiedSeatsWithSelection(occupiedSeats);
        this.updateAvailableSeats();
      },
      error: (error) => {
        console.error('Error loading occupied seats:', error);
        this.notificationService.show({
          message: 'Erreur lors du chargement des places occupées.',
          type: 'error',
          duration: 5000
        });
      }
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
        this.occupiedSeats = result.occupiedSeats;
        this.loadSeatingLayout(result.occupiedSeats);
      },
      error: (error) => {
        console.error('Error loading event details or seats:', error);
        this.notificationService.show({
          message: 'Erreur lors du chargement des informations.',
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  loadSeatingLayout(occupiedSeats: OccupiedSeat[]) {
    this.seatingLayout = Array(this.ROWS).fill(null).map((_, rowIndex) =>
      Array(this.SEATS_PER_ROW).fill(null).map((_, seatIndex) => ({
        row: rowIndex + 1,
        number: seatIndex + 1,
        isOccupied: false,
        isSelected: false,
        isLocked: false
      }))
    );

    // Marquer immédiatement les places occupées
    this.updateOccupiedSeats(occupiedSeats);
    this.updateAvailableSeats();
  }

  updateOccupiedSeats(occupiedSeats: OccupiedSeat[]) {
    // Réinitialiser toutes les places comme non occupées
    this.seatingLayout.forEach(row => {
      row.forEach(seat => {
        seat.isOccupied = false;
        seat.isLocked = false;
      });
    });

    // Marquer les places occupées (toutes les places dans occupiedSeats sont considérées comme occupées)
    occupiedSeats.forEach(occupiedSeat => {
      const seat = this.seatingLayout[occupiedSeat.row - 1]?.[occupiedSeat.number - 1];
      if (seat) {
        seat.isOccupied = true;
      }
    });
  }

  updateOccupiedSeatsWithSelection(occupiedSeats: OccupiedSeat[]) {
    // Réinitialiser toutes les places comme non occupées (sauf la sélectionnée)
    this.seatingLayout.forEach(row => {
      row.forEach(seat => {
        if (!this.isSelectedSeat(seat)) {
          seat.isOccupied = false;
          seat.isLocked = false;
        }
      });
    });

    // Marquer les places occupées
    occupiedSeats.forEach(occupiedSeat => {
      const seat = this.findSeat(occupiedSeat.row, occupiedSeat.number);
      if (seat && !this.isSelectedSeat(seat)) {
        seat.isOccupied = true;
      }
    });

    // Réappliquer les verrous
    this.eventService.lockedSeats$.pipe(
      take(1)
    ).subscribe(currentLockedSeats => {
      if (currentLockedSeats.eventId === this.eventId) {
        this.updateLockedSeats(currentLockedSeats.seats);
      }
    });
  }

  updateLockedSeats(lockedSeats: any[]) {
    // Mettre à jour les places verrouillées dans le layout
    this.seatingLayout.forEach(row => {
      row.forEach(seat => {
        // Vérifier si la place est verrouillée
        const isLocked = lockedSeats.some(
          locked => locked.row === seat.row && locked.number === seat.number
        );
        
        // Si la place est verrouillée et n'est pas la place sélectionnée
        if (isLocked && !this.isSelectedSeat(seat)) {
          seat.isLocked = true;
          seat.isOccupied = true;
        }
        // Si la place n'est pas verrouillée et n'est pas occupée
        else if (!isLocked && !this.occupiedSeats.some(
          occupied => occupied.row === seat.row && occupied.number === seat.number
        )) {
          seat.isLocked = false;
          seat.isOccupied = false;
        }
      });
    });
    this.updateAvailableSeats();
  }

  onSeatClick(seat: Seat) {
    if (seat.isOccupied || seat.isLocked) {
      this.notificationService.show({
        message: 'Cette place est déjà occupée ou verrouillée.',
        type: 'info',
        duration: 3000
      });
      return;
    }

    if (this.selectedSeat) {
      // Si on clique sur le même siège, on le désélectionne
      if (this.isSelectedSeat(seat)) {
        this.cancelSelection();
        return;
      }
      // Si on clique sur un autre siège, on libère d'abord le siège actuel
      this.eventService.stopSeatLockTimer();
      this.selectedSeat.isSelected = false;
      this.selectedSeat.isLocked = false;
      this.selectedSeat.isOccupied = false;
    }

    seat.isSelected = true;
    this.selectedSeat = seat;

    this.eventService.lockSeat(this.eventId, seat.row, seat.number).subscribe({
      next: () => {
        this.eventService.startSeatLockTimer(this.eventId, seat.row, seat.number);
        seat.isLocked = true;
        seat.isOccupied = true;
        // Rafraîchir immédiatement pour voir les changements
        this.loadOccupiedSeats();
      },
      error: (error) => {
        console.error('Erreur lors du verrouillage de la place:', error);
        seat.isSelected = false;
        seat.isLocked = false;
        seat.isOccupied = false;
        this.selectedSeat = null;
        this.notificationService.show({
          message: 'Erreur lors du verrouillage de la place. Veuillez réessayer.',
          type: 'error',
          duration: 5000
        });
      }
    });
  }

  updateAvailableSeats() {
    this.availableSeats = this.seatingLayout.reduce((total, row) => {
      return total + row.filter(seat => !seat.isOccupied && !seat.isLocked).length;
    }, 0);
  }



  private findSeat(row: number, number: number): Seat | null {
    return this.seatingLayout[row - 1]?.[number - 1] || null;
  }

  private isSelectedSeat(seat: Seat): boolean {
    return this.selectedSeat?.row === seat.row && this.selectedSeat?.number === seat.number;
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
          message: `place reservée pour l'événement : ${this.eventTitle} - Place : Rangée ${request.seatInfo.row}, Siège ${request.seatInfo.number} , un mail vous sera communiqué pour confirmation`,
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

  cancelSelection() {
    if (!this.selectedSeat) return;

    const seat = this.selectedSeat;
    this.eventService.stopSeatLockTimer();

    // Libérer le verrou sur le serveur
    this.eventService.releaseSeat(this.eventId, seat.row, seat.number).subscribe({
      next: () => {
        seat.isSelected = false;
        seat.isLocked = false;
        this.selectedSeat = null;
        // Rafraîchir pour voir les changements
        this.loadOccupiedSeats();
      },
      error: (error) => {
        console.error('Erreur lors de la libération de la place:', error);
        this.notificationService.show({
          message: 'Erreur lors de la libération de la place.',
          type: 'error',
          duration: 5000
        });
      }
    });
  }
}
