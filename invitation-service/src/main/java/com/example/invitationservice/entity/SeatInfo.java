package com.example.invitationservice.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatInfo {
    @JsonProperty("row")
    private Integer row;
    
    @JsonProperty("number")
    private Integer number;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeatInfo seatInfo = (SeatInfo) o;
        return Objects.equals(row, seatInfo.row) && 
               Objects.equals(number, seatInfo.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, number);
    }
    
    @Override
    public String toString() {
        return String.format("SeatInfo(row=%d, number=%d)", row, number);
    }
} 