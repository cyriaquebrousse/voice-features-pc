package io.mem0r1es.memoit.sensors.external.voice.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author Cyriaque Brousse
 */
public final class FileUtils {

  private FileUtils() { }

  /**
   * @param fileName
   *            path of the file to read from
   * @param considerEmptyLines
   *            {@code false} if the user wants to skip empty lines in the
   *            iteration (in the sense of {@link String#trim()}),
   *            {@code true} otherwise (default case)
   * @param action
   *            the action to execute on each line
   * @throws IOException
   */
  public static void foreachLine(String fileName, boolean considerEmptyLines,
                                 Consumer<String> action) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line;
    while ((line = reader.readLine()) != null) {

      if (!considerEmptyLines) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
      }

      action.accept(line);
    }
    reader.close();
  }

  /**
   * Shorthand for {@code foreachLine(fileName, false, action)}.
   *
   * @see #foreachLine(String, boolean, Consumer)
   */
  public static void foreachNonEmptyLine(String fileName, Consumer<String> action)
     throws IOException {
    foreachLine(fileName, false, action);
  }

}
