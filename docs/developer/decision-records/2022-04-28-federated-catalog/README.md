# Federated Catalog

## Decision

Use files in an Azure file share to seed federated catalog of all participant EDC instances.

The deployment pipeline for each participant creates a file in common folder in a file share, with a prefix corresponding to each unique deployment. For example, when deploying participants `company1` and `company2`, the files could be named:
- `280-company1.json`
- `280-company2.json`

Here `280-` is an arbitrary prefix for one dataspace (in continuous delivery, one deployment).

Each file contains a serialized EDC `FederatedCacheNode` object, for example:

```json
{
  "name": "company1",
  "supportedProtocols": [
    "ids-multipart"
  ],
  "url": "http://280-company1-edc-mvd.northeurope.azurecontainer.io:8282"
}

```

The deployed EDC instance is configured with a custom extension that reads all files for a given prefix (here `280-`) and populates the`FederatedCacheNodeDirectory`.

## Rationale

URLs of deployed EDC instances are only known after deployment in parallel Terraform runs.

Until [the federated catalog node list can be dynamically updated](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1230), this requires restarting the connectors after all catalog files have been stored.
