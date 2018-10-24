#!/bin/bash

# Create a symbolic link to the segue config properties from a .env file.
# Docker compose scripts use the .env as a file of default values for when it does environment variable substitution.
ln -s ../isaac-other-resources/segue-config.properties .env