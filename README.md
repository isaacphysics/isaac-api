Segue and Isaac Server project
=============

Build profiles set up for different environments in maven. You can build for the development server using:

Development VM ([Internal only](http://rutherford-dev.dtg.cl.cam.ac.uk/home))
=====================
If Dev configuration requires changes then you can configure the following file: /rutherford-server/profiles/dev/dev-config.properties

To build a war for the dev server use:
mvn clean package -P dev

Local Builds
===========
Local configuration will need to be customised for each machine but you can build a war this using command.

Customise this file for local build settings: /rutherford-server/profiles/local/local-config.properties

mvn clean package -P local

The local build is set as the default profile so eclipse is happy.

Required Software to Run 
=================
* Elastic Search - [Windows Installation](http://www.elasticsearch.org/overview/elkdownloads/), [Linux Installation](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/setup-repositories.html)
* Git
* MongoDb (optional depending on configuration)
* Tomcat 7
