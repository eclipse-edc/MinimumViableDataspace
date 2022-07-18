#!/bin/bash

PARTICIPANTS=(provider consumer-eu consumer-us)

# Register dataspace participants
for i in "${PARTICIPANTS[@]}"; do
    echo "Registering $i"
    java -jar $REGISTRATION_SERVICE_CLI_JAR_PATH -s='http://localhost:8184/api' participants add --request='{ "name": "'$i'", "supportedProtocols": [ "ids-multipart" ], "url": "http://'$i':8282" }'
done
