FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 5000
# Running inside a container, "localhost" would resolve to the container itself rather than
# the host machine where the mocks are published (localhost:3001) — host.docker.internal is
# Docker's well-known alias for the host, so this works out of the box on Docker Desktop.
ENV CATALOG_BASE_URL=http://host.docker.internal:3001
ENTRYPOINT ["java", "-jar", "app.jar"]
