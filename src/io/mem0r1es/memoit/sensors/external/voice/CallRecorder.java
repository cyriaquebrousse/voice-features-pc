package io.mem0r1es.memoit.sensors.external.voice;

import java.io.File;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import io.mem0r1es.memoit.sensors.external.voice.util.Preprocessor;

/**
 * @author Cyriaque Brousse
 */
public class CallRecorder {

  public static final int SAMPLING_RATE = 22_050;
  public static final int FRAME_SIZE = 512;
  public static final int FRAME_OVERLAP = 0;

  public static void main(String[] args) throws Exception {
    final File file = new File("voice-samples/test.wav");
    final int[] firstLastSpokenFrames = new int[2];
    newPreprocessor(file, firstLastSpokenFrames).run();
    newDispatcher(file, firstLastSpokenFrames).run();
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
    dispatcher.addAudioProcessor(new BlockProcessor(firstLastSpokenFrames));
    return dispatcher;
  }

}
