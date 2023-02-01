# isaac-api

![Java CI with Maven](https://github.com/isaacphysics/isaac-api/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master)
[![codecov](https://codecov.io/gh/isaacphysics/isaac-api/branch/master/graph/badge.svg)](https://codecov.io/gh/isaacphysics/isaac-api)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/isaacphysics/isaac-api.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/isaacphysics/isaac-api/context:java)


`isaac-api` is the server and API for the [Isaac Physics](https://isaacphysics.org/about) and [Isaac Computer Science](https://isaaccomputerscience.org/about) projects. Together with [`isaac-react-app`](https://github.com/isaacphysics/isaac-react-app), it forms the core stack of the Isaac platform. This repository is a fork of the [Isaac Physics repository](https://github.com/isaacphysics/isaac-api).

The API runs on Jetty, and runs in [Docker](https://www.docker.com/) in production.

<<<<<<< Updated upstream
[docs](./docs)
=======

## Synchronizing changes with Isaac Physics repository
To synchronize this fork with changes from the [Isaac Physics repository](https://github.com/isaacphysics/isaac-api), this fork maintains a `master` branch for synchronizing with Isaac Physics. The process is as follows:
* Synchronize this repo's `master` branch with the physics `master` branch by clicking on Github's "Sync Fork" button.
* Create a new branch off of `master` for creating a pull request into `main` e.g. `git checkout -b merge-master-2023-01-01'. This branch can be deleted once the pull request merges and leave the `master` branch intact.
* Create a pull request from the above new branch to `main`, review the changes to ensure no conflicts, and merge the pull request.
* Here's an [example](https://github.com/isaaccomputerscience/isaac-api/pull/9) of such a pull request.

To introduce changes in this repository back to the physics repository, branch your feature branch off of `master` and create a pull request back to the physic's `master` branch (this is the standard Github fork workflow.)
>>>>>>> Stashed changes
