# A build context a repó gyökere: docker build -f mocsar-server/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY mocsar-core/pom.xml mocsar-core/
COPY mocsar-bot/pom.xml mocsar-bot/
COPY mocsar-server/pom.xml mocsar-server/
RUN mvn -q -B dependency:go-offline -pl mocsar-server -am || true
COPY mocsar-core mocsar-core
COPY mocsar-bot mocsar-bot
COPY mocsar-server mocsar-server
RUN mvn -q -B -pl mocsar-server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/mocsar-server/target/mocsar-server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
