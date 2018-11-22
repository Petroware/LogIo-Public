package no.petroware.logio.json;

import java.util.Date;

import no.petroware.logio.util.Util;

/**
 * A convenience class to hold LAS parameters when LAS files are
 * converted to JSON to avoid that information gets lost.
 * <p>
 * A LAS parameter has a name, unit, value and description.
 * Examples can be:
 * <pre>
 *    SwIrr  .V/V    0.30 : Irreducible Water Saturation
 *    Rshl   .OHMM   2.12 : Resistivity shale
 *    PDAT   .       MSL  : Permanent Datum
 * </pre>
 * <p>
 * Using the {@link JsonLasParameter} class for transition, the information
 * can be represented in JSON as:
 * <pre>
 *   "SwIrr": {
 *     "value": 0.3000,
 *     "unit": "V/V",
 *     "description": "Irreducible Water Saturation"
 *   },
 *   "Rshl": {
 *     "value": 2.12,
 *     "unit": "OHMM",
 *     "description": "Resistivity shale"
 *   },
 *   "PDAT": {
 *     "value": "MSL",
 *     "unit": null,
 *     "description": "Permanent Datum"
 *   }
 * </pre>
 * <p>
 * Note that the <em>informational value</em> of such parameters is low.
 * Some clients may understand the meaning of the names, but in general
 * this information is not fit for further processing.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonLasParameter
{
  /** Parameter name. Non-null. */
  private final String name_;

  /** Parameter value. Null if absent. */
  private final Object value_;

  /** Value unit of measurement. Null if N/A, unitless or unknown. */
  private final String unit_;

  /** Parameter description. Null if none provided. */
  private final String description_;

  /**
   * Create a JSON parameter instance.
   *
   * @param name         The parameter name. Non-null.
   * @param value        The parameter value. Null if absent.
   * @param unit         The value unit. Null if N/A, unitless or not known.
   * @param description  Parameter description. Null if none provided.
   * @throws IllegalArgumentException  If name is null.
   */
  public JsonLasParameter(String name, Object value, String unit, String description)
  {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");

    name_ = name;
    value_ = value;
    unit_ = unit;
    description_ = description;
  }

  /**
   * Create a JSON parameter instance as a copy of the specified one.
   *
   * @param parameter  The parameter to copy. Non-null.
   * @throws IllegalArgumentException  If parameter is null.
   */
  public JsonLasParameter(JsonLasParameter parameter)
  {
    this(parameter.name_, parameter.value_, parameter.unit_, parameter.description_);
  }

  /**
   * Return parameter name.
   *
   * @return  Parameter name. Never null.
   */
  public String getName()
  {
    return name_;
  }

  /**
   * Return parameter value.
   *
   * @return  Parameter value. Null if absent.
   */
  public Object getValue()
  {
    return value_;
  }

  /**
   * Return the parameter value as a double precision decimal number.
   *
   * @return  Parameter value as a double. Null if absent.
   */
  public Double getValueAsDouble()
  {
    return (Double) Util.getAsType(value_, Double.class);
  }

  /**
   * Return the parameter value as a double precision decimal number.
   *
   * @return  Parameter value as a string. Null if absent.
   */
  public String getValueAsString()
  {
    return (String) Util.getAsType(value_, String.class);
  }

  /**
   * Return the parameter value as a double precision decimal number.
   *
   * @return  Parameter value as a date.
   *          Null if absent or the value is not compatible with the date type.
   */
  public Date getValueAsDate()
  {
    return (Date) Util.getAsType(value_, Date.class);
  }

  /**
   * Return parameter unit.
   *
   * @return  Parameter unit. Null if N/A, unitless or unknown.
   */
  public String getUnit()
  {
    return unit_;
  }

  /**
   * Return parameter description.
   *
   * @return  Parameter description. Null if none provided.
   */
  public String getDescription()
  {
    return description_;
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append(name_);
    s.append('=');
    s.append(value_);
    if (unit_ != null)
      s.append(unit_);
    if (description_ != null)
      s.append(" (" + description_ + ")");

    return s.toString();
  }
}
