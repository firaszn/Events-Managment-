package com.example.eventservice.repository;

import com.example.eventservice.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour la gestion des événements
 */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    /**
     * Trouve tous les événements d'un organisateur
     * @param organizerId ID de l'organisateur
     * @return Liste des événements
     */
    List<EventEntity> findByOrganizerId(Long organizerId);

    /**
     * Trouve tous les événements d'un organisateur triés par date
     * @param organizerId ID de l'organisateur
     * @return Liste des événements triés par date
     */
    List<EventEntity> findByOrganizerIdOrderByDateTimeAsc(Long organizerId);

    /**
     * Trouve tous les événements futurs d'un organisateur
     * @param organizerId ID de l'organisateur
     * @param now Date/heure actuelle
     * @return Liste des événements futurs
     */
    @Query("SELECT e FROM EventEntity e WHERE e.organizerId = :organizerId AND e.dateTime > :now ORDER BY e.dateTime ASC")
    List<EventEntity> findUpcomingEventsByOrganizer(@Param("organizerId") Long organizerId, @Param("now") LocalDateTime now);

    /**
     * Trouve tous les événements futurs
     * @param now Date/heure actuelle
     * @return Liste des événements futurs
     */
    @Query("SELECT e FROM EventEntity e WHERE e.dateTime > :now ORDER BY e.dateTime ASC")
    List<EventEntity> findUpcomingEvents(@Param("now") LocalDateTime now);

    /**
     * Trouve tous les événements passés d'un organisateur
     * @param organizerId ID de l'organisateur
     * @param now Date/heure actuelle
     * @return Liste des événements passés
     */
    @Query("SELECT e FROM EventEntity e WHERE e.organizerId = :organizerId AND e.dateTime < :now ORDER BY e.dateTime DESC")
    List<EventEntity> findPastEventsByOrganizer(@Param("organizerId") Long organizerId, @Param("now") LocalDateTime now);

    /**
     * Trouve les événements par titre (recherche partielle)
     * @param title Titre à rechercher
     * @return Liste des événements correspondants
     */
    @Query("SELECT e FROM EventEntity e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY e.dateTime ASC")
    List<EventEntity> findByTitleContainingIgnoreCase(@Param("title") String title);

    /**
     * Trouve les événements par lieu (recherche partielle)
     * @param location Lieu à rechercher
     * @return Liste des événements correspondants
     */
    @Query("SELECT e FROM EventEntity e WHERE LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%')) ORDER BY e.dateTime ASC")
    List<EventEntity> findByLocationContainingIgnoreCase(@Param("location") String location);

    /**
     * Trouve les événements dans une plage de dates
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste des événements dans la plage
     */
    @Query("SELECT e FROM EventEntity e WHERE e.dateTime BETWEEN :startDate AND :endDate ORDER BY e.dateTime ASC")
    List<EventEntity> findEventsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Compte le nombre d'événements d'un organisateur
     * @param organizerId ID de l'organisateur
     * @return Nombre d'événements
     */
    long countByOrganizerId(Long organizerId);
}
