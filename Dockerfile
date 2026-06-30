FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw && ./mvnw package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY uploads /app/bundled-uploads
RUN chmod +x /docker-entrypoint.sh && mkdir -p /app/uploads/products
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
