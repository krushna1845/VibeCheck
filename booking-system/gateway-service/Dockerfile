FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY target/gateway-service-*.jar app.jar
EXPOSE 8079
ENTRYPOINT ["java", "-jar", "app.jar"]
