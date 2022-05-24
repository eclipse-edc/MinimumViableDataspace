## System tests

The test uses the key vault secret to connect to the storage accounts and copy a file from provider to consumer storage account.

### Running test locally

Deploy MVD using the GitHub `Deploy` pipeline. We will run EDC instances locally, connected to the storage accounts and key vaults deployed on Azure.

From the build result, download the artifact named `testing-configuration` and extract the file `.env` into the `system-tests` directory (note that the file could be hidden in your file explorer due to its prefix).

In the file, add the application client secret value under the `APP_CLIENT_SECRET` key. It is used to access Key Vault.

Build the EDC launcher:

```
./gradlew :launcher:shadowJar
```

Run EDC consumer, provider and data seeding:

```
docker-compose -f system-tests/docker-compose.yml up --build
```

In the commands below, adapt the variable values marked with `$` to use the value from the `.env` file.

Login in to Azure:
```
az login --service-principal --user "$APP_CLIENT_ID" --password "$APP_CLIENT_SECRET" --tenant "$APP_TENANT_ID"
```

| ℹ️ Information                                                |
| :----------------------------------------------------------- |
| You could also login interactively with your user identity (`az login`), and [grant yourself at least the *Key Vault Secrets User*](https://docs.microsoft.com/azure/key-vault/general/rbac-guide) role to the Key Vault below. A good option is to grant the *Key Vault Secrets Officer* at the subscription level to the whole development team, so they can read and write secrets on MVD deployments as needed. |

Run tests:
```
CONSUMER_KEY_VAULT="$CONSUMER_KEY_VAULT" ./gradlew :system-tests:test
```

### Debugging MVD locally

Follow the instructions in the previous sections to run an MVD with a consumer and provider locally using docker-compose. 

Once running, you can use a Java debugger to connect to the consumer (port 5006) and provider (port 5005) instances. If you are using IntelliJ you can use the provided "EDC consumer" or "EDC provider" [runtime configurations](../.run) to remote debug the connector instances.

### Issuing requests manually with Postman

A [postman collection](../deployment/data/MVD.postman_collection.json) can be used to issue requests to an MVD instance of your choice. You will need to adapt the environment variables accordingly to match your target MVD instance.