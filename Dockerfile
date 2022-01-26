FROM alpine:3.15

RUN set -ex && \
    apk add gcompat && \
    apk add --no-cache --no-progress openjdk11-jre-headless wget && \
    apk del wget

WORKDIR /app

COPY / ./
RUN ./gradlew assemble

ENTRYPOINT ["java", "-Xms512m", "-Xmx512m", "-Djava.net.preferIPv4Stack=true"]

CMD ["-jar", "./target/tracker-server.jar", "docker-config.xml"]