package no.petroware.logio.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A class capable of formatting (i.e write as text) numbers so that
 * they get uniform appearance and can be presented together, typically
 * in a column with decimal symbol aligned etc.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class Formatter
{
  /** Default number of significant digits. */
  private static final int N_SIGNIFICANT_DEFAULT = 7;

  /** Default number of decimals. */
  private static final int N_DECIMALS_DEFAULT = 2;

  /** The smallest value before switching to scientific notation. */
  private static final double MIN_NON_SCIENTIFIC = 0.0001;

  /** The largest value before switching to scientific notation. */
  private static final double MAX_NON_SCIENTIFIC = 9999999.0;

  /** The format to use. Non-null. */
  private final DecimalFormat format_;

  /**
   * Return number of significant decimals there is in the
   * specified floating point value.
   *
   * @param d  Number to check.
   * @return   Number of decimals. [0,&gt;.
   */
  private static int countDecimals(double d)
  {
    if (Double.isNaN(d))
      return 0;

    if (Double.isInfinite(d))
      return 0;

    d = Math.abs(d);

    long wholePart = Math.round(d);
    int nSignificant = ("" + wholePart).length();

    double fractionPart = d - wholePart;

    int nDecimals = 0;
    int order = 1;
    while (true) {
      double floating = fractionPart * order;
      long whole = Math.round(floating);

      double eps = Math.abs(whole - floating);
      double diff = whole != 0.0 ? eps / whole : eps;

      if (diff < 0.0001)
        break;

      if (nSignificant >= 12)
        break;

      order *= 10;
      nDecimals++;
      nSignificant++;
    }

    return nDecimals;
  }

  /**
   * Create a common formatter for the specified set of numbers.
   *
   * @param values              Representative values to use for creating the formatter.
   *                            Null to create a generic formatter independent of any
   *                            actual values.
   * @param nSignificantDigits  Number of significant digits. Defaults to 7 if null.
   *                            Ignored if nDecimals is specified.
   * @param nDecimals           Number of decimals. If null, decide by significan digits.
   * @param locale              Locale to present numbers in. Null for default.
   */
  public Formatter(double[] values, Integer nSignificantDigits, Integer nDecimals, Locale locale)
  {
    int nActualSignificantDigits = nSignificantDigits != null ? nSignificantDigits : N_SIGNIFICANT_DEFAULT;
    int nActualDecimals = N_DECIMALS_DEFAULT;
    Locale actualLocale = locale != null ? locale : Locale.ROOT;

    double maxValue = 0.0;

    boolean isScientific = false;

    // Maximum number of decimals needed to represent the values provided
    int nMaxDecimalsNeeded = 0;

    //
    // Loop over all the representative values to find the maximum
    // and to check if we should use scientific notation.
    //
    if (values != null) {
      for (int i = 0; i < values.length; i++) {

        double value = values[i];

        // Leave non-printable characters
        if (Double.isNaN(value) || Double.isInfinite(value))
          continue;

        // Work with the absolute value only
        value = Math.abs(value);

        //
        // Check if we should go scientific
        //
        if (value > MAX_NON_SCIENTIFIC || value != 0.0 && value < MIN_NON_SCIENTIFIC) {
          isScientific = true;
          nActualDecimals = nActualSignificantDigits - 1;
          break;
        }

        // Keep track of maximum numeric value of the lot
        if (value > maxValue)
          maxValue = value;

        // Find how many decimals is needed to represent this value correctly
        int nDecimalsNeeded = countDecimals(value);
        if (nDecimalsNeeded > nMaxDecimalsNeeded)
          nMaxDecimalsNeeded = nDecimalsNeeded;
      }
    }

    //
    // Determine n decimals for the non-scietific case
    //
    if (!isScientific) {
      long wholePart = Math.round(maxValue);
      int length = ("" + wholePart).length();
      nActualDecimals = Math.max(nActualSignificantDigits - length, 0);

      // If there are values provided, and they need fewer decimals
      // than computed, we reduce this
      if (values != null && values.length > 0 && nMaxDecimalsNeeded < nActualDecimals)
        nActualDecimals = nMaxDecimalsNeeded;
    }

    // Override n decimals on users request
    if (nDecimals != null)
      nActualDecimals = nDecimals;

    //
    // Create the format string
    //
    StringBuilder formatString = new StringBuilder();
    if (isScientific) {
      formatString.append("0.E0");
      for (int i = 0; i < nActualDecimals; i++)
        formatString.insert(2, '0');
    }
    else {
      formatString.append(nActualDecimals > 0 ? "0." : "0");
      for (int i = 0; i < nActualDecimals; i++)
        formatString.append('0');
    }

    //
    // Create the actual decimal format
    //
    DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(actualLocale);
    format_ = new DecimalFormat(formatString.toString(), formatSymbols);
  }

  /**
   * Create a common formatter for the specified set of numbers.
   *
   * @param values              Representative values to use for creating the formatter.
   *                            Null to create a generic formatter independent of any
   *                            actual values.
   */
  public Formatter(double[] values)
  {
    this(values, null, null, null);
  }

  /**
   * Create a default number formatter,
   */
  public Formatter()
  {
    this(null, null, null, null);
  }

  /**
   * Format the specified value according to the formatting defined
   * by this formatter.
   *
   * @param value  Value to format,
   * @return       Text representation of the value according to the format.
   */
  public String format(double value)
  {
    // Handle the non-printable characters
    if (Double.isNaN(value) || Double.isInfinite(value))
      return "";

    // 0.0 easily gets lost if written with many decimals like 0.00000.
    // Consequently we write this as either 0.0 or 0
    if (value == 0.0)
      return format_.getMaximumFractionDigits() == 0 ? "0" : "0.0";

    return format_.format(value);
  }

  /**
   * Return the back-end decimal format of this formatter.
   *
   * @return  The back-end decimal format of this formatter. Never null.
   */
  public DecimalFormat getFormat()
  {
    return format_;
  }
}
