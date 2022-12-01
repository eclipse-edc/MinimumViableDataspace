plugins {
    java
    `java-library`
}

val downloadArtifact: Configuration by configurations.creating {
    isTransitive = false
}


val identityHubVersion: String by project
val registrationServiceVersion: String by project
val edcGroup: String by project
val annotationProcessorVersion: String by project
val javaVersion: String by project
val metaModelVersion: String by project
val actualVersion: String = project.findProperty("version") as String


dependencies {
    downloadArtifact("org.eclipse.edc:identity-hub-cli:${identityHubVersion}:all")
    downloadArtifact("org.eclipse.edc:registration-service-cli:${registrationServiceVersion}:all")
}

// task that downloads the RegSrv CLI and IH CLI
val getJars by tasks.registering(Copy::class) {
    outputs.upToDateWhen { false } //always download

    from(downloadArtifact)
        // strip away the version string
        .rename { s ->
            s.replace("-${identityHubVersion}", "")
                .replace("-${registrationServiceVersion}", "")
                .replace("-all", "")
        }
    into(layout.projectDirectory.dir("system-tests/resources/cli-tools"))
}

// run the download jars task after the "jar" task
tasks {
    jar {
        finalizedBy(getJars)
    }
}


allprojects {

    apply(plugin = "${edcGroup}.edc-build")


    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        processorVersion.set(annotationProcessorVersion)
        outputDirectory.set(project.buildDir)
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        versions {
            // override default dependency versions here
            projectVersion.set(actualVersion)
            metaModel.set(metaModelVersion)
        }
    }

    tasks.register("printClasspath") {
        doLast {
            println(sourceSets["main"].runtimeClasspath.asPath)
        }
    }
}

buildscript {
    dependencies {
        val edcGradlePluginsVersion: String by project
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:${edcGradlePluginsVersion}")
    }
}
