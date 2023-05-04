# Minimum Viable Dataspace

The Minimum Viable Dataspace (MVD) is a sample implementation of a dataspace that leverages the [Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc).
The main purpose is to demonstrate the capabilities of the EDC, make dataspace concepts tangible based on a specific implementation, and to serve as a starting point to implement a custom dataspace.

The MVD allows developers and decision makers to gauge the current progress of the EDC and its capabilities to satisfy the functionality of a fully operational dataspace.

As a fully decentralized dataspace is hard to imagine, the MVD also serves the purpose of demonstrating how decentralization can be practically implemented.

## Documentation

Developer documentation can be found under [docs/developer](docs/developer), where the main concepts and decisions are captured as [decision records](docs/developer/decision-records/).

## Local Development Setup

The MVD backend and MVD UI (Data Dashboard) can be run locally for testing and development.

1. Check out the repository [eclipse-edc/DataDashboard](https://github.com/eclipse-edc/DataDashboard) or
   your corresponding fork.
2. Set the environment variable `MVD_UI_PATH` to the path of the DataDashboard repository. (See example below.)
3. Use the instructions in section `Publish/Build Tasks` [system-tests/README.md](system-tests/README.md) to set up a
   local MVD environment with the exception to use the profile `ui`. (See example below.)
    - In order to verify your local environment works properly, also follow section `Local Test Execution`
      in `system-tests/README.md` .

> Using the profile `ui` will create three MVD UIs (Data Dashboards) for each EDC participant in addition to the
> services described in [system-tests/README.md](system-tests/README.md).

```bash
export MVD_UI_PATH="/path/to/mvd-datadashboard"
docker compose --profile ui -f system-tests/docker-compose.yml up --build
```

> In Windows Docker Compose expects the path to use forward slashes instead of backslashes.

The profile `ui` creates three Data Dashboards each connected to an EDC participant. The respective `app.config.json`
files can be found in the respective directories:

- `resources/appconfig/company1/app.config.json`
- `resources/appconfig/company2/app.config.json`
- `resources/appconfig/company3/app.config.json`

That's it to run the local development environment. The following section `Run A Standard Scenario Locally` describes a
standard scenario which can be optionally used with the local development environment.

> Tip: The console output from the services spun up by Docker compose can be noisy. To decrease the output from the
> services on the console set `EDC_CATALOG_CACHE_EXECUTION_PERIOD_SECONDS` to a higher value, e.g. 60, for each EDC
> participant in `system-tests/docker-compose.yml`.

> Note: The container `cli-tools` will turn into the state `healthy` after registering successfully all participants and
> will keep running as an entrypoint to the services created by Docker compose. This is useful for local development in order
> to manually check commands against the participants (e.g. `company1`, `company2`, `company3`).

Sample how to enter the container `cli-tools` and test a command manually.

Host:

```bash
docker exec -it cli-tools bash
```

Container:

```bash
java -jar registration-service-cli.jar \
>    -d=did:web:did-server:registration-service \
>    --http-scheme \
>    -k=/resources/vault/company1/private-key.pem \
>    -c=did:web:did-server:company1 \
>    participants get
```

Output (container)

```json
{
  "did": "did:web:did-server:company1",
  "status": "ONBOARDED"
}
```

### Run A Standard Scenario Locally

Prerequisite: create a test document manually:

- Connect to the **local** blob storage account (provided by Azurite) of company1.
    - Storage account name: `company1assets`, storage account key: `key1`.
    - [Microsoft Azure Storage Explorer](https://azure.microsoft.com/features/storage-explorer/) can be used to connect to the local
      storage account on `localhost:10000`.
- Create a container named `src-container`. (Container name is defined for Postman request `Publish Master Data`
  in [deployment/data/MVD.postman_collection.json](deployment/data/MVD.postman_collection.json))
- Copy [deployment/azure/terraform/modules/participant/sample-data/text-document.txt](deployment/azure/terraform/modules/participant/sample-data/text-document.txt) into the newly created container.
    - N.B.: it does not have to be this exact file as long you create a file which has the name `text-document.txt`.

All this can also be done using Azure CLI with the following lines from the root of the MVD repository:

```bash
conn_str="DefaultEndpointsProtocol=http;AccountName=company1assets;AccountKey=key1;BlobEndpoint=http://127.0.0.1:10000/company1assets;"
az storage container create --name src-container --connection-string $conn_str
az storage blob upload -f ./deployment/terraform/participant/sample-data/text-document.txt --container-name src-container --name text-document.txt --connection-string $conn_str
```

This should result in a similar output as follows. Via the Microsoft Azure Storage Explorer it would be possible to
review the new container and the uploaded blob.

```bash
{
  "created": true
}

Finished[#############################################################]  100.0000%
{
  "etag": "\"0x1CC7CAB96842160\"",
  "lastModified": "2022-08-08T15:14:01+00:00"
}
```

The following steps initiate and complete a file transfer with the provided test document.

- Open the website of company1 (e.g. <http://localhost:7080>) and verify the existence of two assets in the
  section `Assets`.
- Open the website of the company2 (e.g. <http://localhost:7081>) and verify six existing assets from all participants in
  the `Catalog Browser`.
    - In the `Catalog Browser` click `Negotiate` for the asset `test-document_company1`.
        - There should be a message `Contract Negotiation complete! Show me!` in less than a minute.
- From the previous message click `Show me!`. If you missed it, switch manually to the section `Contracts`.
    - There should be a new contract. Click `Transfer` to initiate the transfer process.
    - A dialog should open. Here, select as destination `AzureStorage` and click `Start transfer`.
    - There should be a message `Transfer [id] complete! Show me!` in less than a minute. (Where `id` is a UUID.)
- To verify the successful transfer the Storage Explorer can be used to look into the storage account of `company2`.
    - Storage account name and key is set in `system-tests/docker-compose.yml` for the service `azurite`. Default name
      is `company2assets`, key is `key2`.
    - There should be new container in the storage account containing two files `.complete` and `text-document.txt`.

## Contributing

See [how to contribute](https://github.com/eclipse-edc/Connector/blob/main/CONTRIBUTING.md).
