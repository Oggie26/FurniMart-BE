# Use pre-built JAR from host (faster deployment)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Copy JAR from host (build locally and copy to server before docker build)
COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
