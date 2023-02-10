plugins {
    id("local.java-library")
    `maven-publish`
}

// name must already be capitalized for computing task name below
val localPublication = publishing.publications.create<MavenPublication>("Local") {
    from(components["java"])
    afterEvaluate {
        artifactId = base.archivesName.get()
    }

    versionMapping {
        usage("java-api") {
            fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
            fromResolutionResult()
        }
    }

    pom {
        name.set(provider { "$groupId:$artifactId" })
        description.set(provider { project.description ?: name.get() })
        url.set("https://github.com/tbroyer/javac-diagnostics-serializer")
        developers {
            developer {
                name.set("Thomas Broyer")
                email.set("t.broyer@ltgt.net")
            }
        }
        scm {
            connection.set("https://github.com/tbroyer/javac-diagnostics-serializer.git")
            developerConnection.set("scm:git:ssh://github.com:tbroyer/javac-diagnostics-serializer.git")
            url.set("https://github.com/tbroyer/javac-diagnostics-serializer")
        }
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
    }
}

val localRepoDir = layout.buildDirectory.dir("local-maven-repo")

val localRepository = publishing.repositories.maven {
    name = "Local" // must already be capitalized for computing task name below
    url = uri(localRepoDir)
}

tasks {
    val cleanLocalRepository by registering(Delete::class) {
        delete(localRepoDir)
    }
    withType<PublishToMavenRepository>().configureEach {
        if (repository == localRepository) {
            onlyIf { publication == localPublication }
            dependsOn(cleanLocalRepository)
        }
    }
}

val localRepoElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "Shares local maven repository directory that contains the artifacts produced by the current project"
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("maven-repository"))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
    outgoing {
        artifact(localRepoDir) {
            builtBy(tasks.named("publish${localPublication.name}PublicationTo${localRepository.name}Repository"))
        }
    }
}
