FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY shared-kafka/pom.xml /app/shared-kafka/pom.xml
COPY shared-kafka/src /app/shared-kafka/src
WORKDIR /app/shared-kafka
RUN mvn clean install -DskipTests

WORKDIR /app
COPY aco-service/pom.xml .
RUN mvn dependency:go-offline -B

COPY aco-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

