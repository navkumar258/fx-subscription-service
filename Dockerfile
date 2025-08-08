FROM eclipse-temurin:21-alpine

ARG KEYSTORE_PASSWORD
ARG JAR_FILE_NAME
LABEL version=${JAR_FILE_NAME}

WORKDIR /app

RUN addgroup -S spring &&  \
    adduser -S spring -G spring && \
    chown -R spring:spring /app
USER spring:spring

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/app.jar
#COPY src/main/resources/keystore.p12 /app/keystore.p12
RUN --mount=type=secret,id=keystore_p12,dst=keystore.p12 sh -c 'base64 -d /run/secrets/keystore_p12 > keystore.p12'

EXPOSE 8443

ENV KEYSTORE_LOCATION=keystore.p12

ENTRYPOINT ["java","-jar","app.jar"]