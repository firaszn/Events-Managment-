package com.example.invitationservice.repository;

import com.example.invitationservice.entity.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    boolean existsByEventIdAndUserEmail(Long eventId, String userEmail);
    Optional<InvitationEntity> findByEventIdAndUserEmail(Long eventId, String userEmail);
} 