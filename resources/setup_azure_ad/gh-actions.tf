# Create an application for GH Actions
resource "azuread_application" "gh-actions-mvd" {
  display_name     = var.gh_actions_appname
  owners           = [data.azuread_client_config.current.object_id]
  sign_in_audience = "AzureADMyOrg"

  required_resource_access {
    resource_app_id = "00000003-0000-0000-c000-000000000000" # Microsoft Graph
    resource_access {
      id   = "e1fe6dd8-ba31-4d61-89e7-88639da4683d" # User.ReadWrite
      type = "Scope"
    }
  }
}

# Create a service principal
resource "azuread_service_principal" "gh-actions-mvd-sp" {
  application_id = azuread_application.gh-actions-mvd.application_id
}

# Create federated credentials for the main branch, and Pull requests
resource "azuread_application_federated_identity_credential" "gh-actions-fc" {
  application_object_id = azuread_application.gh-actions-mvd.object_id
  display_name          = var.application_fc_name
  description           = "Github Actions federated credential for your fork"
  audiences             = ["api://AzureADTokenExchange"]
  issuer                = "https://token.actions.githubusercontent.com"
  subject               = "repo:${var.github_repo}:ref:refs/heads/main"
}

resource "azuread_application_federated_identity_credential" "gh-actions-fc-pullrequest" {
  application_object_id = azuread_application.gh-actions-mvd.object_id
  display_name          = var.application_fc_pr_name
  description           = "Github Actions federated credential for your fork (Pullrequests)"
  audiences             = ["api://AzureADTokenExchange"]
  issuer                = "https://token.actions.githubusercontent.com"
  subject               = "repo:${var.github_repo}:pullrequest"
}

# grant GH Actions app "Owner" access to subscription
resource "azurerm_role_assignment" "owner" {
  scope                = data.azurerm_subscription.primary.id
  role_definition_name = "Owner"
  principal_id         = azuread_service_principal.gh-actions-mvd-sp.object_id

}
