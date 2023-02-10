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

import com.google.auto.service.AutoService;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Locale;

@AutoService(Plugin.class)
public class JavacDiagnosticsSerializerPlugin implements Plugin {
  @Override
  public String getName() {
    return "JavacDiagnosticsSerializer";
  }

  @Override
  public void init(JavacTask task, String... args) {
    // TODO: parse arguments
    var outputFile = Paths.get(args[0]);

    var context = ((BasicJavacTask) task).getContext();
    var log = Log.instance(context);
    SerializingDiagnosticHandler dh;
    try {
      dh = new SerializingDiagnosticHandler(log, outputFile, context.get(Locale.class));
    } catch (IOException e) {
      // XXX: use log.error() ?
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
    task.addTaskListener(
        new TaskListener() {
          @Override
          public void finished(TaskEvent e) {
            if (e.getKind() == TaskEvent.Kind.COMPILATION) {
              log.popDiagnosticHandler(dh);
              try {
                dh.close();
              } catch (IOException ex) {
                // XXX: use log.error() ?
                ex.printStackTrace();
                throw new UncheckedIOException(ex);
              }
            }
          }
        });
  }
}
