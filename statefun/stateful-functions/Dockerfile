FROM gradle:jdk11-slim AS builder
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src/stateful-functions
RUN gradle :stateful-functions:assemble --no-daemon

FROM openjdk:11
WORKDIR /
COPY --from=builder /home/gradle/src/stateful-functions/build/libs/stateful-functions*.jar stateful-functions.jar
EXPOSE 1108
CMD java -jar stateful-functions.jar