# Just like run-pg, only don't pull in data volume, so data will remain local. Removing this container will nuke the data too.
docker rm -f pg-test
docker run --net isaac --name pg-test -e POSTGRES_PASSWORD=secret -e POSTGRES_USER=rutherford isaac-pg
