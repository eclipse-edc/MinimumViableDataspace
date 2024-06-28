resource "kubernetes_service" "ih-service" {
  metadata {
    name      = var.humanReadableName
    namespace = var.namespace
  }
  spec {
    type = "NodePort"
    selector = {
      App = kubernetes_deployment.identityhub.spec.0.template.0.metadata[0].labels.App
    }
    # we need a stable IP, otherwise there will be a cycle with the issuer
    port {
      name = "resolution"
      port = var.ports.resolution-api
    }
    port {
      name = "debug"
      port = var.ports.ih-debug
    }
    port {
      name = "management"
      port = var.ports.ih-identity-api
    }
    port {
      name = "did"
      port = var.ports.ih-did
    }
  }
}