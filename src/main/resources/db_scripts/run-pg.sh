docker run --name pg1 -e POSTGRES_PASSWORD=secret -e POSTGRES_USER=rutherford -e PGDATA=/pgdata --volumes-from pgstore pg

