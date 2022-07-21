## System tests

The test copy a file from provider to consumer blob storage account.

### Building MVD project

MVD dependencies are Eclipse DataSpaceConnector(EDC) and Registration Service. Both of these dependencies are not published to any central artifactory yet so in local
development we have to use locally published dependencies, once this is done MVD can be build using

```bash
./gradlew build -x test
```

#### Publish EDC Registration Service and Identity Hub to local Maven

Checkout [Eclipse DataSpaceConnector repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

Publish EDC libraries to local Maven artifactory by executing gradle command `./gradlew publishToMavenLocal -Pskip.signing` from EDC root
folder. On windows powershell command `./gradlew publishToMavenLocal -P"skip.signing"` can be used.

Checkout [Registration Service repository](https://github.com/eclipse-dataspaceconnector/RegistrationService). 

Publish Registration Service libraries to local Maven artifactory by executing gradle command `./gradlew publishToMavenLocal` from Registration Service root folder.

Checkout [Identity Hub repository](https://github.com/eclipse-dataspaceconnector/IdentityHub).

Publish Identity Hub libraries to local Maven artifactory by executing gradle command `./gradlew publishToMavenLocal` from Identity Hub root folder.

### Running test locally

MVD System tests can be executed locally against a local MVD instance. MVD runs three EDC Connectors and one Registration Service.

First please make sure that you are able to build MVD locally as described in [Building MVD project](#building-mvd-project) section.

- We need to build EDC Connector launcher and Registration Service launcher.
- Go to MVD root folder. And execute

    ```bash
    ./gradlew -DuseFsVault="true" :launcher:shadowJar
    ```

- Go to Registration service root folder. And execute

    ```bash
    ./gradlew :launcher:shadowJar
    ```

- Start MVD using docker-compose.yml file.

    ```bash
    export REGISTRATION_SERVICE_LAUNCHER_PATH=Registration service launcher path e.g. `/home/user/RegistrationService/launcher`.
    docker-compose -f system-tests/docker-compose.yml up --build
    ```

  for windows powershell

    ```powershell
    $Env:REGISTRATION_SERVICE_LAUNCHER_PATH = "Registration service launcher path e.g. /home/user/RegistrationService/launcher"
    docker-compose -f system-tests/docker-compose.yml up --build
    ```

- This will start three EDC Connectors, one Registration Service, one HTTP Nginx Server to serve DIDs, Azurite blob storage service and also will seed initial required data using a [postman collection](../deployment/data/MVD.postman_collection.json).

- `newman` docker container will automatically stop after seeding initial data from postman scripts.

- EDC Connectors needs to be registered using Registration Service CLI client jar. After publishing RegistrationService locally the client jar should be available under `RegistrationService-Root/client-cli/build/libs` folder.

    ```bash
    export REGISTRATION_SERVICE_CLI_JAR_PATH=registration service client jar path
    ./system-tests/resources/register-participants.sh
    ```

  for windows powershell

    ```powershell
    $Env:REGISTRATION_SERVICE_CLI_JAR_PATH = "registration service client jar path"
    # Execute command by copying it from shell script ./system-tests/resources/register-participants.sh or use git-bash to execute this shell script.
    ```

- Run MVD system tests, and for that environment variable `TEST_ENVIRONMENT` must be set to `local` to enable local blob transfer test.

    ```bash
    export TEST_ENVIRONMENT=local
    ./gradlew :system-tests:test
    ```

  for windows powershell

    ```powershell
    $Env:TEST_ENVIRONMENT = "local"
    ./gradlew :system-tests:test
    ```

- [Storage Explorer](https://azure.microsoft.com/features/storage-explorer/) can be used to connect to Azurite storage container on `127.0.0.1:10000` port and under `consumereuassets` account transferred blob can be viewed.

#### Local test resources

Following test resources are provided in order to run MVD locally.`system-tests/docker-compose.yml` usages it to start MVD.

- Each EDC Connector has its own set of Private and Public keys with java keystore e.g. `system-tests/resources/provider`.

    ```bash
    # generate a private key
    openssl ecparam -name prime256v1 -genkey -noout -out private-key.pem
    # generate corresponding public key
    openssl ec -in private-key.pem -pubout -out public-key.pem
    # create a self-signed certificate
    openssl req -new -x509 -key private-key.pem -out cert.pem -days 360
    ```

- Generated keys are imported to keystores e.g. `system-tests/resources/provider/provider-keystore.jks`. Each keystore has password `test123`.[KeyStore Explorer](https://keystore-explorer.org/) can be used to manage keystores from UI.

- MVD local instance usage EDC File System Vault and its keys are managed using a java properties file e.g.`system-tests/resources/provider/provider-vault.properties`. *File System Vault is NOT a secure vault and thus should only be used for testing purposes*

- Web DIDs are available under `system-tests/resources/webdid` folder. The `publicKeyJwk` section of each `did.json` was generated by converting the corresponding public key to JWK format, for example provider connector public key was converted to JWK using following command:

    ```bash
    docker run -i danedmunds/pem-to-jwk:1.2.1 --public --pretty < system-tests/resources/provider/public-key.pem > key.public.jwk
    ```

### Debugging MVD locally

Follow the instructions in the previous sections to run an MVD with a consumer and provider locally using docker-compose.

Once running, you can use a Java debugger to connect to the consumer (port 5006) and provider (port 5005) instances. If you are using IntelliJ you can use the provided "EDC consumer" or "EDC provider" [runtime configurations](../.run) to remote debug the connector instances.

### Issuing requests manually with Postman

A [postman collection](../deployment/data/MVD.postman_collection.json) can be used to issue requests to an MVD instance of your choice. You will need to adapt the environment variables accordingly to match your target MVD instance.
