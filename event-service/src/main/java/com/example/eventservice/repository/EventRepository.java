package com.example.eventservice.repository;

import com.example.eventservice.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
} 