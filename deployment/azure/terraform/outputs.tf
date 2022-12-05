output "participants" {
  value = var.participants
}

output "test" {
  value = ["val1", "val2"]
}

output "participant_data" {
  value = [
    {
      "participant" : var.participants[0]
      "connector_name" : module.participant1.connector_name
      "assets_account" : module.participant1.assets_storage_account
      "assets_key" : module.participant1.assets_storage_account_key
      "inbox_account" : module.participant1.inbox_storage_account
      "inbox_key" : module.participant1.inbox_storage_account_key
      "didhost" : module.participant1.participant_did_host
      "vault" : module.participant1.key_vault
      "api_key" : module.participant1.api_key
    },
    {
      "participant" : var.participants[1]
      "connector_name" : module.participant2.connector_name
      "assets_account" : module.participant2.assets_storage_account
      "assets_key" : module.participant2.assets_storage_account_key
      "inbox_account" : module.participant2.inbox_storage_account
      "inbox_key" : module.participant2.inbox_storage_account_key
      "didhost" : module.participant2.participant_did_host
      "vault" : module.participant2.key_vault
      "api_key" : module.participant2.api_key
    },
    {
      "participant" : var.participants[2]
      "connector_name" : module.participant3.connector_name
      "assets_account" : module.participant3.assets_storage_account
      "assets_key" : module.participant3.assets_storage_account_key
      "inbox_account" : module.participant3.inbox_storage_account
      "inbox_key" : module.participant3.inbox_storage_account_key
      "didhost" : module.participant3.participant_did_host
      "vault" : module.participant3.key_vault
      "api_key" : module.participant3.api_key
    },
  ]
  sensitive = true
}

output "dataspace_data" {
  value = {
    "vault_name" : module.dataspace.key_vault
    gaiax_did_host : module.dataspace.gaiax_did_host
    dataspace_did_host : module.dataspace.dataspace_did_host
  }
}