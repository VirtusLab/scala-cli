FROM eclipse-temurin:17 as build
RUN apt update && apt install build-essential libz-dev clang procps -y
WORKDIR /workdir
COPY . .
RUN ./mill -i copyTo --task 'cli[]'.nativeImageStatic --dest "./docker-out/scala-cli" 1>&2

FROM debian:stable-slim
COPY --from=build /workdir/docker-out/scala-cli /usr/bin/scala-cli
RUN \
 echo "println(1)" | scala-cli -S 3 - -v -v -v && \
 echo "println(1)" | scala-cli -S 2.13 - -v -v -v && \
 echo "println(1)" | scala-cli -S 2.12 - -v -v -v
RUN \
 echo "println(1)" | scala-cli --power package --native _.sc --force && \
 echo "println(1)" | scala-cli --power package --native-image _.sc --force

ENTRYPOINT ["scala-cli"]
