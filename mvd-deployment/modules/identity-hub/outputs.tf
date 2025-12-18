#
#  Copyright (c) 2023 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#

output "identity-hub-node-ip" {
  value = kubernetes_service.ih-service.spec.0.cluster_ip
}


output "ports" {
  value = var.ports
}

output "ih-superuser-apikey" {
  value = var.ih_superuser_apikey
}

output "credentials" {
  value = {
    path    = var.credentials-dir
    content = fileset(var.credentials-dir, "*-credential.json")
  }
}

output "sts-token-url" {
  value = "http://${kubernetes_service.ih-service.metadata.0.name}:${var.ports.sts-api}${var.sts-token-path}"
}