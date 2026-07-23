# ==========================================
# Runtime Environment (Production)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create a non-root system group and user for security compliance
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the pre-packaged fat jar from the host directory (built on Windows)
COPY inkpulse-api/target/inkpulse-api-1.0-SNAPSHOT.jar app.jar

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
