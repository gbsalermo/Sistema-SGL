# =============================================================================
# sgl-api — imagem de produção (modelo do kit de produção do TIP:
# tip/infra/producao/modelos/api-spring-postgres).
#
# Build em dois estágios:
#   1. "builder": imagem com Maven + JDK, que compila e gera o .jar;
#   2. imagem final: só o JRE (menor e sem ferramentas de build), que roda o .jar.
# A versão do Java precisa ser a mesma do <java.version> do pom.xml (17).
# =============================================================================
ARG JAVA_VERSION=17
FROM maven:3.9-eclipse-temurin-${JAVA_VERSION}-alpine AS builder
WORKDIR /app
# Copiar o pom.xml antes do código faz o Docker reaproveitar (cache) a camada das
# dependências enquanto o pom.xml não mudar: rebuilds ficam bem mais rápidos.
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
# Os testes rodam no desenvolvimento; no deploy só empacotamos.
RUN mvn clean package -DskipTests -q

ARG JAVA_VERSION=17
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
