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
package net.ltgt.gradle.javacdiagnosticsserializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

public class JavacDiagnosticsSerializerPlugin implements Plugin<Project> {

  private static final List<String> JPMS_ARGS =
      Collections.unmodifiableList(
          Arrays.asList(
              "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
              "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"));

  @Override
  public void apply(Project project) {
    Configuration javacDiagnosticsSerializerConfiguration =
        project
            .getConfigurations()
            .create(
                "javacDiagnosticsSerializer",
                configuration -> {
                  configuration.setDescription(
                      "Javac Diagnostics Serializer dependencies, will be extended by all source sets' annotationProcessor configurations");
                  configuration.setVisible(false);
                  configuration.setCanBeConsumed(false);
                  configuration.setCanBeResolved(false);
                });

    project
        .getTasks()
        .withType(JavaCompile.class)
        .configureEach(
            javaCompile -> {
              RegularFileProperty outputFile = project.getObjects().fileProperty();
              javaCompile.getExtensions().add("diagnosticsOutputFile", outputFile);
              javaCompile
                  .getOptions()
                  .getCompilerArgumentProviders()
                  .add(new JavacDiagnosticsSerializerArgumentProvider(outputFile));
              javaCompile.getOptions().getForkOptions().getJvmArgs().addAll(JPMS_ARGS);
              // Force forking for JDK 16+
              javaCompile.doFirst(
                  "javacDiagnosticsSerializer",
                  task -> {
                    if (javaCompile
                        .getJavaCompiler()
                        .map(javaCompiler -> javaCompiler.getMetadata().getLanguageVersion())
                        .getOrElse(JavaLanguageVersion.of(JavaVersion.current().getMajorVersion()))
                        .canCompileOrRun(16)) {
                      javaCompile.getOptions().setFork(true);
                    }
                  });
            });

    project
        .getPluginManager()
        .withPlugin(
            "org.gradle.java-base",
            ignored -> {
              project
                  .getExtensions()
                  .getByType(SourceSetContainer.class)
                  .configureEach(
                      sourceSet -> {
                        project
                            .getConfigurations()
                            .getByName(sourceSet.getAnnotationProcessorConfigurationName())
                            .extendsFrom(javacDiagnosticsSerializerConfiguration);
                        project
                            .getTasks()
                            .named(sourceSet.getCompileJavaTaskName(), JavaCompile.class)
                            .configure(
                                javaCompile -> {
                                  // XXX: use the ReportingExtension#getBaseDir?
                                  ((RegularFileProperty)
                                          javaCompile
                                              .getExtensions()
                                              .getByName("diagnosticsOutputFile"))
                                      .convention(
                                          project
                                              .getLayout()
                                              .getBuildDirectory()
                                              .file(
                                                  "javac-diagnostics/"
                                                      + sourceSet.getName()
                                                      + ".txt"));
                                });
                      });
            });
  }
}
