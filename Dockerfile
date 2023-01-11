FROM maven:3.8.6-eclipse-temurin-11 as base
#Set to "-P etl" for etl build
ARG MVN_PACKAGE_PARAM=""
ARG BUILD_VERSION=""
WORKDIR /isaac-api
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . /isaac-api
RUN mvn package -Dmaven.test.skip=true -Dsegue.version=$BUILD_VERSION $MVN_PACKAGE_PARAM

FROM jetty:11.0.12-jdk11-eclipse-temurin
USER root
RUN mkdir /isaac-logs
RUN chmod 755 /isaac-logs
RUN chown jetty /isaac-logs
ADD resources/school_list_2022.tar.gz /local/data/
COPY config-templates/content_indices.cs.properties /local/data/content-indices.properties
RUN chmod 755 /local/data/content-indices.properties
RUN chown jetty /local/data/content-indices.properties
COPY --from=base /isaac-api/target/isaac-api.war /var/lib/jetty/webapps/isaac-api.war
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE
