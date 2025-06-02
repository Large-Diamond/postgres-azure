FROM maven:3-eclipse-temurin-21 AS build-server
WORKDIR /workspace/server

COPY server/pom.xml ./
COPY server .

RUN mvn package -DskipTests -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM node:24 AS build-client

WORKDIR /workspace/client

COPY client/package*.json ./
RUN npm install

COPY client .
RUN npm run build

FROM bellsoft/liberica-openjre-alpine-musl:24

VOLUME /tmp

ARG DEPENDENCY=/workspace/server/target/dependency
ARG SPRING_PROFILES_ACTIVE=prod

RUN apk add postgresql16-client curl

COPY --from=build-server ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build-server ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build-server ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=build-client /workspace/client/dist/index.html /app/templates/index.html
COPY --from=build-client /workspace/client/dist /app/static
RUN rm /app/static/index.html /app/static/mockServiceWorker.js

ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

ENTRYPOINT ["java", "-cp", "app:app/lib/*", "io.github.mucsi96.postgresbackuptool.App"]
