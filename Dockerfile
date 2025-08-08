FROM eclipse-temurin:21-alpine

ARG KEYSTORE_PASSWORD
ARG JAR_FILE_NAME
LABEL version=${JAR_FILE_NAME}

WORKDIR /app

RUN --mount=type=secret,id=keystore_p12,dst=keystore.p12 sh -c 'base64 -d /run/secrets/keystore_p12 > keystore.p12'

RUN addgroup -S user &&  \
    adduser -S user -G user && \
    chown -R user:user /app
USER user:user

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/app.jar

EXPOSE 8443

ENV KEYSTORE_LOCATION=keystore.p12
ENV KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD}

ENTRYPOINT ["java","-jar","app.jar"]