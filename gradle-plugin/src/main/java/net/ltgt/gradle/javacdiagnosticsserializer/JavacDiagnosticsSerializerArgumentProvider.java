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

import java.util.Collections;
import org.gradle.api.Named;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.process.CommandLineArgumentProvider;

public class JavacDiagnosticsSerializerArgumentProvider
    implements CommandLineArgumentProvider, Named {
  private Provider<RegularFile> outputFile;

  public JavacDiagnosticsSerializerArgumentProvider(Provider<RegularFile> outputFile) {
    this.outputFile = outputFile;
  }

  @Internal
  @Override
  public String getName() {
    return "javacDiagnosticsSerializer";
  }

  @OutputFile
  @Optional
  public Provider<RegularFile> getOutputFile() {
    return outputFile;
  }

  @Override
  public Iterable<String> asArguments() {
    return outputFile
        .map(
            file ->
                Collections.singletonList(
                    "-Xplugin:JavacDiagnosticsSerializer " + file.getAsFile().getPath()))
        .getOrElse(Collections.emptyList());
  }
}
