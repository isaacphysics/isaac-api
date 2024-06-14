#!/usr/bin/env bash

rm isaac-test-es-data.tar.gz
docker cp -a it-elasticsearch:/usr/share/elasticsearch/data data
# find data -name write.lock -exec rm {} \;
# rm data/nodes/0/node.lock
tar zcf isaac-test-es-data.tar.gz data
rm -rf data
