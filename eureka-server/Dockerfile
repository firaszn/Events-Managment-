# Utiliser une image de base avec JDK 17 (ou une autre version compatible)
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/*.jar app.jar

# Exposer le port de Eureka
EXPOSE 8761

# Lancer l'application
ENTRYPOINT ["java", "-jar", "/app.jar"]
