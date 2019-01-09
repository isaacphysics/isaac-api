# Isaac Docker Database Configuration

### Scripts

`dump-db.sh` is a script for dumping one of the server databases and takes the environment as its first argument. The command it contains can be adapted to dump a local database if required by replacing `pg-$1` with `postgres`.

`dump-db-data.sh` works like `dump-db.sh` but does not include any of the database schema information, only the data.

`load-db.sh` is the opposite of `dump-db.sh` and loads a dump into a database container. It again takes the environment as the first argument.

### Schema
There are two files that comprise the database schema for Isaac databases; one containing the bulk of the table schema, and the other containing the database functions required by Isaac.

`postgres-rutherford-create-script.sql` contains the main schema, and should be run to initialise the database.

`postgres-rutherford-functions.sql` contains database functions required by Isaac that will likely be updated more often than the schema itself.

Care should be taken when dumping a new version of the schema not to add the functions to the main schema file but to keep them separate.