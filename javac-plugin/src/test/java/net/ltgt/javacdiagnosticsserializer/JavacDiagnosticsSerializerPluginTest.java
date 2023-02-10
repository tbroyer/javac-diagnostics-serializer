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
package net.ltgt.javacdiagnosticsserializer;

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JavacDiagnosticsSerializerPluginTest {
  @Rule public final TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void foo() throws IOException {
    File outputFile = new File(tempDir.getRoot(), "test.txt");
    Compilation compilation =
        Compiler.javac()
            .withAnnotationProcessorPath(
                Collections.singletonList(new File(System.getProperty("test.jar-filepath"))))
            .withClasspath(Collections.emptyList())
            .withOptions("-Xlint:deprecation", "-Xplugin:JavacDiagnosticsSerializer " + outputFile)
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.Bar",
                    // language=Java
                    "package foo;\n"
                        + "\n"
                        + "import java.util.Date;\n"
                        + "\n"
                        + "class Bar {\n"
                        + "  Bar() {\n"
                        + "    new Date().getDate();\n"
                        + "  }\n"
                        + "}\n"));
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(1);

    assertThat(outputFile.exists()).isTrue();
    String outputContent = Files.readString(outputFile.toPath());
    for (Diagnostic<? extends JavaFileObject> diagnostic : compilation.diagnostics()) {
      assertThat(outputContent)
          .contains(
              diagnostic.getSource().getName()
                  + ":"
                  + diagnostic.getLineNumber()
                  + ": "
                  + diagnostic.getKind()
                  + ": "
                  + diagnostic.getMessage(Locale.getDefault()));
    }
  }
}
