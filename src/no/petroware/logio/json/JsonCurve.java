package no.petroware.logio.json;

import no.petroware.logio.common.Statistics;
import no.petroware.logio.util.DataArray;
import no.petroware.logio.util.Util;

/**
 * Model a log curve as it is defined by JSON.
 * <p>
 * A log curve consist of measurement data of a specific type.
 * The curve may have one or more dimensions.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonCurve
{
  /** Name of this curve. Never null. */
  private final String name_;

  /** Description of curve. May be null if not provided. */
  private final String description_;

  /** Quantity of the curve data. Null if unknown or N/A. */
  private final String quantity_;

  /** Unit of this curve. May be null indicating unitless. */
  private final String unit_;

  /** Value type of the data of this curve. */
  private final Class<?> valueType_;

  /** Number of "dimensions" or directions that have been measured. [1,&gt;. */
  private final int nDimensions_;

  /** The curve values. Array of nDimensions. */
  private final DataArray[] values_;

  /** Curve statistics. */
  private final Statistics statistics_ = new Statistics();

  /**
   * Create a JSON curve instance.
   *
   * @param name                Name (mnemonic) of curve. Non-null.
   * @param description         Curve long name or description. May be null
   *                            if not provided.
   * @param quantity            Quantity of the curve data. Null if unknown or N/A.
   * @param unit                Unit of measure for the curve data. Null if unitless.
   * @param valueType           Value type of curve data.
   * @param nDimensions         Dimension of curve. &lt;0,&gt;.
   * @throws IllegalArgumentException  If name or valueType is or nDimensions is out of bounds.
   */
  public JsonCurve(String name, String description, String quantity, String unit,
                   Class<?> valueType, int nDimensions)
  {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");

    if (valueType == null)
      throw new IllegalArgumentException("valueType cannot be null");

    if (nDimensions <= 0)
      throw new IllegalArgumentException("Invalid nDimensions: " + nDimensions);

    name_ = name;
    description_ = description;
    quantity_ = quantity;
    unit_ = unit;
    valueType_ = valueType;
    nDimensions_ = nDimensions;

    values_ = new DataArray[nDimensions_];
    for (int i = 0; i < nDimensions_; i++)
      values_[i] = new DataArray(valueType_);
  }

  /**
   * Return name of this curve.
   *
   * @return  Name of this curve. Never null.
   */
  public String getName()
  {
    return name_;
  }

  /**
   * Return quantity of the data of this curve.
   *
   * @return  Quantity of the curve data. Null if unitless.
   */
  public String getQuantity()
  {
    return quantity_;
  }


  /**
   * Return unit of measure of the data of this curve.
   *
   * @return  Unit of measure of the curve data. Null if unitless.
   */
  public String getUnit()
  {
    return unit_;
  }

  /**
   * Return description of this curve.
   *
   * @return  Description of this curve. Null if not provided.
   */
  public String getDescription()
  {
    return description_;
  }

  /**
   * Return the value type for the data of this curve,
   * typically Double.class, Integer.class, String.class, etc.
   *
   * @return Value type for the data of this curve.
   */
  public Class<?> getValueType()
  {
    return valueType_;
  }

  /**
   * Return the number of dimensions of this curve.
   *
   * @return  Number of dimensions of this curve. [1,&gt;.
   */
  public int getNDimensions()
  {
    return nDimensions_;
  }

  /**
  /**
   * Add a value to this curve.
   *
   * @param dimension  Dimension index. [0,nDimensions&gt;.
   * @param value      Value to add. Null to indicate absent.
   * @throws IllegalArgumentException  If sampleNo or dimension is out of bounds.
   */
  public void addValue(int dimension, Object value)
  {
    if (dimension < 0 || dimension >= nDimensions_)
      throw new IllegalArgumentException("Invalid dimension: " + dimension);

    values_[dimension].add(value);
  }

  /**
   * Add a value to this curve. If this is a multi-dimensional
   * curve, the value is added to the first dimension.
   *
   * @param value  Value to add. Null indicates absent.
   */
  public void addValue(Object value)
  {
    addValue(0, value);
  }

  /**
   * Return the number of values in this curve.
   *
   * @return  Number of values in this curve. [0,&gt;.
   */
  public int getNValues()
  {
    return values_[0].size();
  }

  /**
   * Return the number of values in the specified dimension.
   *
   * @param dimension  Dimension to check. [0,nDimension&gt;.
   * @return           Number of values in the specified dimension. [0,&gt;.
   */
  int getNValues(int dimension)
  {
    assert dimension >= 0 && dimension < nDimensions_ : "Invalid dimenion: " + dimension;
    return values_[dimension].size();
  }

  /**
   * Return a specific value from the given dimension of this curve.
   *
   * @param dimension  Dimension index. [0,nDimensions&gt;.
   * @param index      Position index. [0,nValues&gt;.
   * @return           The requested value. Null if absent.
   * @throws IllegalArgumentException  If sampleNo, dimension or index is out of bounds.
   */
  public Object getValue(int dimension, int index)
  {
    if (dimension < 0 || dimension >= nDimensions_)
      throw new IllegalArgumentException("Invalid dimension: " + dimension);

    return values_[dimension].get(index);
  }

  /**
   * Return a specific value from this curve. If this is a multi-dimensional
   * curve, the value is retrieved from the first dimension.
   * If there are multiple samples per depth, the first sample is returned.
   *
   * @param index  Position index. [0,nValues&gt;.
   * @return       The requested value. Null if absent.
   * @throws IllegalArgumentException  If index is out of bounds.
   */
  public Object getValue(int index)
  {
    return getValue(0, index);
  }

  /**
   * Return the range (i.e the min and max value) of this curve.
   * The returned array is never null. The two entries may
   * be null if min/max does not exist.
   * <p>
   * If the curve is multi-dimensional, the range is reported
   * across all dimensions.
   *
   * @return  The range of this curve as an array of two (min/max).
   *          Never null. The entries may be null if no range exists.
   */
  public Object[] getRange()
  {
    double minValue = Double.NaN;
    double maxValue = Double.NaN;

    int nValues = getNValues();
    for (int dimensionNo = 0; dimensionNo < nDimensions_; dimensionNo++) {
      for (int index = 0; index < nValues; index++) {
        double v = Util.getAsDouble(getValue(dimensionNo, index));
        if (Double.isNaN(minValue) || v < minValue)
          minValue = v;
        if (Double.isNaN(maxValue) || v > maxValue)
          maxValue = v;
      }
    }

    return new Object[] {Util.getAsType(minValue, valueType_),
                         Util.getAsType(maxValue, valueType_)};
  }

  /**
   * Return curve statistics. Statistics is available even if log
   * data has not been stored.
   *
   * @return  Curve statistics. Never null.
   */
  public Statistics getStatistics()
  {
    return statistics_;
  }

  /**
   * Remove all values from this curve.
   */
  public void clear()
  {
    for (int dimension = 0; dimension < values_.length; dimension++)
      values_[dimension].clear();
    statistics_.reset();
  }

  /**
   * Trim all curve lists to their actual dimension to save memory.
   * The assumption is that the lists are complete and will not grow
   * any further.
   */
  void trim()
  {
    for (int i = 0; i < nDimensions_; i++)
      values_[i].trim();
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append("Name..........: " + name_ + "\n");
    s.append("Unit..........: " + unit_ + "\n");
    s.append("Description...: " + description_ + "\n");
    s.append("Value type....: " + valueType_ + "\n");
    s.append("N dimensions..: " + nDimensions_ + "\n");
    s.append("N values......: " + getNValues() + "\n");
    s.append("Values........: ");
    for (int dimension = 0; dimension < getNDimensions(); dimension++) {
      for (int index = 0; index < getNValues(); index++) {
        s.append(getValue(dimension, index) + ", ");
        if (index == 10)
          break;
      }
      s.append("\n");
    }
    return s.toString();
  }
}
