# ---- Etapa 1: build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copia só o pom primeiro: as dependências ficam em cache enquanto o pom não mudar.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Etapa 2: runtime (imagem enxuta, usuário sem privilégios) ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --create-home vagaradar
COPY --from=build /app/target/vagaradar.jar app.jar
USER vagaradar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
