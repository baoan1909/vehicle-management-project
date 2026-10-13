FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -B -DskipTests package \
    && JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)" \
    && cp "$JAR_FILE" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S coparking \
    && adduser -S coparking -G coparking

EXPOSE 8080

COPY --from=build --chown=coparking:coparking /workspace/app.jar /app/app.jar

USER coparking

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/vehicle-management/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
