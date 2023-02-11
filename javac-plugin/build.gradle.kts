plugins {
    id("local.java-library")
    id("local.maven-publish")
}

group = "net.ltgt.javacdiagnosticsserializer"
base.archivesName.set("javac-diagnostics-serializer")

nullaway {
    annotatedPackages.add("net.ltgt.javacdiagnosticsserializer")
}

dependencies {
    errorprone(libs.bundles.errorprone)

    compileOnlyApi(libs.autoService.annotations)
    annotationProcessor(libs.autoService)

    compileOnly(libs.errorprone.checkApi)
}

val JPMS_ARGS = listOf(
    "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
)

tasks {
    withType<JavaCompile> {
        options.compilerArgs.addAll(
            JPMS_ARGS,
        )
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.truth)
                implementation(libs.compileTesting)
            }
            targets.configureEach {
                testTask {
                    jvmArgs(JPMS_ARGS)

                    // systemProperty doesn't support providers, so fake it with CommandLineArgumentProvider
                    // https://github.com/gradle/gradle/issues/9092
                    jvmArgumentProviders.add(TestJvmArgumentProvider(tasks.jar.flatMap { it.archiveFile }))
                }
            }
        }
    }
}

private class TestJvmArgumentProvider(
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    val jarFile: Provider<RegularFile>,
) : CommandLineArgumentProvider, Named {
    @Internal
    override fun getName() = "test.jar-filepath"

    override fun asArguments() = listOf("-Dtest.jar-filepath=${jarFile.get().asFile}")
}
