# Getting started

## Development

### Running locally

TBD

### Building the images

default image can be built from the root via

`docker build -t {registry}/{group_name}/isaac-api`

etl image can be built from the root via

`docker built -t {registry}/{group_name}/isaac-api-etl --build-arg MVN_PACKAGE_PARAM='-P etl'`

## Automated builds

Docker images are published in the github registry - checkout [publish](../.github/workflows/publish.yaml)