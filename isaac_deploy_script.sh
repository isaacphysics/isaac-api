#!/bin/bash

# Deploy script for isaac-api and isaac-app
# Environment specific variables
SCHOOL_CSV_LIST_LOCATION="/local/data/rutherford/"
REMOTE_GIT_SSH_KEY_LOCATION="/local/data/rutherford/keys/"
GOOGLE_CLIENT_SECRET_LOCATION="/local/data/rutherford/keys/"

ISAAC_API_DEPLOY_FOLDER="isaac-api"
ISAAC_API_DEPLOY_FILE="isaac-api.war"
ISAAC_API_DEPLOY_LOCATION="/var/lib/tomcat7/webapps"

ISAAC_APP_DEPLOY_FOLDER="/var/isaac-app"
ISAAC_APP_DEPLOY_FILE="isaac-app.tar.gz"

# Shutdown apache
echo "Shutting down apache so that maintenance page shows..."
sudo service apache2 stop

# Shut down api server to stop database access
echo "Shutting down the API to stop database access."
sudo service tomcat7 stop

# Delete and rename existing api files
echo "Backing up last API war file... to $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE.bak"
sudo mv $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE.bak

echo "Deleting old Isaac API deployment."
sudo rm -rf $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FOLDER

echo "Copying ./$ISAAC_API_DEPLOY_FILE to $ISAAC_API_DEPLOY_LOCATION"
sudo cp ./$ISAAC_API_DEPLOY_FILE $ISAAC_API_DEPLOY_LOCATION

echo "Resetting permissions for isaac-api so that tomcat can use it."
sudo chown tomcat7 $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE
sudo chgrp tomcat7 $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE

echo "Starting up tomcat"
sudo service tomcat7 start

# Unpack isaac-app into apache root.
echo "Unpacking Isaac App distribution."
sudo rm -rf $ISAAC_APP_DEPLOY_FOLDER/*
sudo tar -zxf $ISAAC_APP_DEPLOY_FILE $ISAAC_APP_DEPLOY_FOLDER

echo "Resetting permissions for isaac-app so that apache can use it."
sudo chown -R tomcat7 $ISAAC_APP_DEPLOY_FOLDER
sudo chgrp -R tomcat7 $ISAAC_APP_DEPLOY_FOLDER

echo "Polling api to trigger reindex operation."
wget --spider http://localhost:8080/isaac-api/api

echo "Starting apache."
sudo service apache2 start

echo "Upgrade script complete."