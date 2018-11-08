package no.petroware.logio.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A collection of utilities for the Log I/O library.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class Util
{
  /** Pre-created strings of spaces of a given length. */
  private final static String[] SPACES = new String[100];

  /**
   * Initialize static members of this class.
   */
  static {
    StringBuilder s = new StringBuilder("");
    for (int i = 0; i < 100; i++) {
      SPACES[i] = s.toString();
      s.append(' ');
    }
  }

  /**
   * Private constructor to prevent client instantiation.
   */
  private Util()
  {
    // Nothing
  }

  /**
   * Return a string where start end end quotes are removed.
   *
   * @param text  Text where quotes should be removed. Non-null.
   * @return      Text without quotes. Never null.
   */
  public static String removeQuotes(String text)
  {
    assert text != null : "text cannot be null";

    if (text.length() > 1 &&
        (text.startsWith("\"") && text.endsWith("\"") ||
         text.startsWith("'") && text.endsWith("'")))
      text = text.substring(1, text.length() - 1);

    return text.trim();
  }

  /**
   * Return number of significant decimals in the specified
   * floating point value.
   *
   * @param d  Number to check.
   * @return   Number of significant decimals. [0,&gt;.
   */
  public static int countDecimals(double d)
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
   * Return a string containing the specified number of
   * space characters.
   *
   * @param n  Length of string to create.
   * @return   Requested string. If n is less than or equal to
   *           0 an empty string is returned. Never null.
   */
  public static String getSpaces(int n)
  {
    if (n <= 0)
      return "";

    if (n < SPACES.length)
      return SPACES[n];

    StringBuilder s = new StringBuilder("");
    for (int i = 0; i < n; i++)
      s.append(' ');

    return s.toString();
  }

  /**
   * Return the positions of the c character inside
   * the specified string.
   *
   * @param s  String to check. Non-null.
   * @param c  Character to search for.
   * @return   Index positions of c within s.
   */
  public static int[] pos(String s, char c)
  {
    assert s != null : "s cannot be null";

    List<Integer> pos = new ArrayList<>();

    int i = 0;
    while (i < s.length()) {
      int p = s.indexOf(c, i);

      if (p == -1)
        break;

      pos.add(p);
      i = p + 1;
    }

    int[] a = new int[pos.size()];
    i = 0;
    for (Integer value : pos)
      a[i++] = value;

    return a;
  }

  /**
   * Check if a given object represents a numeric value.
   *
   * @param value  Value to check. May be null, in case false will be returned.
   * @return  True is value is a numeric value, false otherwise.
   */
  public static boolean isNumeric(Object value)
  {
    if (value == null)
      return false;

    if (value instanceof Number)
      return true;

    try {
      Double.parseDouble(value.toString());
      return true;
    }
    catch (NumberFormatException exception) {
      return false;
    }
  }

  /**
   * Check if the specified value type is numeric.
   *
   * @param valueType  Value type to check. Non-null.
   * @return           True if the value type is numeric, false otherwise.
   * @throws IllegalArgumentException  If valueType is null.
   */
  public static boolean isNumeric(Class<?> valueType)
  {
    if (valueType == null)
      throw new IllegalArgumentException("valueType cannot be null");

    return Number.class.isAssignableFrom(valueType) ||
           valueType == double.class ||
           valueType == float.class ||
           valueType == long.class ||
           valueType == int.class ||
           valueType == short.class ||
           valueType == byte.class;
  }

  /**
   * Convenience method to return the specified value as a double.
   *
   * @param value  Value to represent as a double value.
   *               May be null if this is a no-value.
   * @return       The requested double value.
   */
  public static double getAsDouble(Object value)
  {
    //
    // No-value
    //
    if (value == null)
      return Double.NaN;

    //
    // Number
    //
    if (value instanceof Number) {
      Number v = (Number) value;
      return v.doubleValue();
    }

    //
    // Date
    //
    if (value instanceof Date) {
      Date date = (Date) value;
      return (double) date.getTime();
    }

    //
    // Boolean
    //
    if (value instanceof Boolean) {
      boolean b = (Boolean) value;
      return b ? 1.0 : 0.0;
    }

    //
    // String
    //
    if (value instanceof String) {
      try {
        return Double.parseDouble((String) value);
      }
      catch (NumberFormatException exception) {
        // Ignore. It was possibly not meant to be
        // converted like this.
      }
    }

    //
    // Others
    //
    return (double) value.hashCode();
  }

  /**
   * Return the specified double value as an equivalent
   * object of the specified type.
   *
   * @param value      Value to convert. May be null.
   * @param valueType  Value type to convert to. Non-null.
   * @return           Object of type dataType. May be null.
   * @throws IllegalArgumentException  If valueType is null.
   */
  public static Object getAsType(double value, Class<?> valueType)
  {
    if (valueType == null)
      throw new IllegalArgumentException("valueType cannot be null");

    if (Double.isNaN(value))
      return null;

    if (valueType == Double.class)
      return value;

    if (valueType == Float.class)
      return (float) value;

    if (valueType == Long.class)
      return Math.round(value);

    if (valueType == Integer.class)
      return (int) Math.round(value);

    if (valueType == Date.class)
      return new Date((long) value);

    if (valueType == String.class)
      return "" + value;

    if (valueType == Boolean.class)
      return value != 0.0;

    if (valueType == Short.class)
      return (short) value;

    if (valueType == Byte.class)
      return (byte) value;

    // Others
    return null;
  }

  /**
   * Return the specified value as an object of the given type.
   *
   * @param value      Value to consider. Null if no-value.
   * @param valueType  Type to convert to. Non-null.
   * @return           The requested object. Null if no-value.
   * @throws IllegalArgumentException  If valueType is null.
   */
  public static Object getAsType(Object value, Class<?> valueType)
  {
    if (valueType == null)
      throw new IllegalArgumentException("valueType cannot be null");

    if (value == null)
      return null;

    if (valueType == String.class)
      return value.toString().length() == 0 ? null : value.toString();

    if (valueType == Date.class && value instanceof String) {
      String dateString = value.toString();
      try {
        return ISO8601DateParser.parse(dateString);
      }
      catch (Exception exception) {
        exception.printStackTrace();
        return null;
      }
    }

    if (value.getClass() == valueType)
      return value;

    return getAsType(getAsDouble(value), valueType);
  }
}
