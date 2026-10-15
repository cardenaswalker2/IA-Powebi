# Etapa 1: Build con Maven y JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar archivos de dependencias para aprovechar caché de capas Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera de ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios para seguridad en producción
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar el artefacto generado
COPY --from=build /app/target/*.jar app.jar

# Render asigna el puerto mediante la variable de entorno $PORT
ENV PORT=8080
EXPOSE 8080

# Parámetros optimizados para contenedores en Render
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xmx400m", "-Xms200m", "-jar", "app.jar"]
