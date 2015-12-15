#!/usr/bin/env bash

## Add things we need
sh -c "wget -qO- https://get.docker.io/gpg | apt-key add -"
sh -c "echo deb http://get.docker.io/ubuntu docker main\ > /etc/apt/sources.list.d/docker.list"
wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | apt-key add -
echo "deb http://packages.elastic.co/elasticsearch/1.4/debian stable main" | tee -a /etc/apt/sources.list.d/elasticsearch-1.4.list
sh -c "wget -qO- https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -"
sh -c "echo deb http://apt.postgresql.org/pub/repos/apt/ trusty-pgdg main\ > /etc/apt/sources.list.d/pgdg.list"
apt-get update


### Install Docker

apt-get -y install linux-image-extra-$(uname -r) lxc-docker

usermod -a -G docker vagrant

### Install ElasticSearch
apt-get -y install openjdk-7-jre-headless elasticsearch

# Once ElasticSearch is installed, run this to make it start at startup:
# sudo update-rc.d elasticsearch defaults 95 10

### Install MongoDB

apt-get -y install mongodb

# Once MongoDB is installed, need to edit /etc/mongodb.conf to set bind_ip = 0.0.0.0
# Then sudo service mongodb restart

### Install PostgreSQL

apt-get -y install postgresql-9.4

## After installing postgres, need to create user. This will prompt for password - use the POSTGRES_DB_PASSWORD from your segue-config.properties
#
#     sudo -u postgres createuser -D -A -P rutherford
#     sudo -u postgres createdb -O rutherford rutherford
#     sudo -u postgres psql -f /isaac-api/src/main/resources/db_scripts/postgres-rutherford-create-script.sql rutherford
#
# Then add
#     listen_address = '*'
# to /etc/postgresql/9.4/main/postgresql.conf
#
# Then find 'host all all 127.0.0.1/32 md5' in /etc/postgresql/9.4/main/pg_hba.conf and replace with:
#
#     host all all 0.0.0.0/0 md5
#
# Then sudo service postgresql restart

