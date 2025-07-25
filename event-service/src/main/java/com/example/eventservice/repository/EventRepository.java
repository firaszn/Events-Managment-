package com.example.eventservice.repository;

import com.example.eventservice.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
 
@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    
    /**
     * Trouve les événements qui commencent dans une plage de temps donnée
     * Utilisé pour les rappels automatiques
     */
    @Query("SELECT e FROM EventEntity e WHERE e.eventDate BETWEEN :startTime AND :endTime")
    List<EventEntity> findEventsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
} 