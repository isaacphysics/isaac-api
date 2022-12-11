FROM maven:3.8.6-eclipse-temurin-11 as base
#Set to "-P etl" for etl build
ARG MVN_PACKAGE_PARAM=""
WORKDIR /isaac-api
COPY pom.xml .
#Unsure what there's go-offline when it's never used
#RUN mvn dependency:go-offline
COPY . /isaac-api
RUN mvn package -Dmaven.test.skip=true $MVN_PACKAGE_PARAM

FROM jetty:11.0.12-jdk11-eclipse-temurin
USER root
COPY --from=base /isaac-api/target/isaac-api.war /var/lib/jetty/webapps/isaac-api.war
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE