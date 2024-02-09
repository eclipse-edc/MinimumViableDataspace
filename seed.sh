#!/bin/bash

## Seed application data to both connectors
for url in 'http://127.0.0.1:8081' 'http://127.0.0.1:8091'
do
  newman run --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD_.postman_collection.json

done

## Seed management data to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# alice
newman run --folder "Create Participant" \
  --env-var "CS_URL=http://127.0.0.1:7082" \
  --env-var "IH_API_TOKEN=$API_KEY" \
  --env-var "NEW_PARTICIPANT_ID=did:web:localhost%3A7083" \
  ./deployment/postman/MVD_.postman_collection.json

# bob
newman run --folder "Create Participant" \
  --env-var "CS_URL=http://127.0.0.1:7092" \
  --env-var "IH_API_TOKEN=$API_KEY" \
  --env-var "NEW_PARTICIPANT_ID=did:web:localhost%3A7093" \
  ./deployment/postman/MVD_.postman_collection.json