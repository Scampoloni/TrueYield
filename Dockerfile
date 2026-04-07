# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn ./.mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY backend/src ./src
RUN ./mvnw package -DskipTests -q

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*-exec.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
