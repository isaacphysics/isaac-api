#!/bin/bash

topics=("topic_logged_events" "topic_anonymous_logged_events")

if [[ -z "$START_TIMEOUT" ]]; then
    START_TIMEOUT=30
fi

start_timeout_exceeded=false
count=0
step=10

while true; do

	stat=$(ss -lnt | awk '$4 ~ /:9092$/')
	if [ ${#stat} -gt 0 ]; then
		break
	fi
	
    echo "waiting for kafka to be ready"
    sleep $step;
    count=$(expr $count + $step)
    if [ $count -gt $START_TIMEOUT ]; then
        start_timeout_exceeded=true
        break
    fi
done

if $start_timeout_exceeded; then
    echo "Not able to auto-create topic (waited for $START_TIMEOUT sec)"
    exit 1
fi

for ((i=0; i < ${#topics[@]}; i++))
do
	kafka-topics.sh --create --zookeeper zookeeper:2181 --topic ${topics[$i]} --partitions 1 --replication-factor 2
done