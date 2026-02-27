ARG BUILD_TARGET="production"

FROM maven:3.9.9-eclipse-temurin-11 AS builder

ARG BUILD_TARGET
ARG BUILD_VERSION

WORKDIR /isaac-api
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . /isaac-api
RUN mvn package -Dmaven.test.skip=true -Dsegue.version=$BUILD_VERSION -P $BUILD_TARGET

FROM jetty:11.0.26-jdk11-eclipse-temurin
USER root
COPY --from=builder /isaac-api/target/isaac-api.war /var/lib/jetty/webapps/isaac-api.war
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

# initialise logging for jetty (including approving third-party licensed code) and add jetty log4j config:
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=logging-log4j2 --approve-all-licenses
COPY src/main/resources/log4j2-jetty-only.xml $JETTY_BASE/resources/log4j2.xml

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE