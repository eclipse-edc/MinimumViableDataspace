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

output "connector-node-ip" {
  value = kubernetes_service.controlplane-service.spec.0.cluster_ip
}


output "database-name" {
  value = var.database
}

output "ports" {
  value = var.ports
}

output "audience-mapping" {
  value = {
    #    dspAudience  = "http://${local.connector-cluster-ip}:${var.ports.protocol}/api/dsp"
    dcpAudience = var.participantId
  }
}
