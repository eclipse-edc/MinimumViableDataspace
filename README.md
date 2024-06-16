# Identity And Trust Protocols Demo

## Introduction

The Identity And Trust Protocols define a secure way how to participants in a dataspace can exchange and present
credential information. In particular, the [IATP specification](https://github.com/eclipse-tractusx/identity-trust)
defines the "Presentation Flow", which is the process of requesting and verifying Verifiable Credentials.

A basic understanding of VerifiableCredentials, VerifiablePresentations, Decentralized Identifiers (DID) and
cryptography is assumed and will not be explained further.

The Presentation Flow was adopted in the Eclipse Dataspace Components project and is currently implemented in modules
pertaining to the [Connector](https://github.com/eclipse-edc/connector) as well as
the [IdentityHub](https://github.com/eclipse-edc/IdentityHub).

## Purpose of this Demo

This demo is to demonstrate how two dataspace participants can perform a credential exchange prior to a DSP message
exchange, for example requesting a catalog or negotiating a contract.

It must be stated in the strongest terms that this is **NOT** a production grade installation, nor should any
production-grade developments be based on it. [Shortcuts](#current-caveats-shortcuts-and-workarounds) were taken, and
assumptions
were made that are potentially invalid in other scenarios.

It merely is a playground for new developments in the IATP space, and its purpose is to demonstrate how IATP works to an
otherwise unassuming audience.

## The Scenario

There are two connectors dubbed "Alice" and "Bob", where "Alice" will take on the role of data consumer, and "Bob" will
be the data provider.

Bob, our provider, has two data assets:

- `asset-1`: requires a membership credential to view and a PCF Use Case credential to negotiate a contract
- `asset-2`: requires a membership credential to view and a Sustainability Use Case credential to negotiate a contract

These requirements are formulated in the form of EDC Policies. In addition, it is a dataspace rule that
the `MembershipCredential` must be presented in _every_ request.

Furthermore, both Bob and Alice are in possession of a `MembershipCredential` as well as a `PcfCredential`. _Neither has
the `SustainabilityCredential`_! That means that no contract for `asset-2` can be negotiated!
For the purposes of this demo the VerifiableCredentials are pre-created and are seeded to the participants' credential
storage (no issuance). Both Bob and Alice host an EDC connector instance and an IdentityHub instance each and deploy
them to a
Kubernetes cluster.

Bob wants to view Alice's catalog, then negotiate a contract for an asset, and then transfer the asset. For this he
needs to present several credentials:

- catalog request: present `MembershipCredential`
- contract negotiation: `MembershipCredential` and `PcfCredential` or `SustainabilityCredential`, respectively
- transfer process: `MembershipCredential`

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
- newman (to run Postman collections from the command line)

All commands are executed from the **repository's root folder** unless stated otherwise via `cd` commands.

### 1. Build the runtime images

```shell
cd runtimes
./gradlew build
./gradlew dockerize -PuseHashicorp=true
```

this builds the runtime images and creates the following docker images: `connector:latest` and `identity-hub:latest` in
the local docker image cache. Note the `-PuseHashicorp` puts the HashiCorp Vault module on the classpath.

Next, we bring up and configure the Kubernetes Cluster

```shell
# Create the cluster
kind create cluster -n iatp-demo --config deployment/kind.config.yaml

# Load docker images into KinD
kind load docker-image connector:latest identity-hub:latest -n iatp-demo

# Deploy an NGINX ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Wait for the ingress controller to become available
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s

# Deploy the dataspace, type 'yes' when prompted
terraform -chdir=deployment init
terraform -chdir=deployment apply
```

Once Terraform has completed the deployment, type `kubectl get pods` and verify the output:

```shell
> kubectl get pods --namespace mvd
NAME                                 READY   STATUS    RESTARTS   AGE
alice-connector-6d56797f54-bf44q     1/1     Running   0          14m
alice-identityhub-7664f549f6-bj6v6   1/1     Running   0          14m
alice-vault-0                        1/1     Running   0          14m
bob-connector-658755df69-v24sm       1/1     Running   0          14m
bob-identityhub-8cff855bf-9f7h4      1/1     Running   0          14m
bob-vault-0                          1/1     Running   0          14m
seed-alice-zthgc                     0/1     Completed 0          14m
seed-bob-jccpq                       0/1     Completed 0          14m
```

Every participant has two pods, one for the connector runtime, one for the IdentityHub runtime.
Assets, policies and contract definitions are already seeded to the connectors.

Remote Debugging is possible, but Kubernetes port-forwards are necessary. The following debug ports are exposed:

- 1044 on the connector runtime
- 1045 on the identity hub runtime

Note that both application data and IdentityHub data gets seeded automatically with
a [Kubernetes Job](./deployment/modules/connector/seed.tf), so there is nothing to do. If for some reason you need to
re-seed the data, e.g. after a connector pod crashes, you can use the  `seed-k8s.sh`.

## Running the demo (inside IntelliJ)

There are 5 run configurations for IntelliJ in the `.run/` folder. One each for Bob's and Alice's connector runtimes and
IdentityHub runtimes and one named "dataspace", that brings up all other runtimes together.

However, with `did:web` documents there is a tightly coupled relation between DID and URL, so we can't easily re-use
the same DIDs.

A separate set of DIDs, DID documents and VerifiableCredentials is required when running locally. As generating them,
and implementing a switch for local or clustered deployment would be a significant effort it is **currently missing**.

Another possibility would be to put DIDs on some internet-accessible resource such as a CDN or a static webserver, which
I forewent for the sake of self-contained-ness.

Running the dataspace from within IntelliJ is still useful though for testing APIs, debugging of one runtime, etc.

After executing the `dataspace` run config in Intellij, please be sure to execute the `seed.sh` script after all the
runtimes have started. Omitting to do so will cause all connector-to-connector communication to fail. Note that in the
Kubernetes deployment this is **not** necessary, because seeding is done automatically.

## Executing REST requests using Postman

This demos comes with a Postman collection located in `deployment/assets/postman`. Be aware that the collection is
pre-configured to work with the demo running in Kubernetes - in order to get it to work with the IntelliJ-based variant,
you'll need to change the `HOST` and `IH_HOST` variables!

The collection itself is pretty self-explanatory, it allows you to request a catalog, perform a contract negotiation and
execute a data transfer. NB [this caveat](#9-data-transfers-will-get-terminated) though.

## Other caveats, shortcuts and workarounds

Once again, this is a **DEMO**, does not any provide guarantee w.r.t. operational readiness and comes with a few
significant workarounds and shortcuts. These are:

### 2. In-memory stores all-around

For the sake of simplicity, all data retention is purely done in memory. Using Postgres would not have contributed any
significant value, it would have made configuration a bit more complex.

### 4. Proof-of-possession: only a warning is issued

This demo is inspired heavily by the Catena-X use case, which has a unique situation where the stable ID (= BPN) cannot
be used to resolve key material (= DID). The consequence of that is, that there MUST be a way to resolve one from the
other (which is required by
the [IATP spec](https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/identity.protocol.base.md#3-identities-and-identifiers)).
Such as registry does not currently exist, so when verifying that `siToken.sub == siToken.access_token.sub` we would be
comparing a BPN to a DID which would naturally fail.

### 5. Policy Extractor

Constructing scope strings out of Policy constraints cannot be done in a generic way, because it is highly dependent on
the constraint expression syntax, which is specific to the dataspace. In this demo, there are two extractors:

- `DefaultScopeExtractor`: adds the `org.eclipse.edc.vc.type:MembershipCredential:read` scope to every request, since as
  per Catena-X rules, that credential must always be presented.
- `FrameworkCredentialScopeExtractor`: adds the correct scope for a "Use case credential" (
  check [here](https://github.com/eclipse-tractusx/ssi-docu/blob/main/docs/architecture/policy_credentialtype_scope.md)
  for details), whenever a `FrameworkCredential.XYZ` is required by a policy.

For the sake of simplicity, it is only possible to assert the presence of a particular credential. Introspecting the
schema of the credentials' subjects is not yet implemented.

### 6. Scope-to-criterion transformer

This is similar to the [policy extractor](#5-policy-extractor), as it deals with the reverse mapping from a scope string
onto a `Criterion`. On the IdentityHub, when the VP request is received, we need to be able to query the database based
on the scope string that was received. This is currently a very Catena-X-specific solution, as it needs to distinguish
between "normal" credentials, and "use case" credentials.

### 7. DID resolution

#### 7.1 `did:web` for participants

Every participant hosts their DIDs on their own, which means, that the HTTP-URL that the DID maps to must be accessible
for all other participants. For external access we're using an ingress, which pods on a cluster cannot access. That
means, that the DID cannot be the ingress URL, but must be the _service_ url. A service in turn is not accessible from
outside the cluster. That means, DIDs are only resolvable from _inside_ the cluster, which is only a minor issue, and it
will go away once DIDs are hosted on a CDN or a web server.

#### 7.2 `did:example` for the dataspace credential issuer

The "dataspace issuer" does not exist as participant yet, so instead of deploying a fake IdentityHub, I opted for
introducing the `"did:example"` method, for which there is then a custom-built DID resolver.

### 8. The AudienceMapper

In EDC, the current implementation always sets the `aud` claim of an incoming token to the DSP callback URL of the
counter-party. This is not only wrong on a conceptual level, it is also wrong on an implementation level here, because
the audience of a token must be the identity of the recipient, which the DSP Url is arguably not. This is currently
being addressed in ongoing development effort in EDC. For now, we introduced the notion of an `AudienceMapper`, which
maps DSP Url -> ID (in this case: the BPN).

### 9. Data Transfers will get `TERMINATED`

This demo does *not* include infrastructure to perform any sort of actual data transfer, all transfers will ultimately
fail. That is expected, and could be improved down the line. The important part is, that it will successfully get
through all the identity-related steps.

### 10. No issuance

All credentials are pre-generated manually because the issuance flow is not implemented yet. Credentials are put into
the stores by an extension called `IdentityHubExtension.java` and are **different** for local deployments and Kubernetes
deployments.