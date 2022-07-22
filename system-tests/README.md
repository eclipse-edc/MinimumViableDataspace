# System tests

The system tests copy a file from a provider to a consumer blob storage account.

## Publish/Build Tasks

> ! Important Note !
> 
> MVD dependencies are Eclipse DataSpaceConnector(EDC) and Registration Service. Both of these dependencies 
> are __not__ published to any central artifact repository yet, so in local development we have to use locally 
> published dependencies.
>
>In order to use the correct version of each repo required by the `MVD`, you need to look in [action.yml](./.github/actions/../../../.github/actions/gradle-setup/action.yml) for the hashes of the versions of the `EDC` and the `Registration Service` that are being used by the `MVD`.
>
> For Example, the `Registration Service` and `EDC` repository hash can be found in the _Checkout_ steps  (in the `ref` property) of [action.yml](./.github/actions/../../../.github/actions/gradle-setup/action.yml):

```yml
    - name: Checkout EDC
      uses: actions/checkout@v2
      with:
        repository: eclipse-dataspaceconnector/DataSpaceConnector
        path: DataSpaceConnector
        ref: 3ff940b720f44826df28e893fb31344eb6faacef

    - name: Checkout Registration Service
      uses: actions/checkout@v2
      with:
        repository: eclipse-dataspaceconnector/RegistrationService
        path: RegistrationService
        ref: 374c14bcca23ddb1dcd7476a27264510e54de7fa
```

> After you have cloned the `EDC` and `Registration Service` repos locally you should run the command to
> `reset` to the specific hash.  
>
> For Example:

```bash
# EDC (in the EDC root folder)
git reset --hard 3ff940b720f44826df28e893fb31344eb6faacef

# Registration Service (in the Registration Service root folder)
git reset --hard 374c14bcca23ddb1dcd7476a27264510e54de7fa
```

> Now you can follow the rest of the process below.  
> Once the publications are available in _Maven Central_ this process will not be necessary
> 
<br />

### EDC

<br />

Execute the following command  from `EDC` root folder.

```bash
./gradlew publishToMavenLocal -Pskip.signing
```

_Note for Windows PowerShell, the following command should be used:_

```powershell
./gradlew publishToMavenLocal -P"skip.signing"
```

<br />

### Registration Service

<br />

Execute the following command from `Registration Service` root folder:

```bash
./gradlew publishToMavenLocal
```

<br />

### MVD

<br />

Now that the publishing to the local repositories has been completed, `MVD` can be built by running the following command from the root of the `MVD` project folder:

```bash
./gradlew build -x test
```

## Local Test Execution

- `MVD` system tests can be executed locally against a local `MVD` instance. 
- `MVD` runs three `EDC Connectors` and one `Registration Service`.

