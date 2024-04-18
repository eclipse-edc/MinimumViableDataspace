resource "helm_release" "vault" {
  name      = "${var.humanReadableName}-vault"
  namespace = var.namespace

  force_update      = true
  dependency_update = true
  reuse_values      = true
  cleanup_on_fail   = true
  replace           = true

  repository = "https://helm.releases.hashicorp.com"
  chart      = "vault"

  set {
    name  = "server.dev.devRootToken"
    value = var.vault-token
  }
  set {
    name  = "server.dev.enabled"
    value = true
  }

  set {
    name  = "injector.enabled"
    value = false
  }

  set {
    name  = "hashicorp.token"
    value = var.vault-token
  }

  values = [
    file("${path.module}/vault-values.yaml"),
    yamlencode({
      "server" : {
        "postStart" : [
          "sh",
          "-c",
          join(" && ", [
            "sleep 5",
            "/bin/vault kv put secret/${var.aliases.sts-private-key} content=\"${tls_private_key.ecdsa.private_key_pem}\"",
            "/bin/vault kv put secret/${local.public-key-alias} content=\"${tls_private_key.ecdsa.public_key_pem}\""
          ])
        ]
      }
    }),
  ]
}

# ECDSA key with P256 elliptic curve
resource "tls_private_key" "ecdsa" {
  algorithm   = "ECDSA"
  ecdsa_curve = "P256"
}