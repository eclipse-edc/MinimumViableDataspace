# System tests

The system tests verify the end-to-end deployment of MVD, including:

- That the Identity Hub contains Verifiable Credentials deployed by the CD pipeline.
- That EDC Connectors can interact to populate a federated catalog, and copying a file from a provider (`company1`) to a
  consumer (`company2`) blob storage account.

System tests are run both in local deployment (using docker compose) and in the Azure cloud.

## Publish/Build Tasks

### MVD

EDC, RegistrationService and IdentityHub are available as Maven artifacts. Thus, `MVD` can be built by running the
following command from the root of the `MVD` project folder:

```bash
./gradlew build -x test
```

## Local Test Execution

- `MVD` system tests can be executed locally against a local `MVD` instance.
- `MVD` runs three `EDC Connectors` and one `Registration Service`.

_Note: Ensure that you are able to build `MVD` locally as described in the previous [section](#mvd)._

First, we need to build the `EDC Connector` and `RegistrationService` runtimes. As we are running `MVD` locally, we
include  `useFsVault` to indicate that the system will be using the local file-system based key vault.

From the `MVD` root folder, execute the following command to build the connector JAR and registration service JAR:

```bash
./gradlew -DuseFsVault="true" :launchers:connector:shadowJar
./gradlew -DuseFsVault="true" :launchers:registrationservice:shadowJar
```

Then, to bring up the dataspace, please execute the following command from the `MVD` root folder:

```bash
docker-compose -f system-tests/docker-compose.yml up --build
```

_Note for Windows PowerShell, the following commands should be used from the `MVD` project root:_

```powershell
docker-compose -f system-tests/docker-compose.yml up --build
```

Once completed, following services will start within their docker containers:

- 3 `EDC Connectors`
    - _company1_
    - _company2_
    - _company3_
- A `Registration Service`
- A `HTTP Nginx Server` (to serve DID Documents)
- An `Azurite` blob storage service

(EDC Connectors will also be seeded with initial required data using
a [postman collection](../deployment/data/MVD.postman_collection.json))

> Note, the `Newman` docker container will automatically stop after seeding initial data from postman scripts.

> The container `cli-tools` will turn into the state `healthy` after registering successfully all participants.

Sample for confirming successful run of container `cli-tools`.

Command:

```powershell
docker ps -a
```

Output:

```powershell
CONTAINER ID   IMAGE                                     COMMAND                   CREATED              STATUS                        PORTS                                                                              NAMES
22345bf0c595   system-tests_cli-tools                    "/bin/sh -c \"/app/en…"   About a minute ago   Up About a minute (healthy)                                                                                      cli-tools
```

Set the environment variable `TEST_ENVIRONMENT` to `local` to enable local blob transfer test and then run `MVD` system
test using the following command:

```bash
export TEST_ENVIRONMENT=local
./gradlew :system-tests:test
```

_Note for Windows PowerShell, the following commands should be used:_

```powershell
$Env:TEST_ENVIRONMENT = "local"
./gradlew :system-tests:test
```

> [Storage Explorer](https://azure.microsoft.com/features/storage-explorer/) can be used to connect to the `Azurite`
> storage container on `127.0.0.1:10000` port and under the `consumereuassets` account, the transferred blob can be
> viewed.

### Local Test Resources

The following test resources are provided in order to run `MVD` locally. `system-tests/docker-compose.yml` uses it to
start `MVD`.

<br>

---

<br>

Each `EDC Connector` has its own set of Private and Public keys in PEM and Java KeyStore formats,
e.g. `system-tests/resources/vault/company1`. These were generated using the following commands:

```bash
# generate a private key
openssl ecparam -name prime256v1 -genkey -noout -out private-key.pem
# generate corresponding public key
openssl ec -in private-key.pem -pubout -out public-key.pem
# create a self-signed certificate
openssl req -new -x509 -key private-key.pem -out cert.pem -days 360
```

Generated keys are imported to keystores e.g. `system-tests/resources/vault/company1/company1-keystore.jks`. Each
keystore has password `test123`.

> [KeyStore Explorer](https://keystore-explorer.org/) can be used to manage keystores from UI.

`MVD` local instances use a file-system based vault and its keys are managed using a java properties file
e.g.`system-tests/resources/vault/company[1,2,3]/company[1,2,3]-vault.properties`.

> ! IMPORTANT !
>
> *File system vault is __NOT__ a secure vault and thus should only be used for testing purposes*

<br>

---

<br>

Web DIDs are available under `system-tests/resources/webdid` folder. The `publicKeyJwk` section of each `did.json` was
generated by converting the corresponding public key to JWK format, for example company1 connector public key was
converted to JWK using following command:

```bash
docker run -i danedmunds/pem-to-jwk:1.2.1 --public --pretty < system-tests/resources/vault/company1/public-key.pem > key.public.jwk
```

<br>

---

<br>

### Debugging MVD locally

Follow the instructions in the previous sections to run an MVD with a consumer (`company2`) and provider (`company1`)
locally using docker-compose.

Once running, you can use a Java debugger to connect to the consumer (`company2`, port 5006) and provider (`company1`,
port 5005) instances. If you are using IntelliJ you can use the provided "EDC company1", "EDC company2" or "EDC
company3" [runtime configurations](../.run) to remote debug the connector instances.

### Issuing requests manually with Postman

A [postman collection](../deployment/data/MVD.postman_collection.json) can be used to issue requests to an MVD instance
of your choice. You will need to adapt the environment variables accordingly to match your target MVD instance.
