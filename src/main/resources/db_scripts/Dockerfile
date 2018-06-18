FROM postgres:9.6
COPY postgres-rutherford-create-script.sql /docker-entrypoint-initdb.d/00-isaac-create.sql
COPY postgres-rutherford-functions.sql /docker-entrypoint-initdb.d/01-isaac-functions.sql

ENV PGDATA /pgdata
ENV POSTGRES_USER rutherford
ENV POSTGRES_PASSWORD rutherf0rd
