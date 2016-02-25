docker rm -f pg-live
docker run --name pg-live -e POSTGRES_PASSWORD=secret -e POSTGRES_USER=rutherford -e PGDATA=/pgdata --volumes-from pgstore pg

