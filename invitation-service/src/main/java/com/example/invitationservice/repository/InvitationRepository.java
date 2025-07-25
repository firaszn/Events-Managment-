package com.example.invitationservice.repository;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.SeatInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    boolean existsByEventIdAndUserEmail(Long eventId, String userEmail);
    Optional<InvitationEntity> findByEventIdAndUserEmail(Long eventId, String userEmail);

    @Query("SELECT COUNT(i) > 0 FROM InvitationEntity i WHERE i.eventId = :eventId " +
           "AND i.userEmail = :userEmail AND i.status = 'CONFIRMED'")
    boolean existsByEventIdAndUserEmailAndStatusConfirmed(@Param("eventId") Long eventId, @Param("userEmail") String userEmail);

    @Query("SELECT COUNT(i) > 0 FROM InvitationEntity i WHERE i.eventId = :eventId " +
           "AND i.userEmail = :userEmail AND i.status IN ('PENDING', 'WAITLIST')")
    boolean existsByEventIdAndUserEmailAndStatusPending(@Param("eventId") Long eventId, @Param("userEmail") String userEmail);
    
    @Query("SELECT COUNT(i) > 0 FROM InvitationEntity i WHERE i.eventId = :eventId " +
           "AND i.seatInfo.row = :#{#seatInfo.row} " +
           "AND i.seatInfo.number = :#{#seatInfo.number} " +
           "AND i.seatInfo IS NOT NULL " +
           "AND i.status = 'CONFIRMED'")
    boolean existsByEventIdAndSeatInfo(@Param("eventId") Long eventId, @Param("seatInfo") SeatInfo seatInfo);

    @Query("SELECT i FROM InvitationEntity i WHERE i.eventId = :eventId " +
           "AND i.seatInfo IS NOT NULL " +
           "AND i.status = 'CONFIRMED'")
    List<InvitationEntity> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT DISTINCT i.seatInfo FROM InvitationEntity i " +
           "WHERE i.eventId = :eventId " +
           "AND i.seatInfo IS NOT NULL " +
           "AND i.status = 'CONFIRMED'")
    List<SeatInfo> findOccupiedSeats(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(i) FROM InvitationEntity i " +
           "WHERE i.eventId = :eventId " +
           "AND i.status = 'CONFIRMED'")
    Long countConfirmedInvitations(@Param("eventId") Long eventId);
} 