#!/bin/bash

participants=("company1" "company2" "company3")

OUTPUTDIR="terraform"

# Generate participant keys
for participant in "${participants[@]}"; do
  mkdir -p "$OUTPUTDIR/generated/$participant"
  ./generate_key.sh "$OUTPUTDIR/generated/$participant/participant"
done

# Generate GaiaX  Key
mkdir -p "$OUTPUTDIR/generated/dataspace/"
sh ./generate_key.sh "$OUTPUTDIR/generated/dataspace/gaiaxkey"

# Generate Dataspace Authority Key
sh ./generate_key.sh "$OUTPUTDIR/generated/dataspace/authoritykey"
