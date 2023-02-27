rootProject.name = "mvd"

pluginManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.0.1-SNAPSHOT")
            library("apache.commons.lang3", "org.apache.commons", "commons-lang3").version("3.12.0")
            library("gatling-highcharts", "io.gatling.highcharts", "gatling-charts-highcharts").version("3.7.5")
        }
        create("identityHub") {
            version("identityHub", "0.0.1-SNAPSHOT")
            library("spi-core", "org.eclipse.edc", "identity-hub-spi").versionRef("identityHub")
            library("core", "org.eclipse.edc", "identity-hub").versionRef("identityHub")
            library("core-client", "org.eclipse.edc", "identity-hub-client").versionRef("identityHub")
            library(
                "core-verifier", "org.eclipse.edc", "identity-hub-credentials-verifier"
            ).versionRef("identityHub")

            library("ext-api", "org.eclipse.edc", "identity-hub-api").versionRef("identityHub")
            library("ext-selfdescription-api", "org.eclipse.edc", "self-description-api").versionRef("identityHub")
            library(
                "ext-verifier-jwt", "org.eclipse.edc", "identity-hub-verifier-jwt"
            ).versionRef("identityHub")
            library(
                "ext-credentials-jwt", "org.eclipse.edc", "identity-hub-credentials-jwt"
            ).versionRef("identityHub")

        }
        create("registrationService") {
            version("registrationService", "0.0.1-SNAPSHOT")
            library("core", "org.eclipse.edc", "registration-service").versionRef("registrationService")
            library(
                "core-credential-service",
                "org.eclipse.edc",
                "registration-service-credential-service"
            ).versionRef("registrationService")
            library("core-client", "org.eclipse.edc", "registration-service-client").versionRef("registrationService")
            library("ext-api", "org.eclipse.edc", "registration-service-api").versionRef("registrationService")
        }

        create("fcc") {
            version("catalog", "0.0.1-SNAPSHOT")
            library("api", "org.eclipse.edc", "federated-catalog-api").versionRef("catalog")
            library("spi", "org.eclipse.edc", "federated-catalog-spi").versionRef("catalog")
            library("core", "org.eclipse.edc", "federated-catalog-core").versionRef("catalog")
        }

        create("edc") {
            version("edc", "0.0.1-SNAPSHOT")
            library("util", "org.eclipse.edc", "util").versionRef("edc")
            library("boot", "org.eclipse.edc", "boot").versionRef("edc")
            library("junit", "org.eclipse.edc", "junit").versionRef("edc")

            library("spi-policy-engine", "org.eclipse.edc", "policy-engine-spi").versionRef("edc")
            library("spi-contract", "org.eclipse.edc", "contract-spi").versionRef("edc")
            library("spi-ids", "org.eclipse.edc", "ids-spi").versionRef("edc")
            library("spi-dpf-selector", "org.eclipse.edc", "data-plane-selector-spi").versionRef("edc")

            library("core-connector", "org.eclipse.edc", "connector-core").versionRef("edc")
            library("core-controlplane", "org.eclipse.edc", "control-plane-core").versionRef("edc")
            library("core-micrometer", "org.eclipse.edc", "micrometer-core").versionRef("edc")

            library("policy-engine", "org.eclipse.edc", "policy-engine").versionRef("edc")
            library("policy-evaluator", "org.eclipse.edc", "policy-evaluator").versionRef("edc")

            library("identity-did-core", "org.eclipse.edc", "identity-did-core").versionRef("edc")
            library("identity-did-service", "org.eclipse.edc", "identity-did-service").versionRef("edc")
            library("identity-did-web", "org.eclipse.edc", "identity-did-web").versionRef("edc")


            library("ext-auth-tokenBased", "org.eclipse.edc", "auth-tokenbased").versionRef("edc")
            library("api-dataManagement", "org.eclipse.edc", "data-management-api").versionRef("edc")
            library("api-observability", "org.eclipse.edc", "api-observability").versionRef("edc")
            library("micrometer-jetty", "org.eclipse.edc", "jetty-micrometer").versionRef("edc")
            library("micrometer-jersey", "org.eclipse.edc", "jersey-micrometer").versionRef("edc")

            library("config-filesystem", "org.eclipse.edc", "configuration-filesystem").versionRef("edc")
            library("vault-filesystem", "org.eclipse.edc", "vault-filesystem").versionRef("edc")
            library("vault-azure", "org.eclipse.edc", "vault-azure").versionRef("edc")
            library("provision-blob", "org.eclipse.edc", "provision-blob").versionRef("edc")
            library("ids", "org.eclipse.edc", "ids").versionRef("edc")


            library("ext-azure-blob-core", "org.eclipse.edc", "azure-blob-core").versionRef("edc")
            library("ext-jdklogger", "org.eclipse.edc", "monitor-jdk-logger").versionRef("edc")
            library("ext-http", "org.eclipse.edc", "http").versionRef("edc")


            // DPF modules
            library("dpf-selector-spi", "org.eclipse.edc", "data-plane-selector-spi").versionRef("edc")
            library("dpf-selector-core", "org.eclipse.edc", "data-plane-selector-core").versionRef("edc")
            library("dpf-framework", "org.eclipse.edc", "data-plane-framework").versionRef("edc")
            library("dpf-transfer-client", "org.eclipse.edc", "data-plane-transfer-client").versionRef("edc")
            library("dpf-selector-client", "org.eclipse.edc", "data-plane-selector-client").versionRef("edc")
            library("dpf-storage-azure", "org.eclipse.edc", "data-plane-azure-storage").versionRef("edc")

            bundle(
                "identity",
                listOf(
                    "identity-did-core",
                    "identity-did-service",
                    "identity-did-web"
                )
            )

            bundle(
                "dpf",
                listOf(
                    "dpf-transfer-client",
                    "dpf-selector-client",
                    "dpf-selector-spi",
                    "dpf-selector-core",
                    "dpf-framework",
                    "dpf-storage-azure"
                )
            )
        }
    }
}

include(":launchers:connector")
include(":launchers:registrationservice")
include(":system-tests")
include(":extensions:refresh-catalog")
include(":extensions:policies")
