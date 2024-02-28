#!/bin/bash

## Seed application data to both connectors
for url in 'http://127.0.0.1:8081' 'http://127.0.0.1:8091'
do
  newman run --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/MVD.postman_collection.json > /dev/null

done

## Seed management data to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add participant alice
curl --location 'http://localhost:7082/api/management/v1/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data '{
    "roles":[],
    "serviceEndpoints":[
      {
         "type": "CredentialService",
         "serviceEndpoint": "http://localhost:7081/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDgz",
         "id": "credentialservice-1"
      }
    ],
    "active": true,
    "participantId": "did:web:localhost%3A7083",
    "did": "did:web:localhost%3A7083",
    "key":{
        "keyId": "key-1",
        "privateKeyAlias": "key-1",
        "publicKeyPem":"-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1l0Lof0a1yBc8KXhesAnoBvxZw5r\noYnkAXuqCYfNK3ex+hMWFuiXGUxHlzShAehR6wvwzV23bbC0tcFcVgW//A==\n-----END PUBLIC KEY-----"
    }
}'


# add participant bob
curl --location 'http://localhost:7092/api/management/v1/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data '{
    "roles":[],
    "serviceEndpoints":[
      {
         "type": "CredentialService",
         "serviceEndpoint": "http://localhost:7091/api/resolution/v1/participants/ZGlkOndlYjpsb2NhbGhvc3QlM0E3MDkz",
         "id": "credentialservice-1"
      }
    ],
    "active": true,
    "participantId": "did:web:localhost%3A7093",
    "did": "did:web:localhost%3A7093",
    "key":{
        "keyId": "key-1",
        "privateKeyAlias": "key-1",
        "publicKeyPem":"-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1l0Lof0a1yBc8KXhesAnoBvxZw5r\noYnkAXuqCYfNK3ex+hMWFuiXGUxHlzShAehR6wvwzV23bbC0tcFcVgW//A==\n-----END PUBLIC KEY-----"
    }
}'