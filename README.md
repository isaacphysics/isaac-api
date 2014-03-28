Segue and Isaac Server project
=============

Build profiles set up for different environments. You can build for the development server using:

Shared Development VM:
=====================
If Dev configuration requires changes then you can configure the following file: /rutherford-server/profiles/dev/dev-config.properties

To build a war for the dev server use:
mvn -o clean package -P dev

Local Builds
===========
Local configuration will need to be customised for each machine but you can build a war this using command.

Customise this file for local build settings: /rutherford-server/profiles/local/local-config.properties

mvn -o clean package -P local

The above is set as the default profile so eclipse is happy.

