package io.mem0r1es.memoit.sensors.external.voice.util;

import java.io.IOException;
import java.util.HashMap;

import static io.mem0r1es.memoit.sensors.external.voice.util.FileUtils.foreachNonEmptyLine;

/**
 * Maps file_name -> coarse_emotion -> binary_emotion
 *
 * @author Cyriaque Brousse
 */
public class EmotionMapper extends HashMap<String, Pair<String, String>> {

  private static final String MAPPING_PATH = "emotion_mapping.csv";

  public EmotionMapper() {

    try {
      foreachNonEmptyLine(MAPPING_PATH, line -> {
        final String[] elems = line.split(",");
        if (elems.length != 4) {
          System.err.println(line + "was invalid");
        }

        // remove extension
        if (elems[0].contains(".")) {
          elems[0] = elems[0].substring(0, elems[0].lastIndexOf("."));
        }

        put(elems[0], Pair.create(elems[2], elems[3]));
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
