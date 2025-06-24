package com.example.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Application principale du Event Service
 *
 * Microservice pour la gestion des événements dans l'application d'organisation d'événements
 *
 * Fonctionnalités :
 * - Créer, modifier, supprimer des événements
 * - Gérer les événements par organisateur
 * - Publier des événements Kafka pour les notifications
 * - Intégration avec Eureka pour la découverte de services
 * - Configuration centralisée via Config Server
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EventServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventServiceApplication.class, args);
	}
}
