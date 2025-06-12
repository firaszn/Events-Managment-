# 🎯 Application d'Organisation d'Événements - Architecture Microservices

Une application complète d'organisation d'événements développée avec **Spring Boot**, **Angular**, **PostgreSQL** et **Kafka** en architecture microservices.

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
- [📩 Intégration Kafka](#-intégration-kafka)
- [🧪 Tests](#-tests)
- [📚 Documentation](#-documentation)

## 🎯 Objectif

Permettre aux utilisateurs de :
- ✅ **Créer et gérer des événements** (réunions, conférences, etc.)
- ✅ **Inviter des participants** aux événements
- ✅ **Gérer les inscriptions** et réponses aux invitations
- ✅ **Être notifiés des changements** via Kafka en temps réel

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Gateway       │    │  Config Server  │    │ Eureka Discovery│
│   Port: 8080    │    │   Port: 8888    │    │   Port: 8761    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
    │  User Service   │    │  Event Service  │    │Invitation Service│
    │   Port: 8084    │    │   Port: 8082    │    │   Port: 8083    │
    │  + PostgreSQL   │    │  + PostgreSQL   │    │  + PostgreSQL   │
    └─────────────────┘    └─────────────────┘    └─────────────────┘
                                 │                       │
                                 └───────────────────────┘
                                          │
                                    ┌─────────────────┐
                                    │     Kafka       │
                                    │  (Notifications)│
                                    └─────────────────┘
```

## 🧱 Microservices

### 1. 👥 **User Service** (Port 8084)
**Rôle :** Gérer les utilisateurs (organisateurs + participants)

**Endpoints :**
- `POST /api/users` – Créer utilisateur
- `GET /api/users/{id}` – Détails utilisateur
- `GET /api/users` – Liste des utilisateurs

### 2. 🎪 **Event Service** (Port 8082)
**Rôle :** Créer, modifier, supprimer des événements

**Champs événement :** `id`, `title`, `description`, `location`, `dateTime`, `organizerId`

**Endpoints :**
- `POST /api/events` – Créer événement
- `GET /api/events/{id}` – Détails
- `PUT /api/events/{id}` – Modifier
- `DELETE /api/events/{id}` – Supprimer
- `GET /api/events?organizerId=x` – Événements par utilisateur

### 3. 🎫 **Invitation Service** (Port 8083)
**Rôle :** Gérer les invitations & inscriptions aux événements

**Champs :** `id`, `eventId`, `userId`, `status` (PENDING, ACCEPTED, DECLINED)

**Endpoints :**
- `POST /api/invitations` – Inviter un participant
- `PUT /api/invitations/{id}/respond` – Répondre (accepter/refuser)
- `GET /api/invitations/user/{userId}` – Voir les invitations d'un utilisateur

### 4. 🌐 **API Gateway** (Port 8080)
**Rôle :** Point d'entrée unique pour tous les services

### 5. 🔍 **Eureka Discovery** (Port 8761)
**Rôle :** Service de découverte et enregistrement des microservices

### 6. ⚙️ **Config Server** (Port 8888)
**Rôle :** Gestion centralisée des configurations

## 🚀 Technologies

| Technologie | Version | Usage |
|-------------|---------|-------|
| **Java** | 17 | Langage principal |
| **Spring Boot** | 3.4.2 | Framework microservices |
| **Spring Cloud** | 2024.0.0 | Outils microservices |
| **PostgreSQL** | 12+ | Base de données |
| **Apache Kafka** | 2.8+ | Messaging asynchrone |
| **Maven** | 3.8+ | Gestionnaire de dépendances |
| **Docker** | 20+ | Conteneurisation (optionnel) |

## 📦 Prérequis

- ☕ **Java 17+**
- 🐘 **PostgreSQL 12+**
- 📨 **Apache Kafka 2.8+**
- 🔧 **Maven 3.8+**
- 🐳 **Docker** (optionnel)

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
```

### 3. **Configurer PostgreSQL**
Vérifiez que PostgreSQL fonctionne sur le port `5432` avec :
- **Utilisateur :** `postgres`
- **Mot de passe :** `postgres`

### 4. **Installer Kafka** (si pas déjà fait)
```bash
# Télécharger et démarrer Kafka sur localhost:9092
# Ou utiliser Docker :
docker run -p 9092:9092 apache/kafka:2.8.0
```

## 🔧 Configuration

Les configurations sont centralisées dans le **Config Server** :
- `config-server/src/main/resources/configurations/`
  - `user.properties`
  - `event.properties`
  - `invitation.properties`
  - `gateway.properties`
  - `discovery.properties`

## 🚀 Démarrage

### **Ordre de Démarrage Recommandé :**

1. **Eureka Discovery Server**
```bash
mvn spring-boot:run -pl discovery-server
```
📍 Accès : http://localhost:8761

2. **Config Server**
```bash
mvn spring-boot:run -pl config-server
```
📍 Accès : http://localhost:8888

3. **User Service**
```bash
mvn spring-boot:run -pl user
```
📍 Accès : http://localhost:8084

4. **Event Service**
```bash
mvn spring-boot:run -pl event-service
```
📍 Accès : http://localhost:8082

5. **Invitation Service**
```bash
mvn spring-boot:run -pl invitation-service
```
📍 Accès : http://localhost:8083

6. **API Gateway**
```bash
mvn spring-boot:run -pl gateway
```
📍 Accès : http://localhost:8080

## 📡 APIs

### 🎪 **Event Service Examples**

**Créer un événement :**
```bash
POST http://localhost:8082/api/events
Content-Type: application/json

{
  "title": "Réunion équipe développement",
  "description": "Réunion hebdomadaire de l'équipe",
  "location": "Salle de conférence A",
  "dateTime": "2025-06-15T14:00:00",
  "organizerId": 1
}
```

**Lister les événements d'un organisateur :**
```bash
GET http://localhost:8082/api/events?organizerId=1
```

### 🎫 **Invitation Service Examples**

**Créer une invitation :**
```bash
POST http://localhost:8083/api/invitations/simple
Content-Type: application/json

{
  "eventId": 1,
  "userId": 2
}
```

**Accepter une invitation :**
```bash
PUT http://localhost:8083/api/invitations/1/accept
```

**Voir les invitations d'un utilisateur :**
```bash
GET http://localhost:8083/api/invitations/user/2
```

## 📩 Intégration Kafka

### **Topics Kafka :**

| Événement | Producteur | Topic | Consommateur |
|-----------|------------|-------|--------------|
| Création événement | event-service | `event.created` | notification-service |
| Modification événement | event-service | `event.updated` | notification-service |
| Réponse invitation | invitation-service | `invitation.responded` | notification-service |
| Rappel événement | scheduler-service | `event.reminder` | notification-service |

### **Configuration Kafka :**
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

## 🧪 Tests

### **Tests Unitaires**
```bash
# Tester tous les services
mvn test

# Tester un service spécifique
mvn test -pl event-service
mvn test -pl invitation-service
mvn test -pl user
```

### **Tests d'Intégration**
```bash
# Vérifier que tous les services sont enregistrés dans Eureka
curl http://localhost:8761/eureka/apps

# Tester via le Gateway
curl http://localhost:8080/api/events
curl http://localhost:8080/api/invitations
curl http://localhost:8080/api/users
```

### **Tests des APIs avec curl**

**Event Service :**
```bash
# Créer un événement
curl -X POST http://localhost:8082/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Event",
    "description": "Event de test",
    "location": "Salle Test",
    "dateTime": "2025-06-20T10:00:00",
    "organizerId": 1
  }'

# Lister tous les événements
curl http://localhost:8082/api/events
```

**Invitation Service :**
```bash
# Créer une invitation
curl -X POST http://localhost:8083/api/invitations/simple \
  -H "Content-Type: application/json" \
  -d '{"eventId": 1, "userId": 2}'

# Accepter une invitation
curl -X PUT http://localhost:8083/api/invitations/1/accept

# Voir les invitations d'un utilisateur
curl http://localhost:8083/api/invitations/user/2
```

## 📚 Documentation

### **Swagger UI** (si configuré)
- Event Service : http://localhost:8082/swagger-ui.html
- Invitation Service : http://localhost:8083/swagger-ui.html
- User Service : http://localhost:8084/swagger-ui.html

### **Eureka Dashboard**
- URL : http://localhost:8761
- Visualisation de tous les services enregistrés

### **Config Server**
- URL : http://localhost:8888
- Endpoints de configuration :
  - http://localhost:8888/user/default
  - http://localhost:8888/event/default
  - http://localhost:8888/invitation/default

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

**4. Port déjà utilisé :**
```bash
# Trouver le processus utilisant un port
netstat -ano | findstr :8082
# Tuer le processus si nécessaire
```

## 🚀 Déploiement

### **Docker Compose** (optionnel)
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:13
    environment:
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  kafka:
    image: apache/kafka:2.8.0
    ports:
      - "9092:9092"
```

### **Production**
- Utiliser des profils Spring (`application-prod.properties`)
- Configurer des bases de données séparées
- Mettre en place un monitoring (Actuator + Micrometer)
- Utiliser un service mesh (Istio) pour la production

## 👥 Équipe de Développement

- **Développeur Principal :** [Votre Nom]
- **Stage :** GTI 2025
- **Technologies :** Spring Boot, Microservices, PostgreSQL, Kafka

## 📄 Licence

Ce projet est développé dans le cadre d'un stage GTI 2025.

---

## 🎯 Prochaines Fonctionnalités

- [ ] **Notification Service** - Service de notifications en temps réel
- [ ] **Scheduler Service** - Rappels automatiques d'événements
- [ ] **Frontend Angular** - Interface utilisateur complète
- [ ] **Authentification JWT** - Sécurisation des APIs
- [ ] **Monitoring** - Métriques et logs centralisés
- [ ] **Tests E2E** - Tests bout en bout automatisés

---

**🚀 Bon développement !**
