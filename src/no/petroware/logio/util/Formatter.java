package no.petroware.logio.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

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
   * Create a common formatter for the set of numbers.
   *
   * @param values             Representative values to use for creating the format. Non-null.
   * @param nSignificantDigits Optional number of significant digits. Defaults to 7 if not specified.
   * @param nDecimals          Optional number of decimals. Defaults to 2 if not specified.
   * @throws IllegalArgumentException  If values is null.
   */
  public Formatter(double[] values, Integer nSignificantDigits, Integer nDecimals)
  {
    boolean isScientific = false;

    int nSignificant = nSignificantDigits != null ? nSignificantDigits : N_SIGNIFICANT_DEFAULT;
    int nDec = N_DECIMALS_DEFAULT;

    double maxValue = -1.0;

    //
    // Loop over all the representative values to find the maximum
    // and to check if we should use scientific notation.
    //
    for (int i = 0; i < values.length; i++) {

      double value = values[i];

      // Leave non-printable characters
      if (Double.isNaN(value) || Double.isInfinite(value))
        continue;

      // Work with the absolute value only
      value = Math.abs(value);

      if (value > 0.0 && (value < MIN_NON_SCIENTIFIC || value > MAX_NON_SCIENTIFIC)) {
        isScientific = true;
        nDec = nSignificant - 1;
        break;
      }

      if (value > maxValue)
        maxValue = value;
    }

    if (!isScientific) {
      long wholePart = Math.round(maxValue);
      int length = ("" + wholePart).length();
      nDec = Math.max(nSignificant - length, 0);
    }

    if (nDecimals != null)
      nDec = nDecimals;

    // Create the decimal format
    StringBuilder s = new StringBuilder();
    if (isScientific) {
      s.append("0.E0");
      for (int i = 0; i < nDec; i++)
        s.insert(2, '0');
    }
    else {
      s.append(nDec > 0 ? "0." : "0");
      for (int i = 0; i < nDec; i++)
        s.append('0');
    }

    // This is the format that can be used to format
    // values of similar type to the representation values.
    DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
    formatSymbols.setDecimalSeparator('.');

    format_ = new DecimalFormat(s.toString(), formatSymbols);
  }

  /**
   * Create a common formatter for the set of numbers.
   * Use default values for significant digits and decimals.
   *
   * @param values  Representative values to use for creating the format. Non-null.
   */
  public Formatter(double[] values)
  {
    this(values, null, null);
  }

  /**
   * Create a default number formatter,
   */
  public Formatter()
  {
    this(new double[] {1.0});
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

    // 0.0 is handled specifically to avoid 0.0000 etc
    if (value == 0.0)
      return "0.0";

    return format_.format(value);
  }
}
