/*
 * Copyright © 2023 Thomas Broyer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ltgt.gradle.javacdiagnosticsserializer

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JavacDiagnosticsSerializerPluginTest {
    val gradleVersion = System.getProperty("test.gradle-version", GradleVersion.current().version)!!
    val version = System.getProperty("version")!!
    val testRepositories = System.getProperty("testRepositories")!!.splitToSequence(File.pathSeparator).joinToString("\n") {
        """
            maven { url = uri("${File(it).toURI().toASCIIString()}") }
        """.trimIndent()
    }

    @Rule @JvmField
    val testProjectDir = TemporaryFolder()

    lateinit var settingsFile: File
    lateinit var buildFile: File

    @Before
    fun setUp() {
        settingsFile = testProjectDir.newFile("settings.gradle.kts").apply {
            writeText(
                """
                dependencyResolutionManagement {
                    repositories {
                        ${testRepositories.prependIndent("    ".repeat(2))}
                    }
                }
                """.trimIndent(),
            )
        }
        buildFile = testProjectDir.newFile("build.gradle.kts").apply {
            writeText(
                """
                import net.ltgt.gradle.javacdiagnosticsserializer.*

                plugins {
                    `java-library`
                    id("net.ltgt.javac-diagnostics-serializer")
                }
                dependencies {
                    javacDiagnosticsSerializer("net.ltgt.javacdiagnosticsserializer:javac-diagnostics-serializer:$version")
                }
                tasks.withType<JavaCompile> {
                    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun configurable() {
        buildFile.appendText(
            """

            tasks.compileJava {
                diagnosticsOutputFile.set(project.layout.buildDirectory.file("foo.txt"))
            }
            """.trimIndent(),
        )

        testProjectDir.newFolder("src/main/java/foo")
        testProjectDir.newFile("src/main/java/foo/Bar.java").writeText(
            """
            package foo;

            class Bar {}
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.root)
            .withGradleVersion(gradleVersion)
            .withArguments("compileJava")
            .build()

        assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(File(testProjectDir.root, "build/foo.txt").exists()).isTrue()
        assertThat(File(testProjectDir.root, "build/javac-diagnostics/main.txt").exists()).isFalse()
    }

    @Test
    fun success_noDiagnostics() {
        testProjectDir.newFolder("src/main/java/foo")
        testProjectDir.newFile("src/main/java/foo/Bar.java").writeText(
            """
            package foo;

            class Bar {}
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.root)
            .withGradleVersion(gradleVersion)
            .withArguments("compileJava")
            .build()

        assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val diagnosticsFile = File(testProjectDir.root, "build/javac-diagnostics/main.txt")
        assertThat(diagnosticsFile.exists()).isTrue()
        assertThat(diagnosticsFile.readText()).isEmpty()
    }

    @Test
    fun warning() {
        testProjectDir.newFolder("src/main/java/foo")
        testProjectDir.newFile("src/main/java/foo/Bar.java").writeText(
            """
            package foo;

            import java.util.Date;

            class Bar {
                Bar() {
                    new Date().getDate();
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.root)
            .withGradleVersion(gradleVersion)
            .withArguments("compileJava")
            .buildAndFail()

        assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).containsMatch("""foo[/\\]Bar\.java\b.*getDate\(\).* deprecated(?s:.*)new Date\(\).getDate\(\)""")
        val diagnosticsFile = File(testProjectDir.root, "build/javac-diagnostics/main.txt")
        assertThat(diagnosticsFile.exists()).isTrue()
        assertThat(diagnosticsFile.readText()).containsMatch("""foo[/\\]Bar\.java\b.*getDate\(\).* deprecated""")
    }

    @Test
    fun error() {
        testProjectDir.newFolder("src/main/java/foo")
        testProjectDir.newFile("src/main/java/foo/Bar.java").writeText(
            """
            package foo;

            class Bar {
                Bar() {}
            // unbalanced parentheses
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.root)
            .withGradleVersion(gradleVersion)
            .withArguments("compileJava")
            .buildAndFail()

        assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("reached end of file while parsing")
        val diagnosticsFile = File(testProjectDir.root, "build/javac-diagnostics/main.txt")
        assertThat(diagnosticsFile.exists()).isTrue()
        assertThat(diagnosticsFile.readText()).contains("reached end of file while parsing")
    }
}
