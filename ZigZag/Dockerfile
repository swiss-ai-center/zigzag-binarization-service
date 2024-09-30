FROM eclipse-temurin:17-jdk-alpine
COPY ./target/*.jar app.jar
COPY ./src/main/resources/specification.json .
ENTRYPOINT ["java","-jar","/app.jar"]