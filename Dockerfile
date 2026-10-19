# Stage 1: Build Spring Boot backend from monorepo root
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY backend/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

# Copy source and compile
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lean Java 21 Alpine Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create upload directory for photos and stories
RUN mkdir -p /app/uploads

# Copy executable jar from builder stage
COPY --from=builder /app/target/dating-social-backend-*.jar app.jar

ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
