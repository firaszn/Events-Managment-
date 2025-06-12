# Utiliser une image de base avec JDK 17 (ou une autre version compatible)
FROM openjdk:17-jdk-slim

# Copier le fichier JAR dans le conteneur
COPY target/serverdiscover-0.0.1-SNAPSHOT.jar eureka-server.jar

# Exposer le port de Eureka
EXPOSE 8761

# Lancer l'application
ENTRYPOINT ["java", "-jar", "eureka-server.jar"]
