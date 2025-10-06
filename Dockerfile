FROM gradle:8.10-jdk25 AS build

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle build --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/build/distributions/*.tar .
RUN tar -xf *.tar --strip-components=1 && rm *.tar

EXPOSE 8080

CMD ["./bin/lucene-full-text"]