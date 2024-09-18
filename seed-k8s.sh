#!/bin/bash

#
#  Copyright (c) 2024 Metaform Systems, Inc.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Metaform Systems, Inc. - initial API and implementation
#
#

## This script must be executed when running the dataspace from IntelliJ. Neglecting to do that will render the connectors
## inoperable!

## Seed application DATA to both connectors
echo
echo
echo "Seed data to 'provider-qna' and 'provider-manufacturing'"
for url in 'http://127.0.0.1/provider-manufacturing/cp' 'http://127.0.0.1/provider-qna/cp'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json
done

## Seed linked assets to Catalog Server
echo
echo
echo "Create linked assets on the Catalog Server"
newman run \
  --folder "Seed Catalog Server" \
  --env-var "HOST=http://127.0.0.1/provider-catalog-server/cp" \
  --env-var "PROVIDER_QNA_DSP_URL=http://provider-qna-controlplane:8082" \
  --env-var "PROVIDER_MF_DSP_URL=http://provider-manufacturing-controlplane:8082" \
  ./deployment/postman/MVD.postman_collection.json

## Seed management DATA to identityhubsl
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add consumer participant
echo
echo
echo "Create consumer participant"
CONSUMER_CONTROLPLANE_SERVICE_URL="http://consumer-controlplane:8082"
CONSUMER_IDENTITYHUB_URL="http://consumer-identityhub:7082"
DATA_CONSUMER=$(jq -n --arg url "$CONSUMER_CONTROLPLANE_SERVICE_URL" --arg ihurl "$CONSUMER_IDENTITYHUB_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/presentation/v1/participants/ZGlkOndlYjpjb25zdW1lci1pZGVudGl0eWh1YiUzQTcwODM6Y29uc3VtZXI=",
                "id": "consumer-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)/api/dsp",
                "id": "consumer-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:consumer-identityhub%3A7083:consumer",
           "did": "did:web:consumer-identityhub%3A7083:consumer",
           "key":{
               "keyId": "did:web:consumer-identityhub%3A7083:consumer#key-1",
               "privateKeyAlias": "did:web:consumer-identityhub%3A7083:consumer#key-1",
               "keyGeneratorParams":{
                  "algorithm": "EC"
               }
           }
       }')

curl --location "http://127.0.0.1/consumer/cs/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_CONSUMER"


# add provider participant

echo
echo
echo "Create provider participant"

PROVIDER_CONTROLPLANE_SERVICE_URL="http://provider-catalog-server-controlplane:8082"
PROVIDER_IDENTITYHUB_URL="http://provider-identityhub:7082"

DATA_PROVIDER=$(jq -n --arg url "$PROVIDER_CONTROLPLANE_SERVICE_URL" --arg ihurl "$PROVIDER_IDENTITYHUB_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/presentation/v1/participants/ZGlkOndlYjpwcm92aWRlci1pZGVudGl0eWh1YiUzQTcwODM6cHJvdmlkZXI=",
                "id": "provider-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)/api/dsp",
                "id": "provider-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:provider-identityhub%3A7083:provider",
           "did": "did:web:provider-identityhub%3A7083:provider",
           "key":{
               "keyId": "did:web:provider-identityhub%3A7083:provider#key-1",
               "privateKeyAlias": "did:web:provider-identityhub%3A7083:provider#key-1",
               "keyGeneratorParams":{
                  "algorithm": "EC"
               }
           }
       }')

curl --location "http://127.0.0.1/provider/cs/api/identity/v1alpha/participants/" \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_PROVIDER"