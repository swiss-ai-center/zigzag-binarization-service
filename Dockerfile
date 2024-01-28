FROM eclipse-temurin:17-jdk-alpine
COPY ZigZag/target/*dependencies.jar app.jar
COPY ZigZag/src/main/resources/specification.json .
COPY ZigZag/target/lib/* /lib/
ENTRYPOINT ["java","-jar","/app.jar"]