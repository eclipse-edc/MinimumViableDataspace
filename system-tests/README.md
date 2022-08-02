# System tests

The system tests copies a file from a provider to a consumer blob storage account.

## Publish/Build Tasks

> ! Important Note !
>
> MVD depends on Eclipse DataSpaceConnector(EDC), Identity Hub and Registration Service. These dependencies
> are __not__ published to any central artifact repository yet, so in local development we have to use locally
> published dependencies.
>
>In order to use the correct version of each repo required by the `MVD`, you need to look in [action.yml](./.github/actions/../../../.github/actions/gradle-setup/action.yml) for the hashes of the versions of the `EDC`, `Identity Hub` and the `Registration Service` that are being used by the `MVD`.
>
> For Example for the dependency repositories:
> - `Registration Service`
> - `Identity Hub`
> - `EDC`
>
>  the hash (which is subject to change from the values presented here as an example) can be found in the _Checkout_ steps  (in the `ref` property) of [action.yml](./.github/actions/gradle-setup/action.yml):

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

    - name: Checkout Identity Hub
      uses: actions/checkout@v2
      with:
        repository: eclipse-dataspaceconnector/IdentityHub
        path: IdentityHub
        ref: bc13cf0cb8589b792eef733c7cf7b3422476add5

```

> After you have cloned the `EDC`, `Identity Hub` and `Registration Service` repos locally you should run the command to
> `checkout` to the specific hash.
>
> For Example:

```bash
# EDC (in the EDC root folder)
git checkout 3ff940b720f44826df28e893fb31344eb6faacef

# Identity Hub (in the Identity Hub root folder)
git checkout bc13cf0cb8589b792eef733c7cf7b3422476add5

# Registration Service (in the Registration Service root folder)
git checkout 374c14bcca23ddb1dcd7476a27264510e54de7fa
```

> Now you can follow the rest of the process below.
> Once the publications are available in _Maven Central_ this process will not be necessary
>
<br />

### EDC

<br />

Execute the following command  from `EDC` root folder.

```bash
./gradlew publishToMavenLocal -P "skip.signing"
```

<br />

### Identity Hub

<br />

Execute the following command from `Identity Hub` root folder:


```bash
./gradlew publishToMavenLocal -P "skip.signing"
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

First, we need to build the `EDC Connector` (which also includes the `Identity Hub`) and `Registration Service` runtimes.  As we are running `MVD` locally, we include  `useFsVault` to indicate that the system will be using the local file-system based key vault.

From the `MVD` root folder, execute the following command:

```bash
./gradlew -DuseFsVault="true" :launcher:shadowJar
```

From the `Identity Hub` root folder, execute the following command:

```bash
./gradlew :client-cli:shadowJar
```

Copy Identity Hub client-cli jar which should be located at `<Identity-Hub-root-folder>/client-cli/build/libs/identity-hub-cli.jar` into MVD at folder location `<MVD-root-folder>/system-tests/resources/cli-tools`. If required then update copied jar file name to `identity-hub-cli.jar`, full path will be `<MVD-root-folder>/system-tests/resources/cli-tools/identity-hub-cli.jar`. This `identity-hub-cli.jar` will be used by `cli-tools` docker container to execute the `Identity Hub` commands.

From the `Registration Service` root folder, execute the following command:

```bash
./gradlew :launcher:shadowJar
```

Copy registration service client-cli jar which should be located at `<Registration-Service-root-folder>/client-cli/build/libs/registration-service-cli.jar` into MVD at folder location `<MVD-root-folder>/system-tests/resources/cli-tools`. If required then update copied jar file name to `registration-service-cli.jar`, full path will be `<MVD-root-folder>/system-tests/resources/cli-tools/registration-service-cli.jar`. This `registration-service-cli.jar` will be used by `cli-tools` docker container to execute the `Registration Service` commands.

From the `MVD` root folder execute the following commands to set the `Registration Launcher` path environment variable and start `MVD` using the `docker-compose.yml` file.

> Note that the value of the path is relative to the build system and is only here for example. You **will need to change this**

```bash
export REGISTRATION_SERVICE_LAUNCHER_PATH=/path/to/your/RegistrationService/launcher
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
- A `HTTP Nginx Server` (to serve DID Documents)
- An `Azurite` blob storage service

_Note, the `Newman` docker container will automatically stop after seeding initial data from postman scripts and `cli-tools` container will also automatically stop after registering participants._

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

> [Storage Explorer](https://azure.microsoft.com/features/storage-explorer/) can be used to connect to the `Azurite` storage container on `127.0.0.1:10000` port and under the `consumereuassets` account, the transferred blob can be viewed.

### Local Test Resources

The following test resources are provided in order to run `MVD` locally. `system-tests/docker-compose.yml` uses it to start `MVD`.

<br>

---

<br>

Each `EDC Connector` has its own set of Private and Public keys in PEM and Java KeyStore formats, e.g. `system-tests/resources/vault/provider`. These were generated using the following commands:

```bash
# generate a private key
openssl ecparam -name prime256v1 -genkey -noout -out private-key.pem
# generate corresponding public key
openssl ec -in private-key.pem -pubout -out public-key.pem
# create a self-signed certificate
openssl req -new -x509 -key private-key.pem -out cert.pem -days 360
```

Generated keys are imported to keystores e.g. `system-tests/resources/vault/provider/provider-keystore.jks`. Each keystore has password `test123`.

> [KeyStore Explorer](https://keystore-explorer.org/) can be used to manage keystores from UI.

`MVD` local instances use a file-system based vault and its keys are managed using a java properties file e.g.`system-tests/resources/vault/provider/provider-vault.properties`.

> ! IMPORTANT !
>
> *File System Vault is __NOT__ a secure vault and thus should only be used for testing purposes*

<br>

---

<br>

Web DIDs are available under `system-tests/resources/webdid` folder. The `publicKeyJwk` section of each `did.json` was generated by converting the corresponding public key to JWK format, for example provider connector public key was converted to JWK using following command:

```bash
docker run -i danedmunds/pem-to-jwk:1.2.1 --public --pretty < system-tests/resources/vault/provider/public-key.pem > key.public.jwk
```

<br>

---

<br>

### Debugging MVD locally

Follow the instructions in the previous sections to run an MVD with a consumer and provider locally using docker-compose.

Once running, you can use a Java debugger to connect to the consumer (port 5006) and provider (port 5005) instances. If you are using IntelliJ you can use the provided "EDC consumer" or "EDC provider" [runtime configurations](../.run) to remote debug the connector instances.

### Issuing requests manually with Postman

A [postman collection](../deployment/data/MVD.postman_collection.json) can be used to issue requests to an MVD instance of your choice. You will need to adapt the environment variables accordingly to match your target MVD instance.
