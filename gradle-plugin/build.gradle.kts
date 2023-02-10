plugins {
    id("local.java-library")
    `kotlin-dsl`
}

nullaway {
    annotatedPackages.add("net.ltgt.gradle.javacdiagnosticsserializer")
}

val localMavenRepositories by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    // Same attributes as in local.maven-publish convention plugin
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("maven-repository"))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}
dependencies {
    localMavenRepositories(project(":javac-plugin"))
}

tasks {
    compileJava {
        options.release.set(8)
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.truth)
            }
            targets.configureEach {
                testTask {
                    systemProperty("version", rootProject.version.toString())
                    // systemProperty doesn't support providers, so fake it with CommandLineArgumentProvider
                    // https://github.com/gradle/gradle/issues/9092
                    jvmArgumentProviders.add(TestJvmArgumentProvider(localMavenRepositories))
                }
            }
        }
    }
}

private class TestJvmArgumentProvider(
    private val testRepositories: FileCollection,
) : CommandLineArgumentProvider, Named {
    @Internal
    override fun getName() = "testRepositories"

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getTestRepositories() = testRepositories.asFileTree.matching {
        exclude("**/maven-metadata.*")
    }

    override fun asArguments() = listOf("-DtestRepositories=${testRepositories.joinToString(File.pathSeparator) { project.relativePath(it) }}")
}

gradlePlugin {
    plugins {
        create("plugin") {
            id = "net.ltgt.javac-diagnostics-serializer"
            implementationClass = "net.ltgt.gradle.javacdiagnosticsserializer.JavacDiagnosticsSerializerPlugin"
        }
    }
}

spotless {
    kotlin {
        ktlint(libs.versions.ktlint.get())
        licenseHeaderFile(rootProject.file("LICENSE.header"))
    }
}
