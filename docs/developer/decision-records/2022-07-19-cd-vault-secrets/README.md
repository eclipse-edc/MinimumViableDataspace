# Deployment of Azure Key Vault secrets

## Decision

Terraform deploys Azure Key Vault instances and configures Role-Based Acess Control to allow the CD pipeline service principal to write secrets.

Secrets are written to Azure Key Vault in CD pipeline actions, and not within Terraform actions, and not within the Terraform deployment. The CD pipeline obtains a fresh Azure AD access token after Terraform deployment, to ensure role assignments are propagated.

## Rationale

Propagation of role assignments [can take up to 30 minutes](https://docs.microsoft.com/en-us/azure/role-based-access-control/troubleshooting#role-assignment-changes-are-not-being-detected). This can be circumvented by issuing a new Azure AD access token, which is not possible within a Terraform deployment.
