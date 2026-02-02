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

variable "participant" {
  type = string
}

variable "environment" {
  type = string
}

variable "postgres_endpoint" {
  type = string
  default = "kordat-dev-participants-database.cnsm066acc36.eu-west-1.rds.amazonaws.com"
}

variable "postgres_port" {
  type = number
  default = 5432
}

variable "postgres_admin_password" {
  type = string
}

variable "project" {
  type = string
  default = "kordat"
}

variable "useSVE" {
  type        = bool
  description = "If true, the -XX:UseSVE=0 switch (Scalable Vector Extensions) will be added to the JAVA_TOOL_OPTIONS. Can help on macOs on Apple Silicon processors"
  default     = false
}

# MVD component image versions are upgraded here (connector-deployment), not in the Kordat project.
# The Control Plane stores STS client secrets in Vault; upgrading its image may change Vault key behaviour.
variable "controlplane_image" {
  type        = string
  description = "Control Plane (connector) image. Upgrade tag here when releasing new MVD/EDC versions."
  default     = "150073872684.dkr.ecr.eu-west-1.amazonaws.com/kordat-dev-controlplane:193aad1e"
}

variable "dataplane_image" {
  type        = string
  description = "Data Plane image. Upgrade tag here when releasing new MVD/EDC versions."
  default     = "150073872684.dkr.ecr.eu-west-1.amazonaws.com/kordat-dev-dataplane:193aad1e"
}

variable "identityhub_image" {
  type        = string
  description = "Identity Hub image. Upgrade tag here when releasing new MVD/EDC versions."
  default     = "150073872684.dkr.ecr.eu-west-1.amazonaws.com/kordat-dev-identity-hub:193aad1e"
}
