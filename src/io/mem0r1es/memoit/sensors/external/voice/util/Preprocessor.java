package io.mem0r1es.memoit.sensors.external.voice.util;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import io.mem0r1es.memoit.sensors.external.voice.BlockProcessor;

import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.FRAME_SIZE;
import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.SAMPLING_RATE;
import static io.mem0r1es.memoit.sensors.external.voice.util.BlockStat.Builder.getStat;

/**
 * Silence preprocessor.
 * Detects the first and the last non-silent frames on the given voice file
 *
 * @author Cyriaque Brousse
 */
public class Preprocessor implements AudioProcessor {

  private final List<Float> energies = new ArrayList<>();
  private final int[] output;

  /**
   * Constructs the preprocessor
   *
   * @param output output buffer that will contain (start,end) when the processor has finished
   */
  public Preprocessor(int[] output) {
    if (output.length != 2) {
      throw new IllegalArgumentException("output buffer must be of length 2 (start,end)");
    }

    this.output = output;
  }

  @Override
  public boolean process(AudioEvent event) {
    final float[] frame = event.getFloatBuffer();

    energies.add((float) BlockProcessor.smoothedEnergy(frame));

    // never break the chain
    return true;
  }

  @Override
  public void processingFinished() {
    System.out.println("# Preprocessor for silence finished and yields:");
    // determine silence threshold
    // that is, 0.5% of maximal energy for the voice excerpt
    final DescriptiveStatistics stat = getStat(energies);
    final float silenceThreshold = (float) (0.005 * stat.getMax());

    // determine first non-silent frame
    for (int i = 0; i < energies.size(); i++) {
      if (energies.get(i) > silenceThreshold) {
        output[0] = i;
        System.out.println("START @ " + i + " : " + frameToTime(i));
        break;
      }
    }

    // determine last non-silent frame
    for (int i = energies.size() - 1; i >= 0; i--) {
      if (energies.get(i) > silenceThreshold) {
        output[1] = i;
        System.out.println("END @ " + i + " : " + frameToTime(i) + "\n");
        break;
      }
    }
  }

  public double frameToTime(long frameNumber) {
    return (double) frameNumber * FRAME_SIZE / SAMPLING_RATE;
  }
}
