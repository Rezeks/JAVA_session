# ============ Stage 1: Build ============
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cache layer)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ============ Stage 2: Runtime ============
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S securemsg && adduser -S securemsg -G securemsg

# Copy jar
COPY --from=build /app/target/*.jar app.jar

# Copy static resources
COPY --from=build /app/target/classes/static/ /app/static/

# Create storage directory for file transfers
RUN mkdir -p /app/storage && chown securemsg:securemsg /app/storage

USER securemsg

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
