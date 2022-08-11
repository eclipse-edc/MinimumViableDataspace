plugins {
    java
    `java-library`
    jacoco
}

allprojects {
    apply(plugin = "java")
    if (System.getenv("JACOCO") == "true") {
        apply(plugin = "jacoco")
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
        }
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
    }

    if (System.getenv("JACOCO") == "true") {
        apply(plugin = "jacoco")
        tasks.test {
            finalizedBy(tasks.jacocoTestReport)
        }
        tasks.jacocoTestReport {
            reports {
                // Generate XML report for codecov.io
                xml.required.set(true)
            }
        }
    }
}
