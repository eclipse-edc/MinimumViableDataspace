#!/bin/bash

PARTICIPANTS=(provider consumer-eu consumer-us)

# Register dataspace participants
for i in "${PARTICIPANTS[@]}"; do
    echo "Registering $i"
    java -jar registration-service-cli.jar -d="did:web:did-server:$i" -k=/resources/vault/$i/private-key.pem -s='http://registration-service:8184/authority' participants add --ids-url "http://$i:8282"
done
