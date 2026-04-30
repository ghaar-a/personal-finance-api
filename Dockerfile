# Estágio 1: Build da aplicação
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
# Copia os arquivos do Gradle primeiro para aproveitar o cache
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
# Dá permissão de execução ao gradlew
RUN chmod +x gradlew
# Copia o código fonte
COPY src src
# Compila o projeto gerando o JAR
RUN ./gradlew clean bootJar -x test

# Estágio 2: Imagem final leve
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]