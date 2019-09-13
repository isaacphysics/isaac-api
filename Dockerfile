FROM jetty:9.4.18

USER root
ENV MAVEN_VERSION 3.3.9
ENV JAVA_OPTIONS -Xms2g -Xmx2g
RUN curl -kfsSL https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
  && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven

COPY . /isaac-api 

WORKDIR /isaac-api

VOLUME /root/.m2

# build isaac api without unit tests
RUN mvn package -Dmaven.test.skip=true

#RUN sed -i -e 's#dev/random#dev/./urandom#g' $JAVA_HOME/jre/lib/security/java.security

# deploy to jetty directory and fix permissions
RUN cp ./target/isaac-api.war /var/lib/jetty/webapps
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE

