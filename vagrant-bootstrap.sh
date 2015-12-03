#!/usr/bin/env bash

### Install Docker

apt-get update
apt-get -y install linux-image-extra-$(uname -r)
sh -c "wget -qO- https://get.docker.io/gpg | apt-key add -"
sh -c "echo deb http://get.docker.io/ubuntu docker main\ > /etc/apt/sources.list.d/docker.list"
apt-get update
apt-get -y install lxc-docker

usermod -a -G docker vagrant

### Install ElasticSearch

wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | apt-key add -
echo "deb http://packages.elastic.co/elasticsearch/1.4/debian stable main" | tee -a /etc/apt/sources.list.d/elasticsearch-1.4.list
apt-get update
apt-get -y install openjdk-7-jre-headless elasticsearch

### Install MongoDB

apt-get -y install mongodb

# Once MongoDB is installed, need to edit /etc/mongodb.conf to set bind = 0.0.0.0

### Install PostgreSQL

## TODO: Make sure we have PostgreSQL 9.4
apt-get -y install postgresql postgresql-contrib

## After installing postgres, need to create user. This will prompt for password
#
#     sudo -u postgres createuser -D -A -P rutherford
#     sudo -u postgres createdb -O rutherford rutherford
#     sudo -u postgres psql -f /isaac-api/src/main/resources/db_scripts/postgres-rutherford-create-script.sql

