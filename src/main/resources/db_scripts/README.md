# DB Docker Config

Running `build-pg-image.sh` will create a postgres image that will initialise itself with a blank rutherford DB on first start.

Running `create-pgdata-container.sh` will create a container used solely to connect to a named volume `pgdata`.

`run-pg-live.sh` will remove any old live postgres containers (even if they're running) and start a new one. The data will be mounted from the pgdata container created above.

`run-pg-test.sh` will remove any old test postgres containers (even if they're running) and start a new one. No data volumes will be mounted, meaning that removing this container
when the tests are completed will delete the data.
