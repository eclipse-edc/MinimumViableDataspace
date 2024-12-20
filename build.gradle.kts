/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin

plugins {
    // Apply the Java Library plugin
    `java-library`
    // Apply the Docker Remote API plugin
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    // Apply the Shadow plugin for creating fat/uber JARs
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

buildscript {
    dependencies {
        // Retrieve the EDC Gradle plugins version from the project properties
        val edcGradlePluginsVersion: String by project
        // Add the EDC build plugin classpath dependency
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:${edcGradlePluginsVersion}")
    }
}

// Retrieve the EDC Gradle plugins version from the project properties
val edcGradlePluginsVersion: String by project

allprojects {
    // Apply the EDC build plugin to all projects
    apply(plugin = "${group}.edc-build")

    // Configure the AutodocExtension for generating documentation
    configure<org.eclipse.edc.plugins.autodoc.AutodocExtension> {
        outputDirectory.set(project.layout.buildDirectory.asFile)
        processorVersion.set(edcGradlePluginsVersion)
    }

    // Configure the BuildExtension for generating Swagger documentation
    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        swagger {
            title.set("Identity HUB REST API")
            description = "Identity HUB REST APIs - merged by OpenApiMerger"
            outputFilename.set(project.name)
            outputDirectory.set(file("${rootProject.projectDir.path}/resources/openapi/yaml"))
        }
    }
}

subprojects {
    afterEvaluate {
        // Check if the Shadow plugin is applied and a Dockerfile exists
        if (project.plugins.hasPlugin("com.github.johnrengelman.shadow") &&
            file("${project.projectDir}/src/main/docker/Dockerfile").exists()
        ) {
            // Apply the Docker Remote API plugin to the subproject
            apply(plugin = "com.bmuschko.docker-remote-api")
            // Configure the "dockerize" task for building Docker images
            val dockerTask: DockerBuildImage = tasks.create("dockerize", DockerBuildImage::class) {
                val dockerContextDir = project.projectDir
                dockerFile.set(file("$dockerContextDir/src/main/docker/Dockerfile"))
                images.add("${project.name}:${project.version}")
                images.add("${project.name}:latest")
                // Specify platform with the -Dplatform flag if provided
                if (System.getProperty("platform") != null)
                    platform.set(System.getProperty("platform"))
                buildArgs.put("JAR", "build/libs/${project.name}.jar")
                inputDir.set(file(dockerContextDir))
            }
            // Ensure the "dockerize" task depends on the "shadowJar" task
            dockerTask.dependsOn(tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME))
        }
    }
}