FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY account-service/pom.xml account-service/pom.xml
COPY card-service/pom.xml card-service/pom.xml

RUN mvn -q -pl card-service -am dependency:go-offline

COPY card-service/src card-service/src

RUN mvn -q -pl card-service -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd -r -u 1001 app
WORKDIR /app
COPY --from=build /workspace/card-service/target/card-service-0.0.1-SNAPSHOT.jar app.jar
USER 1001
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
