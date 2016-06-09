package io.mem0r1es.memoit.sensors.external.voice.util;

import be.tarsos.dsp.util.fft.FFT;

import static io.mem0r1es.memoit.sensors.external.voice.BlockProcessor.WINDOW_FUNCTION;
import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.FRAME_SIZE;
import static io.mem0r1es.memoit.sensors.external.voice.CallRecorder.SAMPLING_RATE;

/**
 * Collection of utilities needed for MFCC processing
 *
 * @author Cyriaque Brousse
 */
public class MfccPipeline {

  /* **********************************
             Processing constants
     ********************************** */

  /** Number of cepstrum coefficients. Used for MFCC */
  public static final int NUM_CEPSTRUM_COEF = 15;

  /** Number of filters to use in MFCC processing */
  private static final int NUM_MEL_FILTERS = 30;

  /** Lower bound of MFCC filter bank */
  private static final float MFCC_LOWER_FILTER_FREQ = 133.3334f;

  /** Upper bound of MFCC filter bank */
  private static final float MFCC_UPPER_FILTER_FREQ = SAMPLING_RATE / 2f;

  /** Fast Fourier transform, needed for MFCC */
  private static final FFT FFT = new FFT(FRAME_SIZE, WINDOW_FUNCTION);

  /** MFCC filter bank */
  private final int[] centerFrequencies = new int[NUM_MEL_FILTERS + 2];

  /* **********************************
            Constructor & Pipeline
     ********************************** */

  public MfccPipeline() {
    initCenterFrequencies();
  }

  /**
   * Runs the MFCC pipeline on the provided audio buffer
   *
   * @param frame audio buffer to process
   * @return the array of MFCCs
   */
  public float[] process(float[] frame) {
    float[] bin = magnitudeSpectrum(frame);
    float[] bank = melFilter(bin);
    float[] transformation = nonLinearTransformation(bank);
    return cepstralCoefficients(transformation);
  }

  /* **********************************
                   Helpers
     ********************************** */

  private void initCenterFrequencies() {
    centerFrequencies[0] = Math.round(MFCC_LOWER_FILTER_FREQ / SAMPLING_RATE * FRAME_SIZE);
    centerFrequencies[centerFrequencies.length - 1] = FRAME_SIZE / 2;

    double mel[] = new double[2];
    mel[0] = freqToMel(MFCC_LOWER_FILTER_FREQ);
    mel[1] = freqToMel(MFCC_UPPER_FILTER_FREQ);

    float factor = (float) ((mel[1] - mel[0]) / (NUM_MEL_FILTERS + 1));
    // compute the center frequencies
    for (int i = 1; i <= NUM_MEL_FILTERS; i++) {
      float fc = (inverseMel(mel[0] + factor * i) / SAMPLING_RATE) * FRAME_SIZE;
      centerFrequencies[i - 1] = Math.round(fc);
    }
  }

  /**
   * convert frequency to mel-frequency
   */
  private float freqToMel(float freq) {
    return (float) (2595 * Math.log10(1 + freq / 700));
  }

  /**
   * calculates the inverse of Mel Frequency
   */
  private float inverseMel(double x) {
    return (float) (700 * (Math.pow(10, x / 2595) - 1));
  }

  /**
   * computes the magnitude spectrum of the input frame
   */
  private float[] magnitudeSpectrum(float[] frame) {
    float spectrum[] = new float[frame.length];

    FFT.forwardTransform(frame);

    // calculate magnitude spectrum
    for (int i = 0; i < frame.length / 2; i++) {
      spectrum[frame.length / 2 + i] = FFT.modulus(frame, frame.length / 2 - 1 - i);
      spectrum[frame.length / 2 - 1 - i] = spectrum[frame.length / 2 + i];
    }

    return spectrum;
  }

  /**
   * Calculate the output of the mel filter
   */
  private float[] melFilter(float bin[]) {
    float temp[] = new float[NUM_MEL_FILTERS + 2];

    for (int k = 1; k <= NUM_MEL_FILTERS; k++) {
      float num1 = 0;
      float num2 = 0;

      float den = (centerFrequencies[k] - centerFrequencies[k - 1] + 1);

      for (int i = centerFrequencies[k - 1]; i <= centerFrequencies[k]; i++) {
        num1 += bin[i] * (i - centerFrequencies[k - 1] + 1);
      }
      num1 /= den;

      den = (centerFrequencies[k + 1] - centerFrequencies[k] + 1);

      for (int i = centerFrequencies[k] + 1; i <= centerFrequencies[k + 1]; i++) {
        num2 += bin[i] * (1 - ((i - centerFrequencies[k]) / den));
      }

      temp[k] = num1 + num2;
    }

    float bank[] = new float[NUM_MEL_FILTERS];

    for (int i = 0; i < NUM_MEL_FILTERS; i++) {
      bank[i] = temp[i + 1];
    }

    return bank;
  }

  /**
   * the output of mel filtering is subjected to a logarithm function (natural logarithm)
   * @param bank Output of mel filtering
   * @return Natural log of the output of mel filtering
   */
  private float[] nonLinearTransformation(float bank[]) {
    float f[] = new float[bank.length];
    final float floor = -50f;

    for (int i = 0; i < bank.length; i++) {
      f[i] = (float) Math.log(bank[i]);

      // check if ln() returns a value less than the floor
      if (f[i] < floor) {
        f[i] = floor;
      }
    }

    return f;
  }

  /**
   * Cepstral coefficients are calculated from the output of the Non-linear Transformation method
   * @param transformation Output of the Non-linear Transformation method
   * @return Cepstral Coefficients
   */
  private float[] cepstralCoefficients(float[] transformation) {
    float coefficients[] = new float[NUM_CEPSTRUM_COEF];

    for (int i = 0; i < coefficients.length; i++) {
      for (int j = 0; j < transformation.length; j++) {
        coefficients[i] += transformation[j] * Math.cos(Math.PI * i / transformation.length * (j + 0.5));
      }
    }

    return coefficients;
  }

}
