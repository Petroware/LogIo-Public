package no.petroware.logio.util;

import java.util.Arrays;

/**
 * Class for computing the <em>running percentile</em> (such as median)
 * for a set of observations without holding any of the observations.
 * <p>
 * Implemented using the P-Squared algorithm of
 * <b>Raj Jain</b> and <b>Imrich Chlamtac</b>:
 * <p>
 *   <a href="https://www.cse.wustl.edu/~jain/papers/ftp/psqr.pdf">
 *   https://www.cse.wustl.edu/~jain/papers/ftp/psqr.pdf</a>
 * <p>
 * The present implementation is a simplified version of the
 * following: <a href="https://github.com/jacksonicson/psquared">
 * https://github.com/jacksonicson/psquared</a>.
 * <p>
 * There is also an Apache implementation that seems overly complex:
 * PSquaredPercentile.java.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class RunningPercentile
{
  /** Number of markers. */
  private static final int N_MARKERS = 5;

  /** Percentile to consider. [0.0,1.0]. */
  private final double p_;

  /** Desired marker positions. */
  private final double[] nDesired_ = new double[N_MARKERS];

  /** Desired marker increments. */
  private final double[] dn_ = new double[N_MARKERS];

  /** Marker heights. */
  private final double[] q_ = new double[N_MARKERS];

  /** Marker positions. */
  private final int[] n_ = new int[N_MARKERS];

  /** Total number of observations. */
  private int nObservations_;

  /**
   * Create a running percentile computation instance for the
   * specified percentile target. p = 0.5 for median, p = 0.25 for
   * the Q1 quartile etc.
   *
   * @param p  Percentile target. [0.0,1.0].
   * @throws IllegalArgumentException  If p is outside bounds.
   */
  public RunningPercentile(double p)
  {
    if (p < 0 || p > 1.0)
      throw new IllegalArgumentException("Invalid p: " + p);

    p_ = p;

    // Fixed desired marker increments
    dn_[0] = 0.0;
    dn_[1] = p_ / 2.0;
    dn_[2] = p_;
    dn_[3] = (1.0 + p_) / 2.0;
    dn_[4] = 1.0;

    clear();
  }

  /**
   * Clear the percentile computer, effectively making it ready
   * for a new stream of observations.
   */
  public void clear()
  {
    nObservations_ = 0;

    // Initial marker height and positions
    for (int i = 0; i < N_MARKERS; i++) {
      q_[i] = 0.0;
      n_[i] = i;
    }

    // Initial desired marker positions
    nDesired_[0] = 0.0;
    nDesired_[1] = 2.0 * p_;
    nDesired_[2] = 4.0 * p_;
    nDesired_[3] = 2.0 + 2.0 * p_;
    nDesired_[4] = 4.0;
  }

  /**
   * Compute the correct height of the q[i] marker based on a parabolic
   * fit between neighbors. TODO: Improve description.
   *
   * @param d  +1.0 or -1.0.
   * @param i  Marker to compute height of. [2,4].
   * @return   Computed height.
   */
  private double parabolic(double d, int i)
  {
    double a = d / (n_[i + 1] - n_[i - 1]);

    double b = (n_[i] - n_[i - 1] + d) * (q_[i + 1] - q_[i]) / (n_[i + 1] - n_[i]) +
               (n_[i + 1] - n_[i] - d) * (q_[i] - q_[i - 1]) / (n_[i] - n_[i - 1]);

    return q_[i] + a * b;
  }

  /**
   * Compute the correct height of the q[i] marker based on a linear
   * interpolation between neighbors. TODO: Improve description.
   *
   * @param d  +1.0 or -1.0.
   * @param i  Marker to compute height of. [2,4].
   * @return   Computed height.
   */
  private double linear(int d, int i)
  {
    return q_[i] + d * (q_[i + d] - q_[i]) / (n_[i + d] - n_[i]);
  }

  /**
   * Include a new observation in the statistics.
   *
   * @param value  Observation to include.
   */
  public void push(double value)
  {
    nObservations_++;

    //
    // Still in the initial phase
    //
    if (nObservations_ <= N_MARKERS) {
      q_[nObservations_ - 1] = value;
      Arrays.sort(q_, 0, nObservations_);
      return;
    }

    //
    // Regular phase
    //
    int k = -1;
    if (value < q_[0]) {
      q_[0] = value;
      k = 0;
    }
    else if (value < q_[1])
      k = 0;
    else if (value < q_[2])
      k = 1;
    else if (value < q_[3])
      k = 2;
    else if (value <= q_[4])
      k = 3;
    else {
      // Update maximum value
      q_[4] = value;
      k = 3;
    }

    // Increment positions to the right of k
    for (int i = k + 1; i < N_MARKERS; i++)
      n_[i]++;

    // Update the desired marker positions
    for (int i = 0; i < N_MARKERS; i++)
      nDesired_[i] += dn_[i];

    // Adjust marker heights 2-4 if necessary
    for (int i = 1; i < N_MARKERS - 1; i++) {
      double d = nDesired_[i] - n_[i];

      if ((d >= 1.0 && (n_[i + 1] - n_[i]) > 1) || (d <= -1 && (n_[i - 1] - n_[i]) < -1)) {
        int sign = d >= 0 ? 1 : -1;

        // Adjusting q using P2 or linear formula
        double p2 = parabolic(sign, i);
        q_[i] = (q_[i - 1] < p2 && p2 < q_[i + 1]) ? p2 : linear(sign, i);

        n_[i] += sign;
      }
    }
  }

  /**
   * Return the current percentile value.
   *
   * @return  Current percentile value.
   */
  public double getPercentile()
  {
    if (nObservations_ >= N_MARKERS)
      return q_[2];
    else if (nObservations_ == 0)
      return Double.NaN;
    else
      return q_[(int) (p_ * 0.999 * nObservations_)]; // 0.999: Make sure index cannot be 5
  }

  /**
   * Return current number of observations.
   *
   * @return  Current number of observations. [0,&gt;.
   */
  public int getNObservations()
  {
    return nObservations_;
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append("q: " + Arrays.toString(q_) + "\n");
    s.append("n: " + Arrays.toString(n_) + "\n");
    s.append("n': " + Arrays.toString(nDesired_) + "\n");
    s.append("p = " + getPercentile() + "\n");

    return s.toString();
  }
}
