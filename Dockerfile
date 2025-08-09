FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE_NAME
LABEL version=${JAR_FILE_NAME}

WORKDIR /app

RUN --mount=type=secret,id=keystore_p12 \
    --mount=type=tmpfs,target=/tmp \
    sh -c 'base64 -d /run/secrets/keystore_p12 > /tmp/keystore.p12 && mv /tmp/keystore.p12 /app/keystore.p12'

RUN addgroup -S user && \
    adduser -S user -G user && \
    chown -R user:user /app
USER user:user

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/app.jar

EXPOSE 8443

ENTRYPOINT ["java","-jar","app.jar"]