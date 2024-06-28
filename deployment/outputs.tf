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

output "ted-node-ip" {
  value = {
    connector    = module.provider-ted-connector.connector-node-ip
    identity-hub = module.provider-identityhub.identity-hub-node-ip
  }
}

output "carol-node-ip" {
  value = {
    connector    = module.provider-carol-connector.connector-node-ip
    identity-hub = module.provider-identityhub.identity-hub-node-ip
  }
}

output "alice-node-ip" {
  value = {
    connector    = module.alice-connector.connector-node-ip
    identity-hub = module.consumer-alice-identityhub.identity-hub-node-ip
  }
}

output "catalog-server-node-ip" {
  value = {
    connector    = module.provider-catalog-server.connector-node-ip
  }
}

output "identity-hub-management-api-key" {
  value = {
    ted-superuser   = module.provider-identityhub.ih-superuser-apikey
    carol-superuser   = module.provider-identityhub.ih-superuser-apikey
    alice-superuser = module.consumer-alice-identityhub.ih-superuser-apikey
  }
}

output "consumer-credentials"{
  value = module.consumer-alice-identityhub.credentials
}