# Dockerfile para Abertura de Conta Online
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar arquivo de configuração do Maven
COPY pom.xml .

# Baixar dependências (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Compilar e empacotar a aplicação
RUN mvn clean package -DskipTests

# Imagem final
FROM eclipse-temurin:21-jre-jammy

# Instalar wget para healthcheck
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar o JAR gerado
COPY --from=build /app/target/*.jar app.jar

# Expor a porta da aplicação e debug
EXPOSE 8080 7001

# Executar a aplicação com modo debug na porta 7001
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:7001", "-jar", "app.jar"]

