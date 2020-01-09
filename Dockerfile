## Step : Download dependencies so that they are cached in the docker layer if the pom file doesn't change
#FROM maven:3.5.3-jdk-8-alpine as target
#WORKDIR /isaac-api
#COPY pom.xml .
#RUN mvn dependency:go-offline

FROM isaac-api-base

COPY . /isaac-api

# build isaac api war file without unit tests
RUN mvn package -Dmaven.test.skip=true

# create clean jetty docker container
FROM jetty:9.3.27 as server
USER root
COPY --from=target /isaac-api/target/isaac-api.war /var/lib/jetty/webapps/isaac-api.war
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

#RUN sed -i -e 's#dev/random#dev/./urandom#g' $JAVA_HOME/jre/lib/security/java.security

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE