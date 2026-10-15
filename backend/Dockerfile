# syntax=docker/dockerfile:1.7
# Compilación en la plataforma del builder (el JAR es independiente de la arquitectura);
# la imagen final no tiene instrucciones RUN, así que se construye para arm64/amd64 sin emulación.

FROM --platform=$BUILDPLATFORM eclipse-temurin:21.0.12.1_1-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/polizas.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:21.0.12.1_1-jre-alpine
WORKDIR /app
# Capas ordenadas de menos a más cambiantes para aprovechar la caché
COPY --from=build /src/target/extracted/dependencies/ ./
COPY --from=build /src/target/extracted/spring-boot-loader/ ./
COPY --from=build /src/target/extracted/snapshot-dependencies/ ./
COPY --from=build /src/target/extracted/application/ ./
# Usuario sin privilegios (UID numérico: no requiere crear el usuario en la imagen)
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
