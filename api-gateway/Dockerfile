FROM openjdk:17-jdk-slim

COPY target/ApiGateway-0.0.1-SNAPSHOT.jar api-gateway.jar

EXPOSE 8093

ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
