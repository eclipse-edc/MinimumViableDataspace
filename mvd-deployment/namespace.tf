resource "kubernetes_namespace" "ns_consumer" {
  metadata {
    name = "consumer"
  }
}

resource "kubernetes_namespace" "ns_provider" {
  metadata {
    name = "provider"
  }
}