this directory contains JSON files that make up the Federated Cache Node Directory
and it should be used for running the MVD using `docker-compose`.
Note that the `docker-compose.yaml` is located in the `system-tests` directory, and should then define the following environment variables:

- `NODES_JSON_DIR`: path to this directory
- `NODES_JSON_FILES_PREFIX`: prefix for all files in this directory, should be `registry-`
