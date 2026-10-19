# ---- build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# Dependências numa camada própria: só baixam de novo quando o pom.xml muda.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime --------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Usuário sem privilégios: uma falha na aplicação não vira root no container.
RUN groupadd --system taskbridge && useradd --system --gid taskbridge --no-create-home taskbridge \
    && mkdir -p /app/data/uploads && chown -R taskbridge:taskbridge /app/data

COPY --from=build /build/target/taskbridge-*.jar /app/taskbridge.jar

USER taskbridge
ENV SPRING_PROFILES_ACTIVE=prod \
    STORAGE_DIR=/app/data/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
VOLUME /app/data/uploads
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/taskbridge.jar"]
