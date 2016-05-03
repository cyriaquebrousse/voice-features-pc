package io.mem0r1es.memoit.sensors.external.voice.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.tarsos.dsp.beatroot.Peaks;

import static io.mem0r1es.memoit.sensors.external.voice.BlockProcessor.BLOCK_SIZE;
import static io.mem0r1es.memoit.sensors.external.voice.BlockProcessor.FREQUENCY_BANDS;
import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.FRAME_SIZE;
import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.SAMPLING_RATE;
import static io.mem0r1es.memoit.sensors.external.voice.util.CollectionsUtils.unzipLeft;
import static io.mem0r1es.memoit.sensors.external.voice.util.CollectionsUtils.unzipRight;

/**
 * Holds the statistics for each block of frames
 *
 * @author Cyriaque Brousse
 */
public class BlockStat {

  /** Id of the first frame of this block */
  public final long startId;

  public final ImmutableMap<String, Number> stats;

  public BlockStat(long startId, ImmutableMap<String, Number> stats) {
    this.startId = startId;
    this.stats = stats;
  }

  /**
   * Implements Builder pattern for constructing the block statistic
   */
  public static final class Builder {
    public final long startId;

    /** Extracted pitch for each frame */
    public final ArrayList<Float> pitches = new ArrayList<>(BLOCK_SIZE);

    /** Extracted energy for each frame */
    public final ArrayList<Float> energies = new ArrayList<>(BLOCK_SIZE);

    /** For each band: (band_center, band_energy[frame0..frameN] ) */
    public final Map<Float, List<Float>> bandsEnergies = new HashMap<>();
    {
      for (Pair<Float, Float> band : FREQUENCY_BANDS) {
        bandsEnergies.put(band.first, new ArrayList<Float>());
      }
    }

    public Builder(long startId) {
      this.startId = startId;
    }

    public BlockStat build() {
      final ImmutableMap.Builder<String, Number> stat = ImmutableMap.builder();

      /* **********************************
                   Stats on pitch
         ********************************** */
      {
        final DescriptiveStatistics pitchStat = getStat(pitches);
        final List<Float> pitchDerivatives = getDerivatives(pitches);
        final DescriptiveStatistics pitchUpSlopeStat = getStat(getSlopes(pitchDerivatives, true));
        final DescriptiveStatistics pitchDownSlopeStat = getStat(getSlopes(pitchDerivatives, false));

        final float pitchMean = (float) pitchStat.getMean();
        final float pitchStdDev = (float) pitchStat.getStandardDeviation();

        stat.put("pitchMean", pitchMean)
           .put("pitchMedian", (float) pitchStat.getPercentile(0.50))
           .put("pitchStdDev", pitchStdDev)
           .put("pitchMax", (float) pitchStat.getMax())
           .put("pitchRange", (float) (pitchStat.getMax() - pitchStat.getMin()))
           .put("pitchUpSlopeMedian", (float) pitchUpSlopeStat.getPercentile(0.50))
           .put("pitchDownSlopeMedian", (float) pitchDownSlopeStat.getPercentile(0.50))
           .put("pitchUpFramesRatio", (float) pitchUpSlopeStat.getN() / (float) BLOCK_SIZE)
           .put("pitchVoicedFramesRatio", (float) pitches.size() / (float) BLOCK_SIZE);

        // peaks values analysis
        final List<Pair<Integer, Float>> pitchPeaks = extractPitchPeaks(pitches, pitchMean, pitchStdDev, 10);

        final DescriptiveStatistics pitchPeaksStat = getStat(unzipRight(pitchPeaks));
        stat.put("pitchPeaksNum", pitchPeaksStat.getN())
           .put("pitchPeaksMean", pitchPeaksStat.getMean())
           .put("pitchPeaksStdDev", pitchPeaksStat.getStandardDeviation())
           .put("pitchPeaksRange", pitchPeaksStat.getMax() - pitchPeaksStat.getMin());

        // peaks distances analysis
        final DescriptiveStatistics pitchPeaksDistStat = getStat(computeTimeDistances(unzipLeft(pitchPeaks)));
        stat.put("pitchPeaksDistMean", pitchPeaksDistStat.getMean())
           .put("pitchPeaksDistStdDev", pitchPeaksDistStat.getStandardDeviation())
           .put("pitchPeaksDistRange", pitchPeaksDistStat.getMax() - pitchPeaksDistStat.getMin())
           .put("pitchPeaksDistMin", pitchPeaksDistStat.getMin());

      }

      /* **********************************
                   Stats on energy
         ********************************** */
      final float meanGlobalEnergy;
      {
        final DescriptiveStatistics energyStat = getStat(energies);
        final List<Float> energyDerivatives = getDerivatives(energies);
        final DescriptiveStatistics energyUpSlopeStat = getStat(getSlopes(energyDerivatives, true));
        final DescriptiveStatistics energyDownSlopeStat = getStat(getSlopes(energyDerivatives, false));

        meanGlobalEnergy = (float) energyStat.getMean();

        stat.put("energyMean", meanGlobalEnergy)
            .put("energyMedian", (float) energyStat.getPercentile(0.50))
            .put("energyStdDev", (float) energyStat.getStandardDeviation())
            .put("energyMax", (float) energyStat.getMax())
            .put("energyRange", (float) (energyStat.getMax() - energyStat.getMin()))
            .put("energyUpSlopeMedian", (float) energyUpSlopeStat.getPercentile(0.50))
            .put("energyDownSlopeMedian", (float) energyDownSlopeStat.getPercentile(0.50))
            .put("energyUpFramesRatio", (float) energyUpSlopeStat.getN() / (float) BLOCK_SIZE);
      }

      /* **********************************
           Stats on frequency bands energy
         ********************************** */
      for (Entry<Float, List<Float>> bandEnergies : bandsEnergies.entrySet()) {
        final float center = bandEnergies.getKey();
        final List<Float> values = bandEnergies.getValue();
        final DescriptiveStatistics energyStat = getStat(values);

        final float meanBandEnergy = (float) energyStat.getMean();

        final String prefix = "band" + center + '_';
        stat.put(prefix + "energyMean", meanBandEnergy)
           .put(prefix + "energyMedian", (float) energyStat.getPercentile(0.50))
           .put(prefix + "energyStdDev", (float) energyStat.getStandardDeviation())
           .put(prefix + "energyMax", (float) energyStat.getMax())
           .put(prefix + "energyRange", (float) (energyStat.getMax() - energyStat.getMin()))
           .put(prefix + "energyRatio", meanBandEnergy / meanGlobalEnergy);
      }

      return new BlockStat(startId, stat.build());
    }

