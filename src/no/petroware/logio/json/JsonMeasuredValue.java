package no.petroware.logio.json;

/**
 * A convenience class to capture an JSON object consisting
 * a of a measured value with a corresponding unit, like:
 *
 * <pre>
 *   "pressure": {"value": 250.12, "unit": "bar"}
 * </pre>
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonMeasuredValue
{
  /** The measured value. Null if absent. */
  private final Double value_;

  /** The corresponding unit. Null if unitless or unknown. */
  private final String unit_;

  /**
   * Create a JSON measured value instance.
   *
   * @param value  The measured value. Null if absent.
   * @param unit   The corresponding unit. Null if unitless or unknown.
   */
  public JsonMeasuredValue(Double value, String unit)
  {
    value_ = value;
    unit_ = unit;
  }

  /**
   * Create a JSON measured value instance as a copy of the specified one.
   *
   * @param measuredValue  The measured value to copy. Non-null.
   * @throws IllegalArgumentException  If measuredValue is null.
   */
  public JsonMeasuredValue(JsonMeasuredValue measuredValue)
  {
    this(measuredValue.value_, measuredValue.unit_);
  }

  /**
   * Return the measured value of this instance.
   *
   * @return  The measured value of this instance. Null if absent.
   */
  public Double getValue()
  {
    return value_;
  }

  /**
   * Return the unit of this instance.
   *
   * @return  The unit of this instance. Null if unitless or unknown.
   */
  public String getUnit()
  {
    return unit_;
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    if (value_ != null) {
      s.append(value_);
      if (unit_ != null)
        s.append(unit_);
    }
    return s.toString();
  }
}
