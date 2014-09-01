#!/bin/bash

# Deploy script for isaac-api and isaac-app
# Environment specific variables
SCHOOL_CSV_LIST_LOCATION="/local/data/rutherford/"
REMOTE_GIT_SSH_KEY_LOCATION="/local/data/rutherford/keys/"
GOOGLE_CLIENT_SECRET_LOCATION="/local/data/rutherford/keys/"

ISAAC_APP_LOCAL_REPO_LOCATION="/var/isaac-app/"

ISAAC_API_DEPLOY_FOLDER="isaac-api"
ISAAC_API_DEPLOY_FILE="isaac-api.war"
ISAAC_API_DEPLOY_LOCATION="/var/lib/tomcat7/webapps"

# Step 0 request user input
echo "Type the tag name to deploy for the isaac-app"
read ISAAC_APP_VERSION_TO_DEPLOY

# Step 0.1 shutdown apache
echo "Shutting down apache so that maintenance page shows..."
sudo service apache2 stop

# Step 1 shut down api server to stop database access
echo "Shutting down the api to stop database access."
sudo service tomcat7 stop

# delete and rename existing api files
echo "Backing up last api war file... to $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE.bak"
sudo mv $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE.bak

echo "Deleting old isaac api deployment."
sudo rm -rf $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FOLDER

echo "Copying ./$ISAAC_API_DEPLOY_FILE to $ISAAC_API_DEPLOY_LOCATION"
sudo cp ./$ISAAC_API_DEPLOY_FILE $ISAAC_API_DEPLOY_LOCATION

echo "Resetting permissions for isaac-api so that tomcat can use it."
sudo chown tomcat7 $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE
sudo chgrp tomcat7 $ISAAC_API_DEPLOY_LOCATION/$ISAAC_API_DEPLOY_FILE

echo "Starting up tomcat"
sudo service tomcat7 start

echo "Getting latest from isaac-app repo"
cd $ISAAC_APP_LOCAL_REPO_LOCATION
sudo git fetch

echo "Checking out specified version of isaac-app"
sudo git checkout tags/$ISAAC_APP_VERSION_TO_DEPLOY

echo "Polling api to trigger reindex operation."
wget --spider http://localhost:8080/isaac-api/api

echo "Restarting apache so that users can reuse the site."
sudo service apache2 start

echo "Upgrade script complete."