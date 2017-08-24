#!/bin/bash

# Create topics here
/create-topics.sh &


# Run kafka
/opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties