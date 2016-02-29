FROM java:openjdk-8-jdk

ENV MAVEN_VERSION 3.3.9

RUN curl -kfsSL https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
  && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven

COPY . /isaac-api

WORKDIR /isaac-api

RUN mvn package -P deploy

CMD ["mvn", "jetty:run", "-P", "deploy"] 
