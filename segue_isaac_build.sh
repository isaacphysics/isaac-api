#! /bin/bash
# This script is a build script intended to only work on windows.

echo "Type the tag name to deploy for the isaac-api (BACKEND)"
read SEGUE_API_VERSION_TO_DEPLOY

echo "Type the environment to build the api for either 'live' or 'staging'"
read SEGUE_ENVIRONMENT_TO_BUILD

echo "Type the tag name to deploy for the isaac-app (FRONTEND)"
read SEGUE_APP_VERSION_TO_DEPLOY

BUILD_DIR=$LOCALAPPDATA/Temp/isaacDeploy

cd $LOCALAPPDATA
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
cd $BUILD_DIR

git clone -b $SEGUE_API_VERSION_TO_DEPLOY --depth 1 https://github.com/ucam-cl-dtg/isaac-api.git
cd isaac-api
mvn clean package -P $SEGUE_ENVIRONMENT_TO_BUILD
mv target/*.war ..
mv *.sh ..
cd ..
rm -rf isaac-api

git clone -b $SEGUE_APP_VERSION_TO_DEPLOY --depth 1 https://github.com/ucam-cl-dtg/isaac-app.git
cd isaac-app
npm install
grunt dist
mv isaac-app.tar.gz ..
cd ..
cmd.exe /c "rmdir /s /q isaac-app"
echo "Build complete" 
explorer .
#rm -rf //?/$BUILD_DIR/isaac-app