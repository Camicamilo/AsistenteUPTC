# ---- Etapa 1: compilar con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /fuente
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Etapa 2: imagen liviana solo con el JRE ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S asistente && adduser -S asistente -G asistente && apk add --no-cache wget
COPY --from=compilacion /fuente/target/asistente-backend.jar app.jar
USER asistente
EXPOSE 8080
# MaxRAMPercentage: aprovecha la memoria del contenedor (planes gratuitos de 512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Duser.timezone=UTC"
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
