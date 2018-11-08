package no.petroware.logio.json;

/**
 * List the well known properties of JSON well log files.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public enum JsonWellLogProperty
{
  /** Log name. */
  NAME("name"),

  /** Log description. */
  DESCRIPTION("description"),

  /** Well name. */
  WELL("well"),

  /** Unique well ID. */
  WELL_ID("wellId"),

  /** Wellbore name. */
  WELLBORE("wellbore"),

  /** Field name. */
  FIELD("field"),

  /** Country name. */
  COUNTRY("country"),

  /** Logging date. */
  DATE("date"),

  /** Operator company name. */
  OPERATOR("operator"),

  /** Service company name. */
  SERVICE_COMPANY("serviceCompany"),

  /** Run number. */
  RUN_NUMBER("runNumber"),

  /** Start index. */
  START_INDEX("startIndex"),

  /** End index. */
  END_INDEX("endIndex"),

  /** Step if regular sampling. */
  STEP("step");

  /** Tag used when the property is written to file. Non-null. */
  private final String tag_;

  /**
   * Create a well known well log property entry.
   *
   * @param tag  Tag as when written to file. Non-null.
   */
  private JsonWellLogProperty(String tag)
  {
    assert tag != null : "tag cannot be null";
    tag_ = tag;
  }

  /**
   * Return tag of this property.
   *
   * @return Tag of this property. Never null.
   */
  public String getTag()
  {
    return tag_;
  }

  /**
   * Get property for the specified tag.
   *
   * @param tag  Tag to get property of. Non-null.
   * @return     The associated property, or null if not found.
   * @throws IllegalArgumentException  If tag is null.
   */
  public static JsonWellLogProperty getByTag(String tag)
  {
    if (tag == null)
      throw new IllegalArgumentException("tag cannot be null");

    for (JsonWellLogProperty property : JsonWellLogProperty.values()) {
      if (property.getTag().equals(tag))
        return property;
    }

    // Not found
    return null;
  }


  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    return tag_;
  }
}
