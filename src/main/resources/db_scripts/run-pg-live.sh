docker rm -f postgres
docker run --name postgres -e POSTGRES_PASSWORD=rutherf0rd -e POSTGRES_USER=rutherford -e PGDATA=/pgdata --volumes-from pgstore --net db-live isaac-pg

