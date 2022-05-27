# MinimumViableDataspace

Temporary repository to get started with the MinimumViableDataspace

## Set up your own MinimumViableDataspace fork repository

Follow the instructions [to set up your own MinimumViableDataspace fork repository](docs/developer/continuous_deployment.md).

## Deploy your own DataSpace

To be able to deploy your own DataSpace, you first need to set up your own MinimumViableDataspace fork repository.

- Go to your Github repository MinimumViableDataspace fork.
- Select the tab called `Actions`.
- Select the workflow called `Deploy`.
- Provide your own resources name prefix. Please, use at most 3 characters, composed of lower case letters and numbers.
  This name prefix guarantees the resources name's uniqueness and avoids resource name conflicts.
  Note down the used prefix.
- Click on `Run workflow` to trigger the deployment.

## Destroy your deployed DataSpace

You might need to delete the DataSpace created previously.

- Go to your Github repository MinimumViableDataspace fork
- Select the tab called `Actions`
- Select the workflow called `Destroy`
- Click on `Run workflow`
- Provide the resources prefix that you used when you deployed your DataSpace.
- Click on `Run workflow` to trigger to destroy your MinimumViableDataspace DataSpace.

Note that the GitHub Artifacts by the Destroy pipeline are only retained for the [default time period](https://docs.github.com/actions/using-workflows/storing-workflow-data-as-artifacts#about-workflow-artifacts) which is currently 90 days. If destroying a deployment after that period, the pipeline will fail. In that case, manually delete the provisioned Azure resource groups.

## Local development setup

Please follow the instructions in [this document](system-tests/README.md) to setup a local MVD environment for development purposes.
