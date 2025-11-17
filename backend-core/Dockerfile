FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the JAR file
COPY build/libs/foundation-service-*.jar app.jar

# Expose port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
