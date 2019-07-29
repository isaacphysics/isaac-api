#### Physics and Computer Science

If you want to get both physics and computer science content working, you'll need two copies of the `segue-config.properties` file. For consistency, use `segue-config.phy.properties` and `segue-config.cs.properties`.

For the Computer Science version, you'll need to update:

 - The names and email addresses, if you want to use Computer Science ones.
 - `CONTENT_INDICES_LOCATION` to use `content_indices.cs.properties` (which is needed for the commit SHAs to be valid).
 - The Git repo should be altered to `isaac-content-2` for both the local and remote, and you'll need the relevant SSH key.
 - Update the ElasticSearch options to be `SEARCH_CLUSTER_NAME=cs-isaac`, `SEARCH_CLUSTER_PORT=9301`, `SEARCH_CLUSTER_INFO_PORT=9201`.
 - Update `GOOGLE_CALLBACK_URI` to use `localhost:8003`.

In Jetty, add a new configuration like `Jetty (CS)` with the command-line `jetty:run -Dconfig.location=/path/to/segue-config.cs.properties`.

Note that with this configuration it is only possible to run one of the CS or Physics APIs at a time, and they will share a database which may cause issues loading questions.
It would be possible to override the API port and run both versions simultaneously if necessary, but the configuration in the apps would need altering too.
