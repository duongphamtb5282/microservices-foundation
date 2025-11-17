FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the JAR file
COPY build/libs/api-gateway-*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
