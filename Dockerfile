FROM openjdk:24-ea-17-bookworm
COPY ZigZag/target/*dependencies.jar app.jar
COPY ZigZag/src/main/resources/specification.json .
COPY ZigZag/target/lib/* /lib/
ENTRYPOINT ["java","-jar","/app.jar"]