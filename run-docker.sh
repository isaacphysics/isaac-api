set -e
docker rm -f api-live-$1
docker create --name api-live-$1 --net isaac -v /local/data/conf:/local/data/rutherford/conf -v /local/data/rutherford-content:/local/data/rutherford/git-contentstore/rutherford-content -v /local/data/keys:/local/data/rutherford/keys isaac-api-$1
docker network connect content-live api-live-$1
docker network connect db-live api-live-$1
docker start api-live-$1
docker attach api-live-$1
