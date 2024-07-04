# Minimum Viable Dataspace Demo

## Introduction

The Decentralized Claims Protocols define a secure way how to participants in a dataspace can exchange and present
credential information. In particular, the [DCP specification](https://github.com/eclipse-tractusx/identity-trust)
defines the _Presentation Flow_, which is the process of requesting and verifying Verifiable Credentials.

So in order to get the most out of this demo, a basic understanding of VerifiableCredentials, VerifiablePresentations,
Decentralized Identifiers (DID) and
cryptography is necessary. These concepts will not be explained here further.

The Presentation Flow was adopted in the Eclipse Dataspace Components project and is currently implemented in modules
pertaining to the [Connector](https://github.com/eclipse-edc/connector) as well as
the [IdentityHub](https://github.com/eclipse-edc/IdentityHub).

## Purpose of this Demo

This demo is to demonstrate how two dataspace participants can perform a credential exchange prior to a DSP message
exchange, for example requesting a catalog or negotiating a contract.

It must be stated in the strongest terms that this is **NOT** a production grade installation, nor should any
production-grade developments be based on it. [Shortcuts](#other-caveats-shortcuts-and-workarounds) were taken, and
assumptions
were made that are potentially invalid in other scenarios.

It merely is a playground for developers wanting to kick the tires in the EDC and DCP space, and its purpose is to
demonstrate how DCP works to an
otherwise unassuming audience.

## The Scenario

_In this example, we will see how two companies can share data through federated catalogs
using [Management Domains](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/management-domains/management-domains.md)._

### Participants

There are two ficticious companies, called "Provider Corp" and "Consumer Corp". "Consumer Corp" wants to
consume information from "Provider Corp". Furthermore, Provider Corp has two departments "Q&A" and "Manufacturing".
Both are independent and host their own EDC connectors dubbed "provider-qna" and "provider-manufacturing". Provider Corp
also hosts a catalog server, plus an IdentityHub that is shared between the catalog server, ""provider-qna""
and "provider-manufacturing". This is necessary, because those three share the same `participantId`, and thus, the same
set of credentials.
A catalog server is a stripped-down EDC runtime, that only contains modules for servicing catalog requests.

Consumer Corp has a connector plus its own IdentityHub.

### Data setup

"provider-qna" and "provider-manufacturing" both have two data assets each, named `"asset-1"` and `"asset-2"` but
neither
"provider-qna" nor "provider-manufacturing" expose their
catalog endpoint directly to the internet. Instead, the catalog server (Bob) provides
a catalog that contains special assets (think: pointers) to both "provider-qna"'s and "provider-manufacturing"'s
connectors. We call this a "root catalog", and the pointers are called "catalog assets". This means, that by resolving
the root catalog, and by following the links in it, "Consumer Corp" can resolve the actual asset from "provider-qna" and
"provider-manufacturing".

### Access control

Both assets of "provider-qna" and "provider-manufacturing" have some access restrictions on them:

- `asset-1`: requires a membership credential to view and a PCF Use Case credential to negotiate a contract
- `asset-2`: requires a membership credential to view and a Sustainability Use Case credential to negotiate a contract

These requirements are formulated as EDC policies. In addition, it is a dataspace rule that
the `MembershipCredential` must be presented in _every_ request.

Furthermore, all connectors are in possession of the `MembershipCredential` as well as a `PcfCredential`. _Neither has
the `SustainabilityCredential`_! That means that no contract for `asset-2` can be negotiated!
For the purposes of this demo the VerifiableCredentials are pre-created and are seeded to the participants' credential
storage (no issuance).

If Alice wants to view the consolidated catalog (containing Teds and Carols assets), then negotiate a contract for an
asset, and then transfer the asset, she needs to present several credentials:

- catalog request: present `MembershipCredential`
- contract negotiation: `MembershipCredential` and `PcfCredential` or `SustainabilityCredential`, respectively
- transfer process: `MembershipCredential`

## Running the demo (inside IntelliJ)

There are several run configurations for IntelliJ in the `.run/` folder. One each for Bob's and Alice's connector
runtimes and
IdentityHub runtimes and one named "dataspace". The latter brings up all other runtimes together.

However, with `did:web` documents there is a tightly coupled relation between DID and URL, so we can't easily re-use the
same DIDs.

DID documents are dynamically generated when "seeding" the data, in particular when creating the `ParticipantContext`s
in IdentityHub. This is automatically being done by a script `seed.sh`.

After executing the `dataspace` run config in Intellij, be sure to **execute the `seed.sh` script after all the
runtimes have started**. Omitting to do so will leave the dataspace in an uninitialized state and cause all
connector-to-connector communication to fail.

All REST requests made from the script are available in
the [Postman collection](./deployment/postman/MVD.postman_collection.json).

## Running the Demo (Kubernetes)

For this section a basic understanding of Kubernetes, Docker, Gradle and Terraform is required. It is assumed that the
following tools are installed and readily available:

- Docker
- KinD (other cluster engines may work as well - not tested!)
- Terraform
- JDK 17+
- Git
- a POSIX compliant shell
- Postman (to comfortably execute REST requests)
- `newman` (to run Postman collections from the command line)

All commands are executed from the **repository's root folder** unless stated otherwise via `cd` commands.

### 1. Build the runtime images

```shell
./gradlew build
./gradlew dockerize -Ppersistence=true
```

this builds the runtime images and creates the following docker images: `connector:latest`, `catalog-server:latest`
and `identity-hub:latest` in the local docker image cache. Note the `-Ppersistence` flag which puts the HashiCorp Vault
module and PostgreSQL persistence modules on the classpath. These obviously require additional configuration, which is
handled by the Terraform scripts.

Next, we bring up and configure the Kubernetes Cluster

```shell
# Create the cluster
kind create cluster -n dcp-demo --config deployment/kind.config.yaml

# Load docker images into KinD
kind load docker-image connector:latest identity-hub:latest catalog-server:latest -n dcp-demo

# Deploy an NGINX ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Wait for the ingress controller to become available
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s

# Deploy the dataspace, type 'yes' when prompted
cd deployment
terraform init
terraform apply
```

Once Terraform has completed the deployment, type `kubectl get pods` and verify the output:

```shell
❯ kubectl get pods --namespace mvd
NAME                                       READY   STATUS    RESTARTS   AGE
alice-connector-54d588b4ff-r468g           1/1     Running   0             11m
alice-identityhub-97485f4df-gqklj          1/1     Running   0             11m
alice-postgres-7654f9c97c-nf4d9            1/1     Running   0             11m
consumer-vault-0                           1/1     Running   0             11m
bob-identityhub-647bfb49cd-fpd77           1/1     Running   0             11m
bob-postgres-7f74c86dbc-vhw9n              1/1     Running   0             11m
"provider-manufacturing"-connector-6c9cc8f9cb-t9rtg           1/1     Running   0             11m
provider-catalog-server-64c6488cf5-tdtbj   1/1     Running   0             11m
provider-vault-0                           1/1     Running   0             11m
"provider-qna"-connector-694457685b-82xth             1/1     Running   0             11m
```

"Alice" has a connector, and IdentityHub, a postgres database and a vault to store secrets.
"Bob" has a catalog server, a ""provider-qna"" and a ""provider-manufacturing"" connector plus an IdentityHub, a
postgres database and a vault.

Remote Debugging is possible, but Kubernetes port-forwards are necessary.

Once all the deployments are up-and-running, the seed script needs to be executed which should produce command line
output similar to this:

```shell
❯ ./seed-k8s.sh


Seed data to "provider-qna" and "provider-manufacturing"
(node:545000) [DEP0040] DeprecationWarning: The `punycode` module is deprecated. Please use a userland alternative instead.
(Use `node --trace-deprecation ...` to show where the warning was created)
(node:545154) [DEP0040] DeprecationWarning: The `punycode` module is deprecated. Please use a userland alternative instead.
(Use `node --trace-deprecation ...` to show where the warning was created)


Create linked assets on the Catalog Server
(node:545270) [DEP0040] DeprecationWarning: The `punycode` module is deprecated. Please use a userland alternative instead.
(Use `node --trace-deprecation ...` to show where the warning was created)


Create consumer participant
ZGlkOndlYjphbGljZS1pZGVudGl0eWh1YiUzQTcwODM6YWxpY2U=.KPHR02XRnn+uT7vrpCIu8jJUADTBHKrterGq0PZTRJgzbzvgCXINcMWM3WBraG0aV/NxdJdl3RH3cqgyt+b5Lg==

Create provider participant
ZGlkOndlYjpib2ItaWRlbnRpdHlodWIlM0E3MDgzOmJvYg==.wBgVb44W6oi3lXlmeYsH6Xt3FAVO1g295W734jivUo5PKop6fpFsdXO4vC9D4I0WvqfB/cARJ+FVjjyFSIewew==%
```

_Note the `node` warnings are harmless and can be ignored_

## Executing REST requests using Postman

This demos comes with a Postman collection located in `deployment/assets/postman`. Be aware that the collection has
different sets of variables in different environments, "MVD local development" and "MVD K8S".

The collection itself is pretty self-explanatory, it allows you to request a catalog, perform a contract negotiation and
execute a data transfer. NB [this caveat](#9-data-transfers-will-get-terminated) though.

## Other caveats, shortcuts and workarounds

Once again, this is a **DEMO**, does not any provide guarantee w.r.t. operational readiness and comes with a few
significant workarounds and shortcuts. These are:

### 1. In-memory stores in local deployment

When running the MVD from IntelliJ, the runtimes exclusively use in-memory stores and in-memory vaults. We opted for
this to avoid having to either provide a docker-compose scenario, or to put users through a significant amount of setup
and configuration.

The Kubernetes deployment uses both persistent storage (PostgreSQL) and secure vaults (Hashicorp Vault).

### 2. Policy Extractor

Constructing scope strings out of Policy constraints cannot be done in a generic way, because it is highly dependent on
the constraint expression syntax, which is specific to the dataspace. In this demo, there are two extractors:

- `DefaultScopeExtractor`: adds the `org.eclipse.edc.vc.type:MembershipCredential:read` scope to every request. That
  means that the MembershipCredential credential must always be presented.
- `FrameworkCredentialScopeExtractor`: adds the correct scope for a "Use case credential" (
  check [here](https://github.com/eclipse-tractusx/ssi-docu/blob/main/docs/architecture/policy_credentialtype_scope.md)
  for details), whenever a `FrameworkCredential.XYZ` is required by a policy.

For the sake of simplicity, it is only possible to assert the presence of a particular credential. Introspecting the
schema of the credentials' subjects is not yet implemented.

### 3. Scope-to-criterion transformer

This is similar to the [policy extractor](#5-policy-extractor), as it deals with the reverse mapping from a scope string
onto a `Criterion`. On the IdentityHub, when the VP request is received, we need to be able to query the database based
on the scope string that was received. This is currently a very Catena-X-specific solution, as it needs to distinguish
between "normal" credentials, and "use case" credentials.

### 4. DID resolution

#### 4.1 `did:web` for participants

Every participant hosts their DIDs in their IdentityHubs, which means, that the HTTP-URL that the DID maps to must be
accessible
for all other participants. For access to pods from outside the cluster we are using an ingress controller, but the
other pods on a cluster cannot access it, due to missing DNS entries. That
means, that the DID cannot use the _ingress URL_, but must use the _service's_ URL. A service in turn is not accessible
from
outside the cluster, so DIDs are only resolvable from _inside_ the cluster. Unfortunately, there is no way around this,
unless we put DIDs on a publicly resolvable DNS or similar.

#### 4.2 `did:example` for the dataspace credential issuer

The "dataspace issuer" does not exist as participant yet, so instead of deploying a fake IdentityHub, we opted for
introducing the (fictitious) `"did:example"` method, for which there is a custom-built DID resolver in the code.

### 5. Data Transfers will get `TERMINATED`

This demo does _not_ yet include infrastructure to perform any sort of actual data transfer, all transfers will
ultimately
fail. That is expected, and will be improved down the line. The important part is, that it will successfully get
through all the identity-related steps.

### 6. No issuance (yet)

All credentials are pre-generated manually because the DCP Issuance Flow is not implemented yet. Credentials are put
into
the stores by an extension called `IdentityHubExtension.java` and are **different** for local deployments and Kubernetes
deployments.
