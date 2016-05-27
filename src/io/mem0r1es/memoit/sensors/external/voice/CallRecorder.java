package io.mem0r1es.memoit.sensors.external.voice;

import com.google.common.base.Preconditions;

import java.io.File;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import io.mem0r1es.memoit.sensors.external.voice.util.EmotionMapper;

/**
 * @author Cyriaque Brousse
 */
public class CallRecorder {

  public static final int SAMPLING_RATE = 22_050;
  public static final int FRAME_SIZE = 512;
  public static final int FRAME_OVERLAP = 0;

  public static final EmotionMapper EMOTION_MAPPER = new EmotionMapper();

  public static void main(String[] args) throws Exception {
    Preconditions.checkArgument(args.length >= 1, "not enough arguments");

    for (String arg : args) {
      System.err.println("### Processing file " + arg + " ###");

      final File file = new File(arg);
      final int[] firstLastSpokenFrames = new int[2];
      newPreprocessor(file, firstLastSpokenFrames).run();
      newDispatcher(file, firstLastSpokenFrames).run();

      System.out.println();
    }
  }

  private static AudioDispatcher newPreprocessor(File file, int[] output) throws Exception {
    final AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(file, FRAME_SIZE, FRAME_OVERLAP);
    dispatcher.addAudioProcessor(new Preprocessor(output));
    return dispatcher;
  }

  /**
   * Factory method for dispatcher creation
   */
  private static AudioDispatcher newDispatcher(File file, int[] firstLastSpokenFrames) throws Exception {
    final AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(file, FRAME_SIZE, FRAME_OVERLAP);
    dispatcher.addAudioProcessor(new BlockProcessor(firstLastSpokenFrames, file.getName()));
    return dispatcher;
  }

}
