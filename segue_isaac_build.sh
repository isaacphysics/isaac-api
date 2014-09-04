#! /bin/bash
# This script is a build script intended to only work on windows.

set -e # Exit on failure

read -p "Version to deploy: " VERSION_TO_DEPLOY
read -p "Segue environment (live/staging): " SEGUE_ENVIRONMENT_TO_BUILD

BUILD_DIR=$LOCALAPPDATA/Temp/isaacDeploy

echo Building in $BUILD_DIR

cd $LOCALAPPDATA
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
cd $BUILD_DIR

git clone -b $VERSION_TO_DEPLOY --depth 1 https://github.com/ucam-cl-dtg/isaac-api.git
cd isaac-api
mvn clean package -P $SEGUE_ENVIRONMENT_TO_BUILD
mv target/*.war ..
mv *.sh ..
cd ..
rm -rf isaac-api

git clone -b $VERSION_TO_DEPLOY --depth 1 https://github.com/ucam-cl-dtg/isaac-app.git
cd isaac-app
npm install
grunt dist
mv isaac-app.tar.gz ..
cd ..
#rm -rf //?/$BUILD_DIR/isaac-app
cmd.exe /c "rmdir /s /q isaac-app"
echo "Build complete" 
explorer .
