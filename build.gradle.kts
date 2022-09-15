plugins {
    java
    `java-library`
    jacoco
    checkstyle
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")

    checkstyle {
        toolVersion = "9.0"
        configFile = rootProject.file("resources/checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
        maxErrors = 0 // does not tolerate errors
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
        }
        maven{
            url= uri("https://oss.sonatype.org/content/repositories/snapshots/")
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