    /**
     * Filters the derivatives list to keep only the positive/negative ones
     *
     * @param derivatives list to filter
     * @param up {@code true}  if you want the strictly up slopes,
     *           {@code false} if you want the strictly down ones
     * @return the filtered list of derivatives
     */
    private List<Float> getSlopes(List<Float> derivatives, final boolean up) {
      return Lists.newArrayList(Collections2.filter(derivatives, new Predicate<Float>() {
        public boolean apply(Float f) {
          if (up) {
            return f != null && f > 0f;
          } else {
            return f != null && f < 0f;
          }
        }
      }));
    }

    private DescriptiveStatistics getStat(Iterable<Float> in) {
      DescriptiveStatistics stat = new DescriptiveStatistics(BLOCK_SIZE);
      for (float f : in) {
        stat.addValue(f);
      }
      return stat;
    }

    private List<Float> getDerivatives(List<Float> in) {
      if (in.size() < 2) {
        return new ArrayList<>();
      }

      List<Float> out = new ArrayList<>();

      for (int i = 0; i < in.size() - 1; ++i) {
        out.add(in.get(i + 1) - in.get(i));
      }

      return out;
    }

    private double[] arrayFromFloatList(List<Float> in) {
      if (in.isEmpty()) {
        return new double[]{0};
      }

      double[] out = new double[in.size()];
      for (int i = 0; i < in.size(); ++i) {
        out[i] = in.get(i);
      }
      return out;
    }

    private List<Pair<Integer, Float>> extractPitchPeaks(final List<Float> values,
                                                         float avg, float std,
                                                         float threshold) {
      // extract the peaks indexes and map to (index,value)
      Iterable<Pair<Integer, Float>> peaks = Collections2.transform(
         Peaks.findPeaks(arrayFromFloatList(values), 2, avg - std, 0.1, false),
         new Function<Integer, Pair<Integer, Float>>() {
           public Pair<Integer, Float> apply(Integer i) {
             return Pair.create(i, values.get(i));
           }
         }
      );

      // filter out all peaks that are too close to the average
      final List<Pair<Integer, Float>> filtered = new ArrayList<>();
      for (Pair<Integer, Float> peak : peaks) {
        if (Math.abs(peak.second - avg) > threshold) {
          filtered.add(peak);
        }
      }

      return filtered;
    }

    /**
     * @param in list of indexes
     * @return the time difference between each index
     */
    private Iterable<Float> computeTimeDistances(List<Integer> in) {
      if (in.size() < 2) {
        return new ArrayList<>();
      }

      final List<Float> distances = new ArrayList<>();

      for (int i = 0; i < in.size() - 1; ++i) {
        final int indexDistance = in.get(i + 1) - in.get(i);
        distances.add((float) indexDistance * FRAME_SIZE / SAMPLING_RATE);
      }

      return distances;
    }
  }

  @Override
  public String toString() {
    return "BlockStat{" +
       "startId=" + startId +
       ", stats=" + stats +
       '}';
  }
}
