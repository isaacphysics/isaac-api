#!/usr/bin/env bash

rm isaac-test-es-data.zip
# find data -name write.lock -exec rm {} \;
# rm data/nodes/0/node.lock
docker exec -u root it-elasticsearch bash -c "zip -r /isaac-test-es-data.zip /usr/share/elasticsearch/data/"
docker cp -a it-elasticsearch:/isaac-test-es-data.zip isaac-test-es-data.zip
docker exec -u root it-elasticsearch bash -c "rm /isaac-test-es-data.zip"
