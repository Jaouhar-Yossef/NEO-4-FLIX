# Build stage: pinned Maven + JDK 21 (the repo has no Maven wrapper).
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app
# pom.xml first: the dependency layer is reused until the pom changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run with `mvn test` before a merge, not inside the image build.
RUN mvn -B -q package -DskipTests

# Run stage: JRE only, no Maven, no JDK, no sources.
FROM eclipse-temurin:21.0.13.1_2-jre-noble
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Not root: the base image ships an "ubuntu" user (uid 1000).
USER ubuntu
# Safe default; docker-compose must still set it, because .env has "dev".
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
