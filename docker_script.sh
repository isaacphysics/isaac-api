#!/bin/bash

#This doesn't do any rebuilding, just stops and starts containers


ES_NETWORKS=("content-tracking" "content-live")
PG_NETWORKS=("postgres-live postgres-dev postgres-test")

echo "Removing all networks"
docker network rm isaac
for ((i=0; i < ${#ES_NETWORKS[@]}; i++)) 
do
    docker network rm ${ES_NETWORKS[$i]}
done

for ((i=0; i < ${#PG_NETWORKS[@]}; i++))
do
    echo ${PG_NETWORKS[$i]}
    docker network rm ${PG_NETWORKS[$i]}
done


# Stop all running  docker containers
docker stop `docker ps -qa` >> /dev/null

#Bring up API dependencies
docker rm elasticsearch
docker run --name elasticsearch -d elasticsearch:1.4 || exit 1

#Bring up the API
cd isaac-api
docker rm isaac-api-1.5.6
docker run -p 90:8080 \
       --name isaac-api-1.5.6 \
       -v /local/data/keys:/local/data/rutherford/keys \
       -v /local/data/rutherford-content:/local/data/rutherford/git-contentstore/rutherford-content \
       -v /local/data/conf:/local/data/rutherford/conf \
       isaac-api-1.5.6 || exit 1

# Bring up the APP
#cd ../isaac-app
#docker run --name app-2.3.4 --net-alias app-live --net isaac -d app-2.3.4


#Bring up Balancer
#cd ../balancer
#docker build -t balancer . && docker run -p 85:80 --net isaac -d -name balancer balancer
