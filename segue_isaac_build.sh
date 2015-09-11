#! /bin/bash
# This script is a build script intended to only work on windows.

set -e # Exit on failure

read -p "isaac-app version to deploy (e.g. v1.3.0 or for a SNAPSHOT build type 'master'):" VERSION_TO_DEPLOY

BUILD_DIR=$LOCALAPPDATA/Temp/isaacDeploy

echo Building in $BUILD_DIR

cd $LOCALAPPDATA
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
cd $BUILD_DIR

git clone -b $VERSION_TO_DEPLOY --depth 1 https://github.com/ucam-cl-dtg/isaac-app.git
cd isaac-app
npm install
grunt dist

# determine segue version to use.
SEGUE_VERSION=`grunt segue-version | grep ^segueVersion | sed s/.*://`
if [ $VERSION_TO_DEPLOY = "master" ]; then
	SEGUE_VERSION="master"
else
	SEGUE_VERSION=v$SEGUE_VERSION
fi

mv isaac-app.tar.gz ..
cd ..
#rm -rf //?/$BUILD_DIR/isaac-app
cmd.exe /c "rmdir /s /q isaac-app"

git clone -b $SEGUE_VERSION --depth 1 https://github.com/ucam-cl-dtg/isaac-api.git
cd isaac-api
mvn clean package -P deploy
mv target/*.war ..
mv *.sh ..
rm -rf $BUILD_DIR/generated-config
mv target/generated-config/ $BUILD_DIR
cd ..
rm -rf isaac-api
echo "Build complete" 
explorer .
