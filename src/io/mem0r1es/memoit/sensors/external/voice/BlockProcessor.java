package io.mem0r1es.memoit.sensors.external.voice;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.util.fft.HammingWindow;
import be.tarsos.dsp.util.fft.WindowFunction;
import io.mem0r1es.memoit.sensors.external.voice.util.BandPassFilter;
import io.mem0r1es.memoit.sensors.external.voice.util.BlockStat;
import io.mem0r1es.memoit.sensors.external.voice.util.Pair;

import static be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm.FFT_YIN;
import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.FRAME_SIZE;
import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.SAMPLING_RATE;

/**
 * Processes the audio input by dividing it into blocks of a fixed size.
 * Computes statistics on each block.
 *
 * @author Cyriaque Brousse
 */
public class BlockProcessor implements AudioProcessor {

  /* **********************************
              External utilities
     ********************************** */
  /** Detector for the pitch */
  private static final PitchDetector PITCH_DETECTOR = FFT_YIN.getDetector(SAMPLING_RATE, FRAME_SIZE);

  /** Window function. Gives more weight to the center of a frame. */
  private static final WindowFunction WINDOW_FUNCTION = new HammingWindow();

  /* **********************************
             Processing constants
     ********************************** */
  /** Frame block size. Assumes no overlap. */
  private final int BLOCK_SIZE;

  /** First non-silent frame. Determined in advance. */
  private final int FIRST_SPOKEN_FRAME;

  /** Last non-silent frame. Determined in advance. */
  private final int LAST_SPOKEN_FRAME;

  /** Lower bound of allowed pitch interval (Hz) */
  private static final int MIN_PITCH = 40;

  /** Upper bound of allowed pitch interval (Hz) */
  private static final int MAX_PITCH = 640;

  /** Pairs of frequency bands (band_center,band_width) to extract energy from (Hz) */
  public static final ImmutableList<Pair<Float, Float>> FREQUENCY_BANDS =
     initBands(0, 500, 1000, 2000, 3000, 4000, 5000, 7000, 9000);

  /* **********************************
                Accumulators
     ********************************** */
  /** Keeps track of the currently processed frame. Zero-indexed. */
  private long currentFrameNumber = 0L;

  /** Holds the statistics for each block of frames */
  private final List<BlockStat> stats = new ArrayList<>();

  /** Accumulator for values for one block of frames, on which stats are then computed */
  private BlockStat.Builder currentBlockStatBuilder;

  /* **********************************
                Constructor
     ********************************** */
  public BlockProcessor(int[] firstLastSpokenFrames) {
    Preconditions.checkArgument(firstLastSpokenFrames.length == 2,
       "first-last spoken frame buffer must be of length 2");

    FIRST_SPOKEN_FRAME = firstLastSpokenFrames[0];
    LAST_SPOKEN_FRAME  = firstLastSpokenFrames[1];
    BLOCK_SIZE = LAST_SPOKEN_FRAME - FIRST_SPOKEN_FRAME;

    this.currentBlockStatBuilder = new BlockStat.Builder(currentFrameNumber, BLOCK_SIZE);
  }

  /* **********************************
               Pipeline methods
     ********************************** */

  @Override
  public boolean process(AudioEvent event) {
    final float[] frame = event.getFloatBuffer();

    // do not perform computations if we are outside the spoken duration
    if (currentFrameNumber < FIRST_SPOKEN_FRAME) {
      currentFrameNumber++;
      return true;
    }

    if (currentFrameNumber > LAST_SPOKEN_FRAME) {
      currentFrameNumber++;
      return false;
    }

    // pitch extraction
    final PitchDetectionResult pitchResult = PITCH_DETECTOR.getPitch(frame);
    if (pitchResult.isPitched()) {
      float pitch = pitchResult.getPitch();
      if (pitch >= MIN_PITCH && pitch <= MAX_PITCH) {
        currentBlockStatBuilder.pitches.add(pitch);
      }
    }

    // energy extraction
    currentBlockStatBuilder.energies.add((float) smoothedEnergy(frame));

    // frequency bands energy extraction
    final List<Pair<Float, Float>> bandsEnergy = computeBandsEnergy(frame, FREQUENCY_BANDS);
    for (Pair<Float, Float> bandEnergy : bandsEnergy) {
      // retrieve list of band energies, and add new value to it
      currentBlockStatBuilder.bandsEnergies.get(bandEnergy.first).add(bandEnergy.second);
    }

    // handle going to the next frame
    currentFrameNumber++;
    if (currentFrameNumber % BLOCK_SIZE == 0) {
      // get ready for the next block
      stats.add(currentBlockStatBuilder.build());
      System.out.println("CallStat: " + stats.get(stats.size() - 1).toString());
      currentBlockStatBuilder = new BlockStat.Builder(currentFrameNumber, BLOCK_SIZE);
    }

    // never break the chain
    return true;
  }

  @Override
  public void processingFinished() { }

  /**
   * Calculates the local (linear) energy of an audio buffer.
   * Creates a copy of the frame and applies the window function on it before further processing.
   *
   * @param frame the audio frame
   * @return The local (linear) energy of an audio buffer.
   */
  public static double smoothedEnergy(final float[] frame) {
    final float[] windowedFrame = Arrays.copyOf(frame, frame.length);
    WINDOW_FUNCTION.apply(windowedFrame);

    double power = 0.0;
    for (float sample : windowedFrame) {
      power += sample * sample;
    }
    return power;
  }

  /**
   * Compute the energy for each specified band of the frame
   *
   * @param frame frame to extract the energies on
   * @param bands pairs of (band_center,band_width)
   * @return pairs of (band_center,band_energy)
   */
  private List<Pair<Float, Float>> computeBandsEnergy(float[] frame, Collection<Pair<Float, Float>> bands) {
    final List<Pair<Float, Float>> bandsEnergy = new ArrayList<>(); // (center,energy)

    float[] frameFiltered = new float[frame.length];
    for (Pair<Float, Float> band : bands) {
      System.arraycopy(frame, 0, frameFiltered, 0, frame.length);

      // filter to keep only the wanted band
      final BandPassFilter bandpassFilter = new BandPassFilter(band.first, band.second, SAMPLING_RATE);
      bandpassFilter.process(frameFiltered);

      // compute energy on filtered frame
      final float energy = (float) smoothedEnergy(frameFiltered);
      bandsEnergy.add(Pair.create(band.first, energy));
    }

    return bandsEnergy;
  }

  /**
   * Transforms a series of band delimiters into pairs (band_center,band_width)
   *
   * @param boundaries a series of at least two floats
   * @return the pairs
   */
  private static ImmutableList<Pair<Float, Float>> initBands(float... boundaries) {
    Preconditions.checkArgument(boundaries.length >= 2);
    final List<Pair<Float, Float>> intervals = new ArrayList<>();

    // transform [a b c] => [(a,b) (b,c)]
    for (int i = 0; i < boundaries.length - 1; ++i) {
      intervals.add(Pair.create(boundaries[i], boundaries[i + 1]));
    }

    // transform (begin,end) => (center,width)
    final Iterable<Pair<Float, Float>> bands = Collections2.transform(intervals, new Function<Pair<Float,Float>, Pair<Float, Float>>() {
      public Pair<Float, Float> apply(Pair<Float, Float> interval) {
        return Pair.create(
           (interval.second + interval.first) / 2f, // center of band
           interval.second - interval.first         // width of band
        );
      }
    });

    return ImmutableList.copyOf(bands);
  }

}
