terraform {
  required_providers {
    kubernetes = {
      source = "hashicorp/kubernetes"
    }
    helm = {
      source = "hashicorp/helm"
    }
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = var.monitoring_namespace
  }
}

# Enables the built-in Grafana deployment with default settings
# Provides visualization capabilities for metrics
# enables more flexible service discovery across the cluster
# Configures the service monitor for kube-state-metrics

resource "helm_release" "prometheus" {
  name       = "prometheus"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name
  depends_on = [kubernetes_namespace.monitoring]

  values = [
<<-YAML
  grafana:
    enabled: true
  prometheus:
    prometheusSpec:
      serviceMonitorSelectorNilUsesHelmValues: false
  kube-state-metrics:
    serviceMonitor:
      labels:
        release: prometheus
YAML
  ]

}

# Deploy Loki as central logging solution in the monitoring namespace
# Enable persistent storage with 10GB capacity
# Configure Promtail to collect logs from all pods in the mvd namespace
# Enable Grafana integration with automatic dashboard and datasource provisioning
# Disable authentication for easier initial setup

resource "helm_release" "loki" {
  name       = "loki"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name
  depends_on = [kubernetes_namespace.monitoring]

  values = [
    <<-YAML
  loki:
    auth_enabled: false
    persistence:
      enabled: true
      size: 10Gi
    storage:
      type: filesystem
  promtail:
    enabled: true
    config:
      snippets:
        extraScrapeConfigs: |
          - job_name: mvd-services
            kubernetes_sd_configs:
              - role: pod
                namespaces:
                  names: ["mvd"]
            pipeline_stages:
              - docker: {}
  grafana:
    enabled: true
    sidecar:
      dashboards:
        enabled: true
      datasources:
        enabled: true
YAML
  ]
}

resource "kubernetes_service" "prometheus_grafana" {
  depends_on = [helm_release.prometheus]
  metadata {
    name      = "prometheus-grafana"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }
  spec {
    selector = {
      "app.kubernetes.io/name" = "grafana"
    }
    port {
      port        = 80
      target_port = 3000
      protocol    = "TCP"
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_service" "prometheus_operator" {
  depends_on = [helm_release.prometheus]
  metadata {
    name      = "prometheus-operator"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }
  spec {
    selector = {
      "app.kubernetes.io/name" = "prometheus-operator"
    }
    port {
      port        = 9090
      target_port = 9090
      protocol    = "TCP"
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_service" "alertmanager" {
  depends_on = [helm_release.prometheus]
  metadata {
    name      = "alertmanager"
    namespace = kubernetes_namespace.monitoring.metadata[0].name
  }
  spec {
    selector = {
      "app.kubernetes.io/name" = "alertmanager"
    }
    port {
      port        = 9093
      protocol    = "TCP"
      target_port = 9093
    }
    type = "ClusterIP"
  }
}

