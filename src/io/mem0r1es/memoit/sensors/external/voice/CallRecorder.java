package io.mem0r1es.memoit.sensors.external.voice;

import java.io.File;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

/**
 * @author Cyriaque Brousse
 */
public class CallRecorder {

  public static final int SAMPLING_RATE = 8_000;
  public static final int FRAME_SIZE = 512;
  public static final int FRAME_OVERLAP = 0;

  public static void main(String[] args) throws Exception {
    newDispatcher(new File("voice-samples/test.wav")).run();
  }

  /**
   * Factory method for dispatcher creation
   */
  private static AudioDispatcher newDispatcher(File file) throws Exception {
    final AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(file, FRAME_SIZE, FRAME_OVERLAP);
    dispatcher.addAudioProcessor(new BlockProcessor());
    return dispatcher;
  }

}
