package io.mem0r1es.memoit.sensors.external.voice.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for Java collections
 *
 * @author Cyriaque Brousse
 */
public final class CollectionsUtils {

  private CollectionsUtils() {
  }

  /**
   * <p>Unzips a list of pairs, yielding a list of all the left components of the pairs.<br>
   * For instance, applying this method to {@code List((a,b), (c,d), (e,f))} will yield
   * {@code List(a, c, e)}.</p>
   * <p/>
   * <p>The order of the elements of the returned list is the same as the order
   * returned by the parameter list iterator.</p>
   *
   * @param pairs the pairs to unzip
   * @return the list of all the left components of the pair list
   */
  public static <X, Y> List<X> unzipLeft(List<Pair<X, Y>> pairs) {
    final List<X> list = new ArrayList<>();

    for (Pair<X, Y> pair : pairs) {
      list.add(pair.first);
    }

    return list;
  }

  /**
   * <p>Unzips a list of pairs, yielding a list of all the right components of the pairs.<br>
   * For instance, applying this method to {@code List((a,b), (c,d), (e,f))} will yield
   * {@code List(b, d, f)}.</p>
   * <p/>
   * <p>The order of the elements of the returned list is the same as the order
   * returned by the parameter list iterator.</p>
   *
   * @param pairs the pairs to unzip
   * @return the list of all the right components of the pair list
   */
  public static <X, Y> List<Y> unzipRight(List<Pair<X, Y>> pairs) {
    final List<Y> list = new ArrayList<>();

    for (Pair<X, Y> pair : pairs) {
      list.add(pair.second);
    }

    return list;
  }
}
