# ── Stage 1: Build ──
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ──
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Render free tier has 512MB RAM — optimize JVM
ENV JAVA_OPTS="-Xmx384m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
