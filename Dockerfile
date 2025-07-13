FROM eclipse-temurin:21-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/app.jar
COPY src/main/resources/keystore.p12 /app/keystore.p12

EXPOSE 8443

ENTRYPOINT ["java","-jar","app.jar"]