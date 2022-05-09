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

In the commands below, adapt the variables to the Storage Account and Key Vault used in your deployment. Do not change the API Key value `ApiKeyDefaultValue`, it is hard-coded in the `docker-compose.yml` file.

Seed the provider data:
```
API_KEY=ApiKeyDefaultValue EDC_HOST=localhost ASSETS_STORAGE_ACCOUNT={storage_account} ./deployment/seed-data.sh
```

Run test:
```
API_KEY=ApiKeyDefaultValue PROVIDER_MANAGEMENT_URL=http://localhost:9191 CONSUMER_MANAGEMENT_URL=http://localhost:9192 PROVIDER_IDS_URL=http://provider:8282 CONSUMER_KEY_VAULT={key_vault_name} CONSUMER_CATALOG_URL=http://localhost:8182/api/federatedcatalog ./gradlew :system-tests:test
```
