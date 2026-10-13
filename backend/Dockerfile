# syntax=docker/dockerfile:1

# ---------- Стадия сборки ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Сначала копируем только pom-файлы, чтобы слой с зависимостями кэшировался
COPY pom.xml .
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/
COPY presentation/pom.xml presentation/
COPY bootstrap/pom.xml bootstrap/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DexcludeReactor=true || true

COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ---------- Стадия выполнения ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/bootstrap/target/autoparts-backend.jar app.jar

USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
