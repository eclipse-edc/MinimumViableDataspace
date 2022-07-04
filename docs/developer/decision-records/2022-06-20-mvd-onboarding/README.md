# Participants onboarding in MVD

This document outlines the infrastructure steps for the scenario of onboarding participants to the Minimum Viable Dataspace (MVD).

## Precondition - GAIA-X membership

To join the MVD, each participant must have a Verifiable Credential (VC) signed by GAIA-X Authority which proves GAIA-X membership.

At present the MVD deployment pipeline does not interact with the real GAIA-X Authority. A simulated GAIA-X Authority DID document is deployed within MVD 
for demonstration purposes. 

## Onboarding process - GAIA-X membership verification

During the onboarding process, the GAIA-X membership claim is verified by the Dataspace Authority.
The onboarding flow is presented in the [distributed authorisation sub-flow document](../2022-06-16-distributed-authorization/README.md).
Actors in the onboarding scenario:

- _Participant A_ is a putative participant that wants to join the MVD.
- _Participant B_ is the Registration Service with its `did:web` document.
- _Authority_ is the simulated GAIA-X Authority.

## Infrastructure

To enable above precondition and onboarding scenario in MVD, the following steps need to be implemented in the MVD infrastructure: 

1. The MVD deployment workflow will generate the private and [public keys](#public-key-infrastructure) for GAIA-X Authority.
2. The MVD deployment workflow will deploy the GAIA-X Authority DID document containing the public key. For the simplicity the GAIA-X Authority DID document 
   is deployed together with other Dataspace components, which means that each deployed Dataspace will have its own GAIA-X Authority instance. In a
   real scenario the GAIA-X Authority is an external component and can communicate with multiple Dataspaces.
3. The GAIA-X private key will be used in the participant deployment workflow to generate GAIA-X membership Verifiable Credentials. Additionally,
   it will be made available to be used locally to onboard additional participants.
4. A CLI client for IdentityHub will be implemented. It will be used in the MVD deployment workflow to populate the participant's Identity Hub with GAIA-X 
   membership Verifiable Credentials.
5. A CLI client for the Registration Service will be implemented. It will be used in the MVD deployment workflow to start the participant onboarding process. The CLI
   client can also be used locally to onboard additional participants. The CLI client needs access to the participant's DID private key.
6. The Registration Service will be configured at deployment with the environment variable pointing to GAIA-X Authority DID URL. This is required because each deployment will have a different URL for the GAIA-X Authority.
7. The Registration Service will be configured with a policy, that requires a GAIA-X membership Verifiable Credential issued by the GAIA-X 
   Authority to verify participant's GAIA-X membership.

### Public key infrastructure

Public keys will be represented in [JSON Web Key](https://www.rfc-editor.org/rfc/rfc7517#section-4) format. 

Example JWK:
```json
{
   "kty": "EC",
   "crv": "secp256k1",
   "x": "wSwuib0Eyfsvdb_RPpQQLlFoHsQE4TSlFdncLePp6Zg",
   "y": "uxjZNS8HQ9krKn5ZXpjBtSAAj9FQXSDlHlEMR2YA7Hs"
}
```
The _kty_ (Key Type) parameter in JWK is mandatory and defines the cryptographic algorithm family used with a key. 
In MVD we are going to use _EC_ - [Elliptic Curve](https://en.wikipedia.org/wiki/Elliptic-curve_cryptography) key.

The rationale for choosing this public key format is to reuse existing [EDC libraries to manage the decentralized identity](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/tree/main/extensions/iam/decentralized-identity).
