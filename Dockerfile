ARG BUILD_TARGET="production"

FROM maven:3.9.4-eclipse-temurin-11 AS builder

ARG BUILD_TARGET
ARG BUILD_VERSION

WORKDIR /isaac-api
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . /isaac-api
RUN mvn package -Dmaven.test.skip=true -Dsegue.version=$BUILD_VERSION -P $BUILD_TARGET

FROM jetty:11.0.22-jdk11-eclipse-temurin
USER root
COPY --from=builder /isaac-api/target/isaac-api.war /var/lib/jetty/webapps/isaac-api.war
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE