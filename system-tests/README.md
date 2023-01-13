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

## Test Execution using embedded services

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
./gradlew :system-tests:test -DincludeTags="ComponentTest,EndToEndTest"
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

<br/>

---

<br/>

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



Web DIDs are available under `system-tests/resources/webdid` folder. The `publicKeyJwk` section of each `did.json` was
generated by converting the corresponding public key to JWK format, for example company1 connector public key was
converted to JWK using following command:

```bash
docker run -i danedmunds/pem-to-jwk:1.2.1 --public --pretty < system-tests/resources/vault/company1/public-key.pem > key.public.jwk
```

<br/>

---

<br/>

## Test Execution using cloud resources

Like running tests against embedded services we can run tests against an MVD that uses actual cloud resources, such as
Azure Keyvault or Azure Blobstore.

For that, we need to rebuild the project so that it does not use the filesystem-based vault:

```shell
./gradlew shadowJar
```

### Prepare cloud subscription

Next, we need to create cloud infrastructure that'll be used by our 3 connectors. Please navigate to `deployment/azure`.
For that, we've created a script called `create_azure_dataspace.sh` that will take care of creating cloud infra and
setting up our connector configuration files.

> ! IMPORTANT !
>
> *Important: for the next steps you'll need certain environment variable set up, otherwise the script will fail!*

Please refer to [this guide](../docs/developer/continuous-deployment/continuous_deployment.md) on how to set up your
Azure subscription for CI/CD. The easiest way is to configure and run the Terraform package as described there. Once
that is done,
simply source the `env-vars` file:

```shell
cd <project-root>/resources/setup_azure_ad
terraform init # <-- described in the guide
terraform apply # <-- described in the guide

source env-vars
```

that will export all relevant environment variables in the current shell and make them available for
the `create_azure_dataspace.sh` script. To verify that, simply `echo $ARM_CLIENT_ID` and that should print out a GUID.

**Note: you will need admin rights in the Azure subscription for this!**

### Creating cloud resources

Once the subscription is prepared, i.e. all the correct permissions are set, app IDs are created, etc. we can execute
the script. Navigate back to `<project-root>/deployment/azure` and run

```shell
./create_azure_dataspace.sh
```

The script will perform these essential steps:

- generate asymmetric keypairs for every dataspace participant and the registration service
- create a Terraform backend configuration
- create a Terraform variable file (`*.tfvars)
- initialize and run Terraform (this creates the infra)
- generate `*.env` files for every participant and the registration service

### Running the dataspace + tests

Just like in the [previous chapter](README.md#test-execution-using-embedded-services) we start up our dataspace
using `docker-compose`. One small difference is that seeding is now done with a separate script instead of inside
another docker container. The reason for this is easier traceability and debuggability.

```shell
cd <project-root>/deployment/azure
docker-compose docker/docker-compose.yaml --build --wait
./seed_dataspace.sh
```

To run the tests, simply replicate the steps from the chapter about
the [embedded services](README.md#test-execution-using-embedded-services):

```shell
cd <project-root>
export TEST_ENVIRONMENT=local
./gradlew :system-tests:test -DincludeTags="ComponentTest,EndToEndTest"
```

The last command will push master data (policies, assets) and VerifiableCredentials to the participants, and then
register them one after the other with the registration service.

### Destroying the dataspace again

This step assumes there is still the `terraform.tfvars` and `backend.conf` file present from the setup step. If that is
not the case simply re-run the `setup_azure_dataspace.sh` script again. If all the cloud resources are still there, it
won't create new ones.
To stop the docker containers and destroy all cloud resources, simply execute:

```shell
cd <project-root>/deployment/azure
./shutdown_azure_dataspace.sh
```

<br/>

---

<br/>

## Debugging MVD locally

Follow the instructions in the previous sections to run an MVD with a consumer (`company2`) and provider (`company1`)
locally using docker-compose.

Once running, you can use a Java debugger to connect to the consumer (`company2`, port 5006) and provider (`company1`,
port 5005) instances. If you are using IntelliJ you can use the provided "EDC company1", "EDC company2" or "EDC
company3" [runtime configurations](../.run) to remote debug the connector instances.

Alternately, when running MVD with [cloud resources](README.md#test-execution-using-cloud-resources), you could use
the generated `*.env` files located in `deployment/azure/docker/` as launch
configuration [EnvFiles](https://plugins.jetbrains.com/plugin/7861-envfile) in Intellij run one or multiple participants
directly from your IDE.

## Issuing requests manually with Postman

A [postman collection](../deployment/data/MVD.postman_collection.json) can be used to issue requests to an MVD instance
of your choice. You will need to adapt the environment variables accordingly to match your target MVD instance.
