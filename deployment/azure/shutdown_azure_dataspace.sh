#!/bin/bash

echo "#########################"
echo "### SHUTDOWN dataspace"
echo "#########################"


echo "Stop running Dataspace participants"
docker-compose -f docker/docker-compose.yml down --remove-orphans
echo

echo "Destroy resources for dataspace"
sh ./destroy_dataspace_resources.sh
echo
echo " ### DONE ###"