# Minimum Viable Dataspace

The Minimum Viable Dataspace (MVD) is a sample implementation of a dataspace that leverages the [Eclipse Dataspace Connector (EDC)](https://github.com/eclipse-dataspaceconnector/dataspaceconnector). The main purpose is to demonstrate the capabilities of the EDC, make dataspace concepts tangible based on a specific implementation, and to serve as a starting point to implement a custom dataspace.

The MVD allows developers and decision makers to gauge the current progress of the EDC and its capabilities to satisfy the functionality of a fully operational dataspace.

As a fully decentralized dataspace is hard to imagine, the MVD also serves the purpose of demonstrating how decentralization can be practically implemented.

## MVD Documentation

Developer documentation can be found under [docs/developer](docs/developer/), where the main concepts and decisions are captured as [decision records](docs/developer/decision-records/).

## Create Dataspace Deployment

To be able to deploy your own dataspace instances, you first need to [fork the MVD repository and set up your environment](docs/developer/continuous_deployment.md).

Once your environment is set up, follow these steps to create a new dataspace instance:

- Go to your MVD fork in GitHub.
- Select the tab called `Actions`.
- Select the workflow called `Deploy`.
- Provide your own resources name prefix. Please, use at most 3 characters, composed of lower case letters and numbers.
  This name prefix guarantees the resources name's uniqueness and avoids resource name conflicts.
  Note down the used prefix.
- Click on `Run workflow` to trigger the deployment.

## Destroy Dataspace Deployment

Follow these steps to delete a dataspace instance and free up the corresponding resources:

- Go to your MVD fork in GitHub.
- Select the tab called `Actions`
- Select the workflow called `Destroy`
- Click on `Run workflow`
- Provide the resources prefix that you used when you deployed your DataSpace.
- Click on `Run workflow` to trigger to destroy your MinimumViableDataspace DataSpace.

## Local development setup

Please follow the instructions in [this document](system-tests/README.md) to setup a local MVD environment for development purposes.

## Contributing

See [how to contribute](CONTRIBUTING.md).