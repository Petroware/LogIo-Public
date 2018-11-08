package no.petroware.logio.common;

import no.petroware.logio.util.RunningPercentile;

/**
 * Class for computing running statistics on a data stream.
 * <p>
 * No data is cached in the class, all statistics are computed
 * during streaming:
 *   <ul>
 *     <li>Number of values</li>
 *     <li>Number of actual values</li>
 *     <li>Number of no-values (NaN or Infinity)</li>
 *     <li>Minimum value</li>
 *     <li>Maximum value</li>
 *     <li>Mean (average)</li>
 *     <li>Variance</li>
 *     <li>Standard deviation</li>
 *     <li>25% percentile</li>
 *     <li>Median (50% percentile)</li>
 *     <li>75% percentile</li>
 *     <li>Sum</li>
 *  </ul>
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class Statistics
{
  /** Number of pushed values, not counting NaNs or infinities. [0,&gt;. */
  private int nActualValues_ = 0;

  /** Number of no-values. [0,&gt;. */
  private int nNoValues_ = 0;

  /** Min value. Double.NaN if N actual values is 0. */
  private double minValue_ = Double.NaN;

  /** Max value. Double.NaN if N actual values is 0. */
  private double maxValue_ = Double.NaN;

  /** Q1 (25%) quartile. */
  private RunningPercentile percentile25_ = new RunningPercentile(0.25);

  /** Q2 (50% = median) quartile. */
  private RunningPercentile median_ = new RunningPercentile(0.5);

  /** Q3 (75%) quartile. Double.NaN if N actual values is &lt; 2. */
  private RunningPercentile percentile75_ = new RunningPercentile(0.75);

  /** Mean (average) value. Double.NaN if N actual values is 0. */
  private double mean_ = Double.NaN;

  /** Variance. Double.NaN if N actual values is 0. */
  private double variance_ = Double.NaN;

  /** Sum of all values. */
  private double sum_ = 0.0;

  /**
   * Create an instance for keeping track of running statistics.
   * <p>
   * Add observation values by calling the {@link #push} method.
   */
  public Statistics()
  {
    // Nothing
  }

  /**
   * Include a new observation in the statistics.
   *
   * @param value  Observation to include. Double.NaN or Double.Infinity
   *               will count as no-values.
   */
  public void push(double value)
  {
    if (Double.isNaN(value) || Double.isInfinite(value))
      nNoValues_++;

    else {
      nActualValues_++;

      //
      // Percentiles
      //
      percentile25_.push(value);
      median_.push(value);
      percentile75_.push(value);

      //
      // Min value
      //
      if (Double.isNaN(minValue_) || value < minValue_)
        minValue_ = value;

      //
      // Max value
      //
      if (Double.isNaN(maxValue_) || value > maxValue_)
        maxValue_ = value;

      //
      // Sum
      //
      sum_ += value;

      //
      // Mean
      // See: https://www.johndcook.com/blog/standard_deviation/
      //
      if (Double.isNaN(mean_))
        mean_ = 0.0;
      double previousMean = mean_;
      mean_ += (value - mean_) / nActualValues_;

      //
      // Variance
      //
      if (Double.isNaN(variance_))
        variance_ = 0.0;
      variance_ += (value - previousMean) * (value - mean_);
    }
  }

  /**
   * Reset the statistics.
   */
  public void reset()
  {
    nActualValues_ = 0;
    nNoValues_ = 0;
    minValue_ = Double.NaN;
    maxValue_ = Double.NaN;
    percentile25_ = new RunningPercentile(0.25);
    median_ = new RunningPercentile(0.5);
    percentile75_ = new RunningPercentile(0.75);
    mean_ = Double.NaN;
    variance_ = Double.NaN;
    sum_ = 0.0;
  }

  /**
   * Return total number of observations. I.e. number of
   * times the {@link #push} method has been called.
   *
   * @return  Total number of observed values. [0,&gt;.
   */
  public int getNValues()
  {
    return nActualValues_ + nNoValues_;
  }

  /**
   * Return number of <em>actual</em> observations,
   * i.e. all values except NaNs or infinities.
   *
   * @return  Number of <em>actual</em> observations. [0,&gt;.
   */
  public int getNActualValues()
  {
    return nActualValues_;
  }

  /**
   * Return number of no-values (NaN or Infinity) observed.
   *
   * @return  Number of no-values observed. [0,&gt;.
   */
  public int getNNoValues()
  {
    return nNoValues_;
  }

  /**
   * Return minimum observed value.
   *
   * @return  Minimum observed value.
   */
  public Object getMinValue()
  {
    return minValue_;
  }

  /**
   * Return maximum observed value.
   *
   * @return  Maximum observed value.
   */
  public Object getMaxValue()
  {
    return maxValue_;
  }

  /**
   * Return mean (average) value of the (actual) observations.
   *
   * @return  Mean value of the observations.
   */
  public double getMean()
  {
    return mean_;
  }

  /**
   * Return the variance of the (actual) observations.
   *
   * @return  Variance of the observations. [0,&gt;.
   */
  public double getVariance()
  {
    return nActualValues_ > 1 ? variance_ / (nActualValues_ - 1) : 0.0;
  }

  /**
   * Return the standard deviation of the (actual) observations.
   *
   * @return  Standard deviation of the observations. [0,&gt;.
   */
  public double getStandardDeviation()
  {
    return Math.sqrt(getVariance());
  }

  /**
   * Return the 25% percentile of the (actual) observations.
   *
   * @return  The 25% percentile of the observations.
   */
  public double getPercentile25()
  {
    return percentile25_.getPercentile();
  }

  /**
   * Return the 50% percentile (median) of the (actual) observations.
   *
   * @return  The median of the observations.
   */
  public double getMedian()
  {
    return median_.getPercentile();
  }

  /**
   * Return the 75% percentile of the (actual) observations.
   *
   * @return  The 75% percentile of the observations.
   */
  public double getPercentile75()
  {
    return percentile75_.getPercentile();
  }

  /**
   * Return the sum of the (actual) observations.
   *
   * @return  Sum of the observations.
   */
  public double getSum()
  {
    return sum_;
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append("N values...........: " + getNValues() + "\n");
    s.append("N actual values....: " + getNActualValues() + "\n");
    s.append("N no-values........: " + getNNoValues() + "\n");
    s.append("Min value..........: " + getMinValue() + "\n");
    s.append("Max value..........: " + getMaxValue() + "\n");
    s.append("Mean...............: " + getMean() + "\n");
    s.append("Variance...........: " + getVariance() + "\n");
    s.append("Standard deviation.: " + getStandardDeviation() + "\n");
    s.append("25% percentile.....: " + getPercentile25() + "\n");
    s.append("Median.............: " + getMedian() + "\n");
    s.append("75% percentile.....: " + getPercentile75() + "\n");
    s.append("Sum................: " + getSum() + "\n");

    return s.toString();
  }
}
