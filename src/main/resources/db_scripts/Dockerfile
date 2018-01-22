FROM postgres:9.6
COPY postgres-rutherford-create-script.sql /docker-entrypoint-initdb.d/isaac-create.sql

ENV PGDATA /pgdata
ENV POSTGRES_USER rutherford
ENV POSTGRES_PASSWORD rutherf0rd
