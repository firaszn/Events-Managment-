package com.example.invitationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Application principale du Invitation Service
 *
 * Microservice pour la gestion des invitations dans l'application d'organisation d'événements
 *
 * Fonctionnalités :
 * - Créer et gérer les invitations aux événements
 * - Gérer les réponses aux invitations (accepter/refuser)
 * - Publier des événements Kafka pour les notifications
 * - Intégration avec Eureka pour la découverte de services
 * - Configuration centralisée via Config Server
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
public class InvitationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvitationServiceApplication.class, args);
	}
}
