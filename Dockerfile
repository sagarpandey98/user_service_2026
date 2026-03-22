# Stage 1: Build the JAR using Maven and JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Build the application and skip tests for faster deployment
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR using JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
