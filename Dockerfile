# ==========================================
# Stage 1: Build & Package the Application
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy the root POM and module POMs to cache dependencies
COPY pom.xml .
COPY inkpulse-repo/pom.xml inkpulse-repo/
COPY inkpulse-service/pom.xml inkpulse-service/
COPY inkpulse-api/pom.xml inkpulse-api/

# Download dependencies offline to optimize Docker layer caching
RUN mvn dependency:go-offline -B

# Copy all source files
COPY inkpulse-repo/src inkpulse-repo/src
COPY inkpulse-service/src inkpulse-service/src
COPY inkpulse-api/src inkpulse-api/src

# Package the application (build fat jar, skip tests for speed in packaging)
RUN mvn package -DskipTests -B

# ==========================================
# Stage 2: Runtime Environment (Production)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create a non-root system group and user for security compliance
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the packaged fat jar from the builder stage
COPY --from=builder /app/inkpulse-api/target/inkpulse-api-1.0-SNAPSHOT.jar app.jar

# Adjust ownership to the non-root user
RUN chown -R spring:spring /app

# Switch to the non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Environment and JVM variables suited for containerized deployment (e.g. K8s)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Run the jar file using execution form
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
