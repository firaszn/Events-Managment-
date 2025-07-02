package com.example.invitationservice.repository;

import com.example.invitationservice.entity.InvitationEntity;
import com.example.invitationservice.entity.SeatInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    boolean existsByEventIdAndUserEmail(Long eventId, String userEmail);
    Optional<InvitationEntity> findByEventIdAndUserEmail(Long eventId, String userEmail);
    
    @Query("SELECT COUNT(i) > 0 FROM InvitationEntity i WHERE i.eventId = :eventId AND i.seatInfo.row = :#{#seatInfo.row} AND i.seatInfo.number = :#{#seatInfo.number}")
    boolean existsByEventIdAndSeatInfo(Long eventId, SeatInfo seatInfo);

    List<InvitationEntity> findByEventId(Long eventId);
} 