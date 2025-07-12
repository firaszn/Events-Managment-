export interface Seat {
    id?: number;
    row: number;
    number: number;
    isOccupied: boolean;
    isSelected: boolean;
    isLocked: boolean;
}

export interface OccupiedSeat {
    row: number;
    number: number;
} 