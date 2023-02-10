plugins {
    id("local.base")
    `java-library`
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
}

dependencies {
    errorprone(project.the<VersionCatalogsExtension>().named("libs").findBundle("errorprone").orElseThrow())
}

tasks {
    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Werror", "-Xlint:all,-processing"))
    }
    javadoc {
        (options as CoreJavadocOptions).addBooleanOption("Xdoclint:all,-missing", true)
        (options as CoreJavadocOptions).addBooleanOption("html5", true)
    }
}

project.findProperty("test.java-toolchain")?.also { testJavaToolchain ->
    tasks.withType<Test>().configureEach {
        javaLauncher.set(
            project.javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(testJavaToolchain.toString()))
            },
        )
    }
}

spotless {
    java {
        googleJavaFormat(project.the<VersionCatalogsExtension>().named("libs").findVersion("googleJavaFormat").orElseThrow().requiredVersion)
        licenseHeaderFile(rootProject.file("LICENSE.header"))
    }
}
