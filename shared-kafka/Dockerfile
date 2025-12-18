FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /root/.m2/repository/br/com/banco/shared-kafka/1.0.0-SNAPSHOT/shared-kafka-1.0.0-SNAPSHOT.jar app.jar

CMD ["echo", "shared-kafka is a library, not a runnable service"]