_Note: Ensure that you are able to build `MVD` locally as described in the previous [section](#mvd)._

First, we need to build `EDC Connector launcher` and `Registration Service launcher`.

From the `MVD` root folder, execute the following command:

```bash
./gradlew -DuseFsVault="true" :launcher:shadowJar
```

From the `Registration Service` root folder, execute the following command:

```bash
./gradlew :launcher:shadowJar
```

From the `MVD` root folder execute the following commands to set the `Registration Launcher` path environment variable and start `MVD` using the `docker-compose.yml` file.

```bash
export REGISTRATION_SERVICE_LAUNCHER_PATH=/home/user/RegistrationService/launcher
docker-compose -f system-tests/docker-compose.yml up --build
```

_Note for Windows PowerShell, the following commands should be used from the `MVD` project root.  (The path will depend on the location of the your `RegistrationService` project root):_

```powershell
$Env:REGISTRATION_SERVICE_LAUNCHER_PATH="c:/RegistrationService/launcher"
docker-compose -f system-tests/docker-compose.yml up --build
```

Once completed, following services will start within their docker containers:
- 3 `EDC Connectors`
  - _consumer-us_
  - _consumer-eu_
  - _provider_ (which will also be seeded with initial required data using a [postman collection](../deployment/data/MVD.postman_collection.json))
- A `Registration Service`
- A `HTTP Nginx Server` (to serve DIDs) 
- An `Azurite` blob storage service 
  
_Note, the `Newman` docker container will automatically stop after seeding initial data from postman scripts._

`EDC Connectors` need to be registered using `Registration Service` CLI client jar. After publishing `Registration Service` locally the client jar should be available under the `Registration Service` root project folder in _client-cli/build/libs_.

```bash
# Replace path according to your local set up
export REGISTRATION_SERVICE_CLI_JAR_PATH=c:/RegistrationService/client-cli/build/libs/registration-service-cli.jar

# Register Participants
./system-tests/resources/register-participants.sh
```

_Note for Windows PowerShell, the following commands should be run the the `MVD` root project folder._

```powershell
# Replace path according to your local set up

$Env:REGISTRATION_SERVICE_CLI_JAR_PATH = "c:\RegistrationService\client-cli\build\libs\registration-service-cli.jar"

# Register Provider
java -jar $Env:REGISTRATION_SERVICE_CLI_JAR_PATH -s="http://localhost:8184/api" participants add --request="{ \`"name\`": \`"provider\`", \`"supportedProtocols\`": [ \`"ids-multipart\`" ], \`"url\`": \`"http://provider:8282\`" }"

# Register Consumer-EU
java -jar $Env:REGISTRATION_SERVICE_CLI_JAR_PATH -s="http://localhost:8184/api" participants add --request="{ \`"name\`": \`"consumer-eu\`", \`"supportedProtocols\`": [ \`"ids-multipart\`" ], \`"url\`": \`"http://consumer-eu:8282\`" }"

# Register Consumer-US
java -jar $Env:REGISTRATION_SERVICE_CLI_JAR_PATH -s="http://localhost:8184/api" participants add --request="{ \`"name\`": \`"consumer-us\`", \`"supportedProtocols\`": [ \`"ids-multipart\`" ], \`"url\`": \`"http://consumer-us:8282\`" }"
```

Set the environment variable `TEST_ENVIRONMENT` to `local` to enable local blob transfer test and then run `MVD` system test using the following command:

```bash
export TEST_ENVIRONMENT=local
./gradlew :system-tests:test
```

_Note for Windows PowerShell, the following commands should be used:_

```powershell
$Env:TEST_ENVIRONMENT = "local"
./gradlew :system-tests:test
```

> [Storage Explorer](https://azure.microsoft.com/features/storage-explorer/) can be used to connect to the `Azurite` storage container on `127.0.0.1:10000` port and under `consumereuassets`, account transferred blob can be viewed.

### Local Test Resources

The following test resources are provided in order to run `MVD` locally. `system-tests/docker-compose.yml` usages it to start `MVD`.

<br>

---

<br>

Each `EDC Connector` has its own set of Private and Public keys with java keystore e.g. `system-tests/resources/provider`. These were generated using the following commands:

```bash
# generate a private key
openssl ecparam -name prime256v1 -genkey -noout -out private-key.pem
# generate corresponding public key
openssl ec -in private-key.pem -pubout -out public-key.pem
# create a self-signed certificate
openssl req -new -x509 -key private-key.pem -out cert.pem -days 360
```

Generated keys are imported to keystores e.g. `system-tests/resources/provider/provider-keystore.jks`. Each keystore has password `test123`.

> [KeyStore Explorer](https://keystore-explorer.org/) can be used to manage keystores from UI.

`MVD` local instance usage `EDC File System Vault` and its keys are managed using a java properties file e.g.`system-tests/resources/provider/provider-vault.properties`.

> ! IMPORTANT !
> 
> *File System Vault is NOT a secure vault and thus should only be used for testing purposes*

<br>

---

<br>

Web DIDs are available under `system-tests/resources/webdid` folder. The `publicKeyJwk` section of each `did.json` was generated by converting the corresponding public key to JWK format, for example provider connector public key was converted to JWK using following command:

```bash
docker run -i danedmunds/pem-to-jwk:1.2.1 --public --pretty < system-tests/resources/provider/public-key.pem > key.public.jwk
```
<br>

---

<br>

### Debugging MVD locally

Follow the instructions in the previous sections to run an MVD with a consumer and provider locally using docker-compose.

Once running, you can use a Java debugger to connect to the consumer (port 5006) and provider (port 5005) instances. If you are using IntelliJ you can use the provided "EDC consumer" or "EDC provider" [runtime configurations](../.run) to remote debug the connector instances.

### Issuing requests manually with Postman

A [postman collection](../deployment/data/MVD.postman_collection.json) can be used to issue requests to an MVD instance of your choice. You will need to adapt the environment variables accordingly to match your target MVD instance.
