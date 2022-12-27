#!/bin/bash

if [ "$#" -lt 1 ]; then
  echo "Usage: sh $0 <KEY_PREFIX>"
  exit 1
fi

KEY_PREFIX="$1"
PEMFILE="${KEY_PREFIX}.pem"

if [ -f "$PEMFILE" ]; then
    echo "  PEM $PEMFILE exists, will not recreate. Delete this file to force re-generation."
    exit 0
fi

echo "generate $PEMFILE"
openssl ecparam -name prime256v1 -genkey -noout -out "$PEMFILE"
echo "generate public key"
openssl ec -in "$PEMFILE" -pubout -out "${KEY_PREFIX}.public.pem" > /dev/null
echo "generate JWK"
docker run --rm -i danedmunds/pem-to-jwk:1.2.1 --public --pretty <"${KEY_PREFIX}.public.pem" >"${KEY_PREFIX}.public.jwk"