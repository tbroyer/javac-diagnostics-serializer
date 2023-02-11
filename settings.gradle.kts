pluginManagement {
    repositories {
        // gradlePluginPortal redirects to JCenter which isn't reliable,
        // prefer Central to JCenter (for the same dependencies)
        // cf. https://github.com/gradle/gradle/issues/15406
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

rootProject.name = "javac-diagnostics-serializer"

include(":javac-plugin", ":gradle-plugin")

dependencyResolutionManagement {
    repositories {
        mavenLocal {
            mavenContent {
                snapshotsOnly()
                onlyForConfigurations("compileClasspath")
                includeVersionByRegex("""com\.google\.errorprone""", """error_prone_.*""", "HEAD-SNAPSHOT")
            }
        }
        mavenCentral()
    }
}
