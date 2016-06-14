package io.mem0r1es.memoit.sensors.external.voice;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchProcessor;
import io.mem0r1es.memoit.sensors.external.voice.util.BlockStat;
import io.mem0r1es.memoit.sensors.external.voice.util.EmotionMapper;
import io.mem0r1es.memoit.sensors.external.voice.util.PitchShiftingExample;
import io.mem0r1es.memoit.sensors.external.voice.util.Tones;

import static be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm.FFT_YIN;

/**
 * @author Cyriaque Brousse
 */
public class CallRecorder {

  public static final int SAMPLING_RATE = 44_100;
  public static final int FRAME_SIZE = 512;
  public static final int FRAME_OVERLAP = 0;

  public static final EmotionMapper EMOTION_MAPPER = new EmotionMapper();
  public static final Tones TONES = new Tones();

  public static void main(String[] args) {
    Preconditions.checkArgument(args.length >= 1, "not enough arguments");

    for (String arg : args) {
      System.err.println("### Processing file " + arg + " ###");

      final File source = new File(arg);
      final File target = new File(arg + "_temp.wav");
      final int[] firstLastSpokenFrames = new int[2];

      try {
        shiftPitch(source, target);
        newPreprocessor(source, firstLastSpokenFrames).run();
        newDispatcher(target, firstLastSpokenFrames).run();
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }

      target.deleteOnExit();

      System.out.println();
    }
  }

  private static void shiftPitch(File file, File target) throws Exception {
    // get average pitch of source file
    final List<Float> pitches = new ArrayList<>();
    newPitchDetector(file, pitches).run();
    final float pitchMean = (float) BlockStat.Builder.getStat(pitches).getMean();

    // differences of semitones to the reference
    final int numSemitones = TONES.semitonesDifferenceCount(pitchMean);

    // map the source file to the target file
    PitchShiftingExample.startCli(file.getAbsolutePath(), target.getAbsolutePath(), numSemitones * 100);
  }

  private static AudioDispatcher newPitchDetector(File file, List<Float> pitches) throws Exception {
    final AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(file, FRAME_SIZE, FRAME_OVERLAP);
    dispatcher.addAudioProcessor(
       new PitchProcessor(FFT_YIN,
          SAMPLING_RATE,
          FRAME_SIZE,
          (result, event) -> {
            if (result.isPitched()) {
              float pitch = result.getPitch();
              if (pitch >= 40 && pitch <= 640) {
                pitches.add(pitch);
              }
            }
          }
       )
    );
    return dispatcher;
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
