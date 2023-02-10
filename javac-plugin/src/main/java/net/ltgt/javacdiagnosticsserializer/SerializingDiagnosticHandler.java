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

import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public class SerializingDiagnosticHandler extends Log.DiagnosticHandler implements Closeable {
  private final BufferedWriter writer;
  private final Locale locale;

  public SerializingDiagnosticHandler(Log log, Path outputFile, Locale locale) throws IOException {
    this.writer =
        Files.newBufferedWriter(
            outputFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
    this.locale = locale;
    install(log);
  }

  @Override
  public void report(JCDiagnostic diag) {
    if (!shouldIgnore(diag)) {
      try {
        writer.write(
            diag.getSource().getName()
                + ":"
                + diag.getLineNumber()
                + ": "
                + diag.getKind()
                + ": "
                + diag.getMessage(locale));
        writer.newLine();
      } catch (IOException e) {
        // FIXME
        e.printStackTrace();
        throw new UncheckedIOException(e);
      }
    }
    prev.report(diag);
  }

  private boolean shouldIgnore(JCDiagnostic diag) {
    return diag.getCode().equals("compiler.err.warnings.and.werror");
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }
}
