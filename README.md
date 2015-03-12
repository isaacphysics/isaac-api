Segue and Isaac Server project
=============
Required Software to Run (Pre-requisites) 
=================
* Elastic Search (v1.4.x) - [Windows Installation](http://www.elasticsearch.org/overview/elkdownloads/), [Linux Installation](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/setup-repositories.html)
* Git
* MongoDb (optional depending on configuration)
* Tomcat 8

Development VM ([Internal only](http://dev.isaacphysics.org))
=====================
If Dev configuration requires changes then you can configure the following file: /rutherford-server/profiles/dev/dev-config.properties

To build a war for the dev server use:
mvn clean package -P dev

Staging VM ([Internal only](http://staging.isaacphysics.org))
=====================
If Staging configuration requires changes then you can configure the following file: /rutherford-server/profiles/staging/staging-config.properties

To build a war for the dev server use:
mvn clean package -P staging

Live VM ([Public Access](http://live.isaacphysics.org))
=====================
If Live configuration requires changes then you can configure the following file: /rutherford-server/profiles/live/live-config.properties

To build a war for the dev server use:
mvn clean package -P live

Local Builds
===========
Local configuration will need to be customised for each machine but you can build a war this using command.

Customise this file for local build settings: /rutherford-server/profiles/local/local-config.properties

mvn clean package -P local

The local build is set as the default profile so eclipse is happy.


Installation Notes
=================
1. After installing the pre-requisites check the config files under profiles/dev, profiles/staging, profiles/live and edit the values accordingly.
2. Build the segue CMS using the instructions above (alternatively if you are building isaacphysics you can use segue_isaac_build.sh).
3. Install the war file so that it is unpacked on a local tomcat8 server
4. Inside WEB-INF/classes/conf/ you will find a number of properties files.
	* segue-config-location.properties can be edited to tell segue where your other configuration files are stored
	* segue-config.properties contains most of the important configuration values and should be stored somewhere outside of the normal tomcat deploy directory.
	* live_version.properties is a mutable config file and tomcat8 should be given read and write permissions for where ever you store it.
5. Ensure that a git repository is available for segue to use as a database (see property LOCAL_GIT_DB) - tomcat user requires re-write permissions.
