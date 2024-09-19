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

## Seed asset/policy/contract-def data to both "provider-qna" and "provider-manufacturing"
for url in 'http://127.0.0.1:8191' 'http://127.0.0.1:8291'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null
done

## Seed linked assets to Catalog Server "Bob"
newman run \
  --folder "Seed Catalog Server" \
  --env-var "HOST=http://127.0.0.1:8091" \
  --env-var "PROVIDER_QNA_DSP_URL=http://localhost:8192" \
  --env-var "PROVIDER_MF_DSP_URL=http://localhost:8292" \
  ./deployment/postman/MVD.postman_collection.json > /dev/null


## Seed identity data to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add participant "consumer"
echo
echo
echo "Create consumer participant"
PEM_CONSUMER=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/consumer_public.pem)
DATA_CONSUMER=$(jq -n --arg pem "$PEM_CONSUMER" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "http://localhost:7081/api/presentation/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
                "id": "consumer-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "http://localhost:8082/api/dsp",
                "id": "consumer-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:localhost%3A7083",
           "did": "did:web:localhost%3A7083",
           "key":{
               "keyId": "did:web:localhost%3A7083#key-1",
               "privateKeyAlias": "key-1",
               "publicKeyPem":"\($pem)"
           }
       }')

curl -s --location 'http://localhost:7082/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_CONSUMER"

# add participant "provider"
echo
echo
echo "Create provider participant"
PEM_PROVIDER=$(sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' deployment/assets/provider_public.pem)
DATA_PROVIDER=$(jq -n --arg pem "$PEM_PROVIDER" '{
            "roles":[],
            "serviceEndpoints":[
              {
                 "type": "CredentialService",
                 "serviceEndpoint": "http://localhost:7091/api/presentation/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDkz",
                 "id": "provider-credentialservice-1"
              },
              {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "http://localhost:8092/api/dsp",
                "id": "provider-catalogserver-dsp"
              }
            ],
            "active": true,
            "participantId": "did:web:localhost%3A7093",
            "did": "did:web:localhost%3A7093",
            "key":{
                "keyId": "did:web:localhost%3A7093#key-1",
                "privateKeyAlias": "key-1",
                "publicKeyPem":"\($pem)"
            }
      }')

curl -s --location 'http://localhost:7092/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_PROVIDER"