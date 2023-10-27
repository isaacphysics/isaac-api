#### Computer Science

If you want to get computer science content working, you'll need a copy of the `segue-config.properties` file.

For the Computer Science version, you'll need to update:

 - The names and email addresses, if you want to use Computer Science ones.
 - `CONTENT_INDICES_LOCATION` to use `content_indices.properties` (which is needed for the commit SHAs to be valid).
 - The Git repo should be altered to `isaac-content-2` for both the local and remote, and you'll need the relevant SSH key.
 - Update the ElasticSearch options to be `SEARCH_CLUSTER_NAME=cs-isaac`, `SEARCH_CLUSTER_PORT=9301`, `SEARCH_CLUSTER_INFO_PORT=9201`.
 - Update `GOOGLE_CALLBACK_URI` to use `localhost:8003`.

In Jetty, add a new configuration like `Jetty` with the command-line `jetty:run -Dconfig.location=/path/to/segue-config.properties`.
