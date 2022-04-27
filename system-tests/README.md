## System tests

The test use the key vault secret to connect to the storage accounts and copy a file
from provider to consumer storage account.

### Running test locally

Deploy MVD using the GitHub pipeline, adapting it in a branch to skip the destroy step.
This leaves a storage account and a key vault for each of the consumer and the provider.

Copy `system-tests/.env.example` to `system-tests/.env` and adapt the values.

Build and run EDC consumer and provider:

```
./gradlew :launcher:shadowJar

docker-compose -f system-tests/docker-compose.yml build
docker-compose -f system-tests/docker-compose.yml up
```

Seed the provider data:
```
EDC_HOST=localhost ASSETS_STORAGE_ACCOUNT=257company2assets ./deployment/seed-data.sh
```

Run test:
```
PROVIDER_MANAGEMENT_URL=http://localhost:9191 CONSUMER_MANAGEMENT_URL=http://localhost:9192 PROVIDER_IDS_URL=http://provider:8282 CONSUMER_KEY_VAULT=kv257company1 ./gradlew :system-tests:test
```
