# 🎯 Application d'Organisation d'Événements - Architecture Microservices

Une application complète d'organisation d'événements développée avec **Spring Boot**, **Angular**, **PostgreSQL**, **Kafka** et **Keycloak** en architecture microservices avec gestion avancée des listes d'attente et notifications en temps réel.

## 📋 Table des Matières

- [🎯 Objectif](#-objectif)
- [🏗️ Architecture](#️-architecture)
- [🧱 Microservices](#-microservices)
- [🚀 Technologies](#-technologies)
- [📦 Prérequis](#-prérequis)
- [⚙️ Installation](#️-installation)
- [🔧 Configuration](#-configuration)
- [🚀 Démarrage](#-démarrage)
- [📡 APIs](#-apis)
- [🔐 Authentification & Sécurité](#-authentification--sécurité)
- [📩 Intégration Kafka](#-intégration-kafka)
- [📧 Service de Notifications](#-service-de-notifications)
- [🔄 Resilience4j & Retry](#-resilience4j--retry)
- [📊 Monitoring & Métriques](#-monitoring--métriques)
- [🧪 Tests](#-tests)
- [📚 Documentation](#-documentation)
- [🔧 Dépannage](#-dépannage)
- [🚀 Déploiement](#-déploiement)

## 🎯 Objectif

Permettre aux utilisateurs de :
- ✅ **Créer et gérer des événements** (réunions, conférences, etc.)
- ✅ **Inviter des participants** aux événements avec gestion des places
- ✅ **Gérer les inscriptions** et réponses aux invitations
- ✅ **Système de liste d'attente** avec notifications automatiques
- ✅ **Gestion des places** avec sélection de sièges
- ✅ **Notifications en temps réel** via Kafka
- ✅ **Authentification sécurisée** avec Keycloak
- ✅ **Interface utilisateur moderne** avec Angular
- ✅ **Résilience et retry** avec Resilience4j
- ✅ **Monitoring complet** avec Prometheus et Grafana

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │  Config Server  │
│   Angular       │    │   Port: 8080    │    │   Port: 8888    │
│   Port: 4200    │    │   + Keycloak    │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
    │  User Service   │    │  Event Service  │    │Invitation Service│
    │   Port: 8084    │    │   Port: 8082    │    │   Port: 8083    │
    │  + PostgreSQL   │    │  + PostgreSQL   │    │  + PostgreSQL   │
    │  + Keycloak     │    │  + Waitlist     │    │  + Seat Mgmt    │
    └─────────────────┘    └─────────────────┘    └─────────────────┘
                                 │                       │
                                 └───────────────────────┘
                                          │
                                    ┌─────────────────┐
                                    │     Kafka       │
                                    │  (Notifications)│
                                    └─────────────────┘
                                          │
                                    ┌─────────────────┐
                                    │Notification Svc │
                                    │   Port: 8085    │
                                    │  + Email + DLQ  │
                                    └─────────────────┘
```

## 🧱 Microservices

### 1. 👥 **User Service** (Port 8084)
**Rôle :** Gérer les utilisateurs avec authentification Keycloak

**Fonctionnalités :**
- ✅ Authentification OAuth2/JWT avec Keycloak
- ✅ Gestion des profils utilisateurs
- ✅ Synchronisation avec Keycloak
- ✅ Gestion des rôles (USER, ADMIN)
- ✅ Inscription et connexion sécurisées

**Endpoints :**
- `POST /auth/register` – Inscription utilisateur
- `POST /auth/login` – Connexion
- `GET /api/users/profile` – Profil utilisateur
- `PUT /api/users/profile` – Mettre à jour le profil
- `GET /api/users` – Liste des utilisateurs (Admin)

### 2. 🎪 **Event Service** (Port 8082)
**Rôle :** Gestion complète des événements avec liste d'attente

**Fonctionnalités :**
- ✅ CRUD événements
- ✅ Système de liste d'attente avancé
- ✅ Gestion des capacités et places
- ✅ Notifications automatiques
- ✅ Intégration avec Invitation Service

**Endpoints :**
- `POST /events` – Créer événement
- `GET /events` – Lister tous les événements
- `GET /events/{id}` – Détails événement
- `PUT /events/{id}` – Modifier événement
- `DELETE /events/{id}` – Supprimer événement
- `POST /events/{id}/waitlist/join` – Rejoindre liste d'attente
- `POST /events/{id}/waitlist/confirm` – Confirmer place
- `DELETE /events/{id}/waitlist/leave` – Quitter liste d'attente

### 3. 🎫 **Invitation Service** (Port 8083)
**Rôle :** Gestion des invitations et places avec sélection de sièges

**Fonctionnalités :**
- ✅ Gestion des invitations
- ✅ Sélection et réservation de places
- ✅ Verrouillage temporaire des sièges
- ✅ Intégration avec Event Service
- ✅ Gestion des statuts (PENDING, CONFIRMED, CANCELLED, WAITLIST)

**Endpoints :**
- `POST /invitations` – Créer invitation
- `PUT /invitations/{id}/confirm` – Confirmer invitation
- `PUT /invitations/{id}/cancel` – Annuler invitation
- `GET /invitations/event/{eventId}` – Invitations d'un événement
- `GET /invitations/user/{email}` – Invitations d'un utilisateur
- `POST /invitations/{eventId}/seats/lock` – Verrouiller place
- `DELETE /invitations/{eventId}/seats/release` – Libérer place

### 4. 📧 **Notification Service** (Port 8085)
**Rôle :** Service de notifications avec Resilience4j

**Fonctionnalités :**
- ✅ Envoi d'emails automatiques
- ✅ Consommation des messages Kafka
- ✅ Dead Letter Queue (DLQ)
- ✅ Retry automatique avec Resilience4j
- ✅ Circuit Breaker et Bulkhead
- ✅ Monitoring des métriques

**Types de notifications :**
- 📧 Confirmation d'inscription
- 📧 Notifications de liste d'attente
- 📧 Rappels d'événements
- 📧 Promotions depuis la liste d'attente

### 5. 🌐 **API Gateway** (Port 8080)
**Rôle :** Point d'entrée unique avec sécurité Keycloak

**Fonctionnalités :**
- ✅ Routage intelligent
- ✅ Authentification JWT
- ✅ CORS configuré
- ✅ Rate limiting
- ✅ Logging des requêtes

### 6. 🔍 **Eureka Discovery** (Port 8761)
**Rôle :** Service de découverte et enregistrement des microservices

### 7. ⚙️ **Config Server** (Port 8888)
**Rôle :** Gestion centralisée des configurations

### 8. 🔐 **Keycloak** (Port 8081)
**Rôle :** Serveur d'authentification et autorisation

**Fonctionnalités :**
- ✅ Authentification OAuth2/JWT
- ✅ Gestion des utilisateurs
- ✅ Gestion des rôles
- ✅ Single Sign-On (SSO)

## 🚀 Technologies

| Technologie | Version | Usage |
|-------------|---------|-------|
| **Java** | 17 | Langage principal |
| **Spring Boot** | 3.4.2 | Framework microservices |
| **Spring Cloud** | 2024.0.0 | Outils microservices |
| **Spring Security** | 6.x | Sécurité et authentification |
| **Spring Cloud Gateway** | 4.x | API Gateway |
| **Spring Cloud OpenFeign** | 4.x | Client HTTP déclaratif |
| **Spring Data JPA** | 3.x | Persistance des données |
| **PostgreSQL** | 12+ | Base de données |
| **Apache Kafka** | 2.8+ | Messaging asynchrone |
| **Keycloak** | 24.x | Authentification et autorisation |
| **Resilience4j** | 2.x | Patterns de résilience |
| **Angular** | 17.x | Frontend |
| **Maven** | 3.8+ | Gestionnaire de dépendances |
| **Docker** | 20+ | Conteneurisation |
| **Prometheus** | 2.x | Monitoring |
| **Grafana** | 10.x | Visualisation des métriques |

## 📦 Prérequis

- ☕ **Java 17+**
- 🐘 **PostgreSQL 12+**
- 📨 **Apache Kafka 2.8+**
- 🔐 **Keycloak 24+**
- 🔧 **Maven 3.8+**
- 🐳 **Docker** (optionnel)
- 📊 **Prometheus & Grafana** (optionnel)

## ⚙️ Installation

### 1. **Cloner le Projet**
```bash
git clone <votre-repo>
cd Stage-GTI-2025
```

### 2. **Créer les Bases de Données**
```sql
-- Dans pgAdmin ou psql
CREATE DATABASE userdb;
CREATE DATABASE eventdb;
CREATE DATABASE invitationdb;
CREATE DATABASE notificationdb;
```

### 3. **Configurer PostgreSQL**
Vérifiez que PostgreSQL fonctionne sur le port `5432` avec :
- **Utilisateur :** `postgres`
- **Mot de passe :** `postgres`

### 4. **Installer et Configurer Keycloak**
```bash
# Télécharger Keycloak
wget https://github.com/keycloak/keycloak/releases/download/24.0.0/keycloak-24.0.0.tar.gz
tar -xzf keycloak-24.0.0.tar.gz
cd keycloak-24.0.0

# Démarrer Keycloak
./bin/kc.sh start-dev
```

**Configuration Keycloak :**
1. Créer un realm `event-management`
2. Créer un client `event-app`
3. Configurer les utilisateurs et rôles
4. Exporter la configuration

### 5. **Installer Kafka**
```bash
# Télécharger et démarrer Kafka sur localhost:9092
# Ou utiliser Docker :
docker run -p 9092:9092 apache/kafka:2.8.0
```

### 6. **Installer le Frontend Angular**
```bash
cd frontend
npm install
```

## 🔧 Configuration

### **Configuration Centralisée (Config Server)**
Les configurations sont centralisées dans le **Config Server** :
- `config-server/src/main/resources/configurations/`
  - `api-gateway.properties`
  - `USER.properties`
  - `EVENT.properties`
  - `INVITATION-SERVICE.properties`
  - `NOTIFICATION-SERVICE.properties`
  - `eureka-server.properties`

### **Configuration Keycloak**
```properties
# Dans chaque service
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/event-management
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/realms/event-management/protocol/openid-connect/certs
```

### **Configuration Resilience4j**
```properties
# Retry
resilience4j.retry.instances.emailRetry.maxAttempts=3
resilience4j.retry.instances.emailRetry.waitDuration=2s

# Circuit Breaker
resilience4j.circuitbreaker.instances.emailCircuitBreaker.failureRateThreshold=50
resilience4j.circuitbreaker.instances.emailCircuitBreaker.waitDurationInOpenState=60s

# Bulkhead
resilience4j.bulkhead.instances.emailBulkhead.maxConcurrentCalls=10
```

## 🚀 Démarrage

### **Avec Docker Compose (Recommandé)**
```bash
# Démarrer tous les services
docker-compose up -d

# Vérifier les services
docker-compose ps
```

### **Démarrage Manuel**

**Ordre de Démarrage Recommandé :**

1. **Eureka Discovery Server**
```bash
mvn spring-boot:run -pl eureka-server
```
📍 Accès : http://localhost:8761

2. **Config Server**
```bash
mvn spring-boot:run -pl config-server
```
📍 Accès : http://localhost:8888

3. **Keycloak**
```bash
# Démarrer Keycloak
./bin/kc.sh start-dev
```
📍 Accès : http://localhost:8081

4. **User Service**
```bash
mvn spring-boot:run -pl user-service
```
📍 Accès : http://localhost:8084

5. **Event Service**
```bash
mvn spring-boot:run -pl event-service
```
📍 Accès : http://localhost:8082

6. **Invitation Service**
```bash
mvn spring-boot:run -pl invitation-service
```
📍 Accès : http://localhost:8083

7. **Notification Service**
```bash
mvn spring-boot:run -pl notification-service
```
📍 Accès : http://localhost:8085

8. **API Gateway**
```bash
mvn spring-boot:run -pl api-gateway
```
📍 Accès : http://localhost:8080

9. **Frontend Angular**
```bash
cd frontend
ng serve
```
📍 Accès : http://localhost:4200

## 📡 APIs

### 🔐 **Authentification**

**Inscription :**
```bash
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john.doe@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Connexion :**
```bash
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "john.doe",
  "password": "password123"
}
```

### 🎪 **Event Service**

**Créer un événement :**
```bash
POST http://localhost:8080/events
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "title": "Conférence Tech 2025",
  "description": "Conférence sur les nouvelles technologies",
  "location": "Centre de Congrès",
  "eventDate": "2025-06-15T14:00:00",
  "maxCapacity": 100,
  "waitlistEnabled": true
}
```

**Rejoindre la liste d'attente :**
```bash
POST http://localhost:8080/events/1/waitlist/join
Authorization: Bearer <jwt-token>
```

**Confirmer une place depuis la liste d'attente :**
```bash
POST http://localhost:8080/events/1/waitlist/confirm
Authorization: Bearer <jwt-token>
```

### 🎫 **Invitation Service**

**Créer une invitation :**
```bash
POST http://localhost:8080/invitations
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "eventId": 1,
  "userEmail": "john.doe@example.com",
  "seatRow": 1,
  "seatNumber": 5
}
```

**Confirmer une invitation :**
```bash
PUT http://localhost:8080/invitations/1/confirm
Authorization: Bearer <jwt-token>
```

### 📧 **Notification Service**

**Métriques Resilience4j :**
```bash
GET http://localhost:8085/api/metrics/resilience4j
Authorization: Bearer <jwt-token>
```

## 🔐 Authentification & Sécurité

### **Keycloak Configuration**
- **Realm :** `event-management`
- **Client :** `event-app`
- **Rôles :** `USER`, `ADMIN`
- **Protocole :** OAuth2/JWT

### **Sécurité par Service**
- ✅ **API Gateway :** Validation JWT, CORS, Rate Limiting
- ✅ **User Service :** Intégration Keycloak, gestion des profils
- ✅ **Event Service :** Autorisation basée sur les rôles
- ✅ **Invitation Service :** Sécurité des invitations
- ✅ **Notification Service :** Métriques sécurisées

### **JWT Token Structure**
```json
{
  "sub": "user-id",
  "email": "user@example.com",
  "realm_access": {
    "roles": ["USER", "ADMIN"]
  },
  "exp": 1640995200
}
```

## 📩 Intégration Kafka

### **Topics Kafka :**

| Événement | Producteur | Topic | Consommateur | Description |
|-----------|------------|-------|--------------|-------------|
| Réponse invitation | invitation-service | `invitation.responded` | notification-service | Confirmation d'inscription |
| Rappel événement | event-service | `event.reminder` | notification-service | Rappel 1h avant |
| Notification liste d'attente | event-service | `waitlist.notification` | notification-service | Place disponible |
| Promotion liste d'attente | event-service | `waitlist.promotion` | notification-service | Promotion automatique |
| Dead Letter Queue | notification-service | `notification.dlq` | notification-service | Messages en échec |

### **Configuration Kafka :**
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.group-id=notification-group
spring.kafka.consumer.auto-offset-reset=earliest
```

## 📧 Service de Notifications

### **Fonctionnalités :**
- ✅ **Envoi d'emails** avec templates personnalisés
- ✅ **Retry automatique** avec Resilience4j
- ✅ **Circuit Breaker** pour la résilience
- ✅ **Dead Letter Queue** pour les échecs
- ✅ **Monitoring** des métriques

### **Types de Notifications :**
1. **Confirmation d'inscription** - Email de confirmation
2. **Notification liste d'attente** - Place disponible
3. **Promotion automatique** - Confirmation de place
4. **Rappel d'événement** - 1h avant l'événement

### **Configuration Email :**
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## 🔄 Resilience4j & Retry

### **Patterns Implémentés :**

#### **1. Retry Pattern**
```java
@Retry(name = "emailRetry", fallbackMethod = "emailRetryFallback")
public CompletableFuture<Void> sendEmailWithRetry(String to, String subject, String text)
```

#### **2. Circuit Breaker Pattern**
```java
@CircuitBreaker(name = "emailCircuitBreaker", fallbackMethod = "emailCircuitBreakerFallback")
```

#### **3. Bulkhead Pattern**
```java
@Bulkhead(name = "emailBulkhead", fallbackMethod = "emailBulkheadFallback")
```

#### **4. Time Limiter Pattern**
```java
@TimeLimiter(name = "emailTimeLimiter", fallbackMethod = "emailTimeLimiterFallback")
```

### **Configuration :**
- **Max Attempts :** 3
- **Wait Duration :** 2 secondes
- **Failure Rate Threshold :** 50%
- **Sliding Window Size :** 10
- **Max Concurrent Calls :** 10

## 📊 Monitoring & Métriques

### **Endpoints de Monitoring :**
- **Health Check :** `/actuator/health`
- **Métriques :** `/actuator/metrics`
- **Prometheus :** `/actuator/prometheus`
- **Resilience4j :** `/api/metrics/resilience4j`

### **Métriques Disponibles :**
- ✅ **Circuit Breaker :** État, taux d'échec, appels
- ✅ **Retry :** Tentatives, succès, échecs
- ✅ **Bulkhead :** Appels simultanés, rejets
- ✅ **Time Limiter :** Timeouts
- ✅ **Kafka :** Messages envoyés/reçus
- ✅ **Email :** Emails envoyés, échecs

### **Prometheus & Grafana :**
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'notification-service'
    static_configs:
      - targets: ['localhost:8085']
```

## 🧪 Tests

### **Tests Unitaires**
```bash
# Tester tous les services
mvn test

# Tester un service spécifique
mvn test -pl event-service
mvn test -pl invitation-service
mvn test -pl user-service
mvn test -pl notification-service
```

### **Tests d'Intégration**
```bash
# Vérifier que tous les services sont enregistrés dans Eureka
curl http://localhost:8761/eureka/apps

# Tester via le Gateway
curl http://localhost:8080/events
curl http://localhost:8080/invitations
curl http://localhost:8080/api/users
```

### **Tests des APIs avec curl**

**Authentification :**
```bash
# Obtenir un token JWT
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}'
```

**Créer un événement :**
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Event",
    "description": "Event de test",
    "location": "Salle Test",
    "eventDate": "2025-06-20T10:00:00",
    "maxCapacity": 5,
    "waitlistEnabled": true
  }'
```

**Rejoindre la liste d'attente :**
```bash
curl -X POST http://localhost:8080/events/1/waitlist/join \
  -H "Authorization: Bearer <jwt-token>"
```

## 📚 Documentation

### **Swagger UI**
- Event Service : http://localhost:8082/swagger-ui.html
- Invitation Service : http://localhost:8083/swagger-ui.html
- User Service : http://localhost:8084/swagger-ui.html
- Notification Service : http://localhost:8085/swagger-ui.html

### **Eureka Dashboard**
- URL : http://localhost:8761
- Visualisation de tous les services enregistrés

### **Keycloak Admin Console**
- URL : http://localhost:8081/admin
- Gestion des utilisateurs, rôles et clients

### **Config Server**
- URL : http://localhost:8888
- Endpoints de configuration :
  - http://localhost:8888/api-gateway/default
  - http://localhost:8888/EVENT/default
  - http://localhost:8888/INVITATION-SERVICE/default
  - http://localhost:8888/NOTIFICATION-SERVICE/default

### **Prometheus**
- URL : http://localhost:9090
- Métriques et alertes

### **Grafana**
- URL : http://localhost:3000
- Dashboards de monitoring

## 🔧 Dépannage

### **Problèmes Courants**

**1. Erreur de connexion PostgreSQL :**
```bash
# Vérifier que PostgreSQL fonctionne
pg_isready -h localhost -p 5432

# Vérifier les bases de données
psql -U postgres -l
```

**2. Services non enregistrés dans Eureka :**
- Vérifier que Eureka fonctionne sur le port 8761
- Vérifier les configurations `eureka.client.serviceUrl.defaultZone`

**3. Erreurs Kafka :**
```bash
# Vérifier que Kafka fonctionne
kafka-topics.sh --list --bootstrap-server localhost:9092
```

**4. Erreurs Keycloak :**
```bash
# Vérifier que Keycloak fonctionne
curl http://localhost:8081/realms/event-management/.well-known/openid_configuration
```

**5. Erreurs Resilience4j :**
```bash
# Vérifier les métriques
curl http://localhost:8085/api/metrics/resilience4j
```

**6. Port déjà utilisé :**
```bash
# Trouver le processus utilisant un port
netstat -ano | findstr :8082
# Tuer le processus si nécessaire
```

### **Logs de Debug**
```properties
# Ajouter dans application.properties pour plus de logs
logging.level.com.example=DEBUG
logging.level.org.springframework.cloud=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.io.github.resilience4j=DEBUG
```

## 🚀 Déploiement

### **Docker Compose**
```bash
# Démarrer tous les services
docker-compose up -d

# Vérifier les services
docker-compose ps

# Voir les logs
docker-compose logs -f
```

### **Production**
- Utiliser des profils Spring (`application-prod.properties`)
- Configurer des bases de données séparées
- Mettre en place un monitoring (Prometheus + Grafana)
- Utiliser un service mesh (Istio) pour la production
- Configurer des secrets pour les mots de passe
- Mettre en place des backups automatiques

### **CI/CD**
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build and Deploy
        run: |
          mvn clean package
          docker-compose -f docker-compose.prod.yml up -d
```

## 👥 Équipe de Développement

- **Développeur Principal :** [Votre Nom]
- **Stage :** GTI 2025
- **Technologies :** Spring Boot, Microservices, PostgreSQL, Kafka, Keycloak, Angular

## 📄 Licence

Ce projet est développé dans le cadre d'un stage GTI 2025.

---

## 🎯 Fonctionnalités Implémentées

### ✅ **Backend Microservices**
- [x] **User Service** - Gestion des utilisateurs avec Keycloak
- [x] **Event Service** - Gestion des événements avec liste d'attente
- [x] **Invitation Service** - Gestion des invitations et places
- [x] **Notification Service** - Notifications avec Resilience4j
- [x] **API Gateway** - Point d'entrée avec sécurité
- [x] **Config Server** - Configuration centralisée
- [x] **Eureka Discovery** - Service de découverte

### ✅ **Frontend Angular**
- [x] **Interface utilisateur moderne** - Design responsive
- [x] **Authentification** - Intégration Keycloak
- [x] **Gestion des événements** - CRUD complet
- [x] **Liste d'attente** - Interface intuitive
- [x] **Sélection de places** - Interface visuelle
- [x] **Notifications** - Feedback utilisateur

### ✅ **Sécurité & Authentification**
- [x] **Keycloak** - Serveur d'authentification
- [x] **JWT Tokens** - Authentification sécurisée
- [x] **OAuth2** - Protocole d'autorisation
- [x] **Rôles et permissions** - Gestion des accès

### ✅ **Résilience & Monitoring**
- [x] **Resilience4j** - Patterns de résilience
- [x] **Retry automatique** - Gestion des échecs
- [x] **Circuit Breaker** - Protection contre les pannes
- [x] **Dead Letter Queue** - Gestion des messages en échec
- [x] **Prometheus** - Collecte de métriques
- [x] **Grafana** - Visualisation des données

### ✅ **Messaging & Notifications**
- [x] **Apache Kafka** - Messaging asynchrone
- [x] **Notifications email** - Templates personnalisés
- [x] **Notifications temps réel** - Via Kafka
- [x] **Rappels automatiques** - Système de scheduling

---

## 🚀 Prochaines Améliorations

- [ ] **Tests E2E** - Tests bout en bout automatisés
- [ ] **WebSocket** - Notifications en temps réel
- [ ] **Mobile App** - Application mobile React Native
- [ ] **Analytics** - Tableaux de bord avancés
- [ ] **Multi-tenancy** - Support multi-organisations
- [ ] **API Versioning** - Gestion des versions d'API
- [ ] **Caching** - Redis pour les performances
- [ ] **Load Balancing** - Distribution de charge

---

**🚀 Bon développement !**
