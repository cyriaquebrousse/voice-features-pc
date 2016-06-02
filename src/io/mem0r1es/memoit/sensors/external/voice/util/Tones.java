package io.mem0r1es.memoit.sensors.external.voice.util;

import java.io.IOException;
import java.util.ArrayList;

import static io.mem0r1es.memoit.sensors.external.voice.util.FileUtils.foreachNonEmptyLine;

/**
 * @author Cyriaque Brousse
 */
public class Tones extends ArrayList<Float> {

  private static final String TONES_FILE_PATH = "tones.csv";
  private static final float REFERENCE = 196.00f;
  private final int REF_INDEX;

  public Tones() {

    try {
      foreachNonEmptyLine(TONES_FILE_PATH, line -> {
        final String[] elems = line.split("\t");
        if (elems.length != 2) {
          System.err.println(line + " was invalid");
        }

        add(Float.parseFloat(elems[1]));
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    REF_INDEX = indexOf(REFERENCE);

  }

  public int semitonesDifferenceCount(float f) {
    final float closest = findClosest(f);
    final int index = indexOf(closest);
    return REF_INDEX - index;
  }

  private float findClosest(float f) {
    for (int i = 0; i < size() - 1; ++i) {
      final float prev = get(i);
      final float next = get(i + 1);
      if (prev < f && f < next) {
        return Math.abs(prev - f) < Math.abs(next - f) ? prev : next;
      }
    }

    return get(size() - 1);
  }

}
