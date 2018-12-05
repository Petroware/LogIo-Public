package no.petroware.logio.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;

import no.petroware.logio.util.Formatter;
import no.petroware.logio.util.ISO8601DateParser;
import no.petroware.logio.util.Util;

/**
 * Class representing the content of a JSON well log (sub-) file.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonFile
{
  /**
   * All the metadata captured as a single JSON object.
   */
  private JsonObject metadata_;

  /**
   * The curves of this JSON file.
   */
  private final List<JsonCurve> curves_ = new CopyOnWriteArrayList<>();

  /** Indicate if this instance includes curve data or not. */
  private boolean hasCurveData_;

  /**
   * Create a new JSON file instance.
   *
   * @param hasCurveData  Indicate if the file includes curve data.
   */
  JsonFile(boolean hasCurveData)
  {
    hasCurveData_ = hasCurveData;
  }

  /**
   * Create an empty JSON well log file.
   */
  public JsonFile()
  {
    this(true); // It has all the curve data that exists (none)

    // Default empty metadata
    metadata_ = Json.createObjectBuilder().build();
  }

  /**
   * Return whether the JSON file instance includes curve data
   * or not, i.e if only header data was read or created.
   *
   * @return  True if bulk (curve) data is present, false otherwise.
   */
  public boolean hasCurveData()
  {
    return hasCurveData_;
  }

  /**
   * Set the metadata of this instance.
   *
   * @param metadata  JSON metadata object. Non-null.
   */
  void setMetadata(JsonObject metadata)
  {
    assert metadata != null : "metadata cannot be null";

    synchronized (this) {
      metadata_ = metadata;
    }
  }

  /**
   * Return the metadata of this instance as a single JSON object.
   *
   * @return  Metadata of this JSON file. Never null.
   */
  JsonObject getMetadata()
  {
    synchronized (this) {
      return metadata_;
    }
  }

  /**
   * Set a string metadata property of this JSON file.
   *
   * @param key    Key of property to set. Non-null.
   * @param value  Associated value. Null to unset.
   * @throws IllegalArgumentException  If key is null.
   */
  public void setProperty(String key, String value)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    metadata_.forEach(objectBuilder::add);

    if (value == null)
      objectBuilder.addNull(key);
    else
      objectBuilder.add(key, value);

    setMetadata(objectBuilder.build());
  }

  /**
   * Set a floating point numeric metadata property of this JSON file.
   *
   * @param key    Key of property to set. Non-null.
   * @param value  Associated value. Null to unset.
   * @throws IllegalArgumentException  If key is null.
   */
  public void setProperty(String key, Double value)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    metadata_.forEach(objectBuilder::add);

    if (value == null)
      objectBuilder.addNull(key);
    else
      objectBuilder.add(key, value);

    setMetadata(objectBuilder.build());
  }

  /**
   * Set a integer numeric metadata property of this JSON file.
   *
   * @param key    Key of property to set. Non-null.
   * @param value  Associated value. Null to unset.
   * @throws IllegalArgumentException  If key is null.
   */
  public void setProperty(String key, Integer value)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    metadata_.forEach(objectBuilder::add);

    if (value == null)
      objectBuilder.addNull(key);
    else
      objectBuilder.add(key, value);

    setMetadata(objectBuilder.build());
  }

  /**
   * Set a boolean metadata property of this JSON file.
   *
   * @param key    Key of property to set. Non-null.
   * @param value  Associated value. Null to unset.
   * @throws IllegalArgumentException  If key is null.
   */
  public void setProperty(String key, Boolean value)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    metadata_.forEach(objectBuilder::add);

    if (value == null)
      objectBuilder.addNull(key);
    else
      objectBuilder.add(key, value);

    setMetadata(objectBuilder.build());
  }

  /**
   * Set a date metadata property of this JSON file.
   *
   * @param key    Key of property to set. Non-null.
   * @param value  Associated value. Null to unset.
   * @throws IllegalArgumentException  If key is null.
   */
  public void setProperty(String key, Date value)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    metadata_.forEach(objectBuilder::add);

    if (value == null)
      objectBuilder.addNull(key);
    else
      objectBuilder.add(key, ISO8601DateParser.toString(value));

    setMetadata(objectBuilder.build());
  }

  /**
   * Add a LAS type parameter to this JSON file.
   * <p>
   * This method is typically used when converting LAS files to
   * JSON to make sure information is not lost. In general one
   * should be careful adding properties like these as their
   * <em>information value</em> is low. There is very limited
   * possibility to further process information that is not tagged
   * or dictionary controlled.
   *
   * @param lasParameter  Parameter to add. Non-null.
   * @throws IllegalArgumentException  If lasParameter is null.
   */
  public void addLasParameter(JsonLasParameter lasParameter)
  {
    if (lasParameter == null)
      throw new IllegalArgumentException("lasParameter cannot be null");

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    metadata_.forEach(objectBuilder::add);

    Object value = lasParameter.getValue();

    JsonObjectBuilder lasParameterBuilder = Json.createObjectBuilder();
    if (value == null)
      lasParameterBuilder.addNull("value");
    else if (value instanceof Double)
      lasParameterBuilder.add("value", (double) value);
    else if (value instanceof Integer)
      lasParameterBuilder.add("value", (int) value);
    else if (value instanceof Date)
      lasParameterBuilder.add("value", ISO8601DateParser.toString((Date) value));
    else if (value instanceof Boolean)
      lasParameterBuilder.add("value", (boolean) value);
    else
      lasParameterBuilder.add("value", value.toString());

    String unit = lasParameter.getUnit();
    if (unit != null)
      lasParameterBuilder.add("unit", unit);
    else
      lasParameterBuilder.addNull("unit");

    String description = lasParameter.getDescription();
    if (description != null)
      lasParameterBuilder.add("description", description);
    else
      lasParameterBuilder.addNull("description");


    objectBuilder.add(lasParameter.getName(), lasParameterBuilder);

    setMetadata(objectBuilder.build());
  }

  /**
   * Return metadata property for the specified key as a <em>LAS parameter</em>.
   * <p>
   * This feature is typically used when a JSON file has been converted from LAS.
   * See {@link JsonLasParameter} for more information.
   *
   * @param name  Name of LAS parameter to get. Non-null.
   * @return      The associated JSON object as a LAS parameter.
   *              Null if not found, or not compatible with the LAS parameter
   *              type.
   * @throws IllegalArgumentException  If name is null.
   */
  public JsonLasParameter getLasParameter(String name)
  {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    Object object = JsonUtil.findObject(jsonParser, name);
    jsonParser.close();

    if (object instanceof JsonObject) {
      jsonParser = Json.createParserFactory(null).createParser((JsonObject) object);
      jsonParser.next(); // Proceed past the first START_OBJECT

      Object valueObject = JsonUtil.findObject(jsonParser, "value");
      Object value = valueObject;

      Object unitObject = JsonUtil.findObject(jsonParser, "unit");
      String unit = unitObject != null ? unitObject.toString() : null;

      Object descriptionObject = JsonUtil.findObject(jsonParser, "description");
      String description = descriptionObject != null ? descriptionObject.toString() : null;

      jsonParser.close();

      return new JsonLasParameter(name, value, unit, description);
    }

    return null;
  }

  /**
   * Add a DLIS type set to this JSON file.
   * <p>
   * This method is typically used when converting DLIS files to
   * JSON to make sure information is not lost. In general one
   * should be careful adding properties like these as their
   * <em>information value</em> is low. There is very limited
   * possibility to further process information that is not tagged
   * or dictionary controlled.
   *
   * @param dlisSet  Set to add. Non-null.
   * @throws IllegalArgumentException  If dlisSet is null.
   */
  public void addDlisSet(JsonDlisSet dlisSet)
  {
    if (dlisSet == null)
      throw new IllegalArgumentException("dlisSet cannot be null");

    // Root builder
    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
    metadata_.forEach(objectBuilder::add);

    // Attribute builder
    JsonArrayBuilder attributeBuilder = Json.createArrayBuilder();
    for (String attributeName : dlisSet.getAttributes())
      attributeBuilder.add(attributeName);

    /*
    // Objects builder
    JsonArrayBuilder objectsBuilder = Json.createArrayBuilder();
    for (String objectName : dlisSet.getObjects()) {
      JsonArrayBuilder objectBuilder = Json.createArrayBuilder();


    }
    */


    JsonObjectBuilder dlisSetBuilder = Json.createObjectBuilder();
    dlisSetBuilder.add("attributes", attributeBuilder);

    objectBuilder.add(dlisSet.getName(), dlisSetBuilder);

    setMetadata(objectBuilder.build());
  }

  /**
   * Return metadata property for the specified key as a <em>DLIS set</em>.
   * <p>
   * This feature is typically used when a JSON file has been converted from DLIS.
   * See {@link JsonDlisSet} for more information.
   *
   * @param name  Name of LAS parameter to get. Non-null.
   * @return      The associated JSON object as a LAS parameter.
   *              Null if not found, or not compatible with the LAS parameter
   *              type.
   * @throws IllegalArgumentException  If name is null.
   */
  public JsonDlisSet getDlisSet(String name)
  {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");

    // TODO

    return null;
  }

  /**
   * Return all the metadata property keys of this JSON file.
   *
   * @return  All property keys of this JSON file. Never null.
   */
  public List<String> getProperties()
  {
    List<String> properties = new ArrayList<>();

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      // Capture keys
      if (parseEvent == JsonParser.Event.KEY_NAME)
        properties.add(jsonParser.getString());

      // Proceed past complete objects
      else if (parseEvent == JsonParser.Event.START_OBJECT)
        JsonUtil.readJsonObject(jsonParser);
    }

    jsonParser.close();

    return properties;
  }

  /**
   * Return metadata property for the specified key as a string.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a string. Null if not found, or
   *             not compatible with the string type.
   * @throws IllegalArgumentException  If key is null.
   */
  public String getPropertyAsString(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    Object object = JsonUtil.findObject(jsonParser, key);
    jsonParser.close();

    // Since Util.getAsType() return null for empty string
    if (object instanceof String && object.toString().isEmpty())
      return "";

    return (String) Util.getAsType(object, String.class);
  }

  /**
   * Return metadata property for the specified key as a double.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a double. Null if not found, or
   *             not compatible with the double type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Double getPropertyAsDouble(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    Object object = JsonUtil.findObject(jsonParser, key);
    jsonParser.close();

    return (Double) Util.getAsType(object, Double.class);
  }

  /**
   * Return metadata property for the specified key as an integer.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as an integer. Null if not found, or
   *             not compatible with the integer type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Integer getPropertyAsInteger(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    Object object = JsonUtil.findObject(jsonParser, key);
    jsonParser.close();

    return (Integer) Util.getAsType(object, Integer.class);
  }

  /**
   * Return metadata property for the specified key as a boolean.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a boolean. Null if not found, or
   *             not compatible with the boolean type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Boolean getPropertyAsBoolean(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    Object object = JsonUtil.findObject(jsonParser, key);
    jsonParser.close();

    return (Boolean) Util.getAsType(object, Boolean.class);
  }

  /**
   * Return metadata property for the specified key as date.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value as a date. Null if not found, or
   *             not compatible with the date type.
   * @throws IllegalArgumentException  If key is null.
   */
  public Date getPropertyAsDate(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    Object object = JsonUtil.findObject(jsonParser, key);
    jsonParser.close();

    return (Date) Util.getAsType(object, Date.class);
  }

  /**
   * Return metadata property for the specified key.
   * <p>
   * This is a generic method for clients that add or know about custom content
   * of the JSON well log file. It is up to the client program to parse the returned
   * content into the appropriate type.
   *
   * @param key  Key of property to get. Non-null.
   * @return     The associated value, or null if not found.
   * @throws IllegalArgumentException  If key is null.
   */
  public Object getProperty(String key)
  {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(metadata_);
    jsonParser.next(); // Proceed past the first START_OBJECT

    Object object = JsonUtil.findObject(jsonParser, key);
    jsonParser.close();

    return object;
  }

  /**
   * Return name of the log of this file.
   *
   * @return  Name of the log of this file. Null if none provided.
   */
  public String getName()
  {
    return getPropertyAsString(JsonWellLogProperty.NAME.getKey());
  }

  /**
   * Set name of the log of this JSON file.
   *
   * @param name  Name to set. Null to unset.
   */
  public void setName(String name)
  {
    setProperty(JsonWellLogProperty.NAME.getKey(), name);
  }

  /**
   * Get description of the log of this JSON file.
   *
   * @return  Description of the log of this JSON file. Null if none provided.
   */
  public String getDescription()
  {
    return getPropertyAsString(JsonWellLogProperty.DESCRIPTION.getKey());
  }

  /**
   * Set description of the log of this JSON file.
   *
   * @param description  Description to set. Null to unset.
   */
  public void setDescription(String description)
  {
    setProperty(JsonWellLogProperty.DESCRIPTION.getKey(), description);
  }

  /**
   * Return well name of the log of this JSON file.
   *
   * @return  Well name of the log of this JSON file. Null if none provided.
   */
  public String getWell()
  {
    return getPropertyAsString(JsonWellLogProperty.WELL.getKey());
  }

  /**
   * Set well name of the log of this JSON file.
   *
   * @param well  Well name to set. Null to unset.
   */
  public void setWell(String well)
  {
    setProperty(JsonWellLogProperty.WELL.getKey(), well);
  }

  /**
   * Return wellbore name of the log of this JSON file.
   *
   * @return  Wellbore name of the log of this JSON file. Null if none provided.
   */
  public String getWellbore()
  {
    return getPropertyAsString(JsonWellLogProperty.WELLBORE.getKey());
  }

  /**
   * Set wellbore name of the log of this JSON file.
   *
   * @param wellbore  Wellbore name to set. Null to unset.
   */
  public void setWellbore(String wellbore)
  {
    setProperty(JsonWellLogProperty.WELLBORE.getKey(), wellbore);
  }

  /**
   * Return field name of the log of this JSON file.
   *
   * @return  Field name of the log of this JSON file. Null if none provided.
   */
  public String getField()
  {
    return getPropertyAsString(JsonWellLogProperty.FIELD.getKey());
  }

  /**
   * Set field name of the log of this JSON file.
   *
   * @param field  Field name to set. Null to unset.
   */
  public void setField(String field)
  {
    setProperty(JsonWellLogProperty.FIELD.getKey(), field);
  }

  /**
   * Return country of the log of this JSON file.
   *
   * @return  Country of the log of this JSON file. Null if none provided.
   */
  public String getCountry()
  {
    return getPropertyAsString(JsonWellLogProperty.COUNTRY.getKey());
  }

  /**
   * Set country of the log of this JSON file.
   *
   * @param country  Country to set. Null to unset.
   */
  public void setCountry(String country)
  {
    setProperty(JsonWellLogProperty.COUNTRY.getKey(), country);
  }

  /**
   * Return logging date of the log of this JSON file.
   *
   * @return  Logging date of the log of this JSON file. Null if none provided.
   */
  public Date getDate()
  {
    return getPropertyAsDate(JsonWellLogProperty.DATE.getKey());
  }

  /**
   * Set logging date of the log of this JSON file.
   *
   * @param date  Logging date to set. Null to unset.
   */
  public void setDate(Date date)
  {
    setProperty(JsonWellLogProperty.DATE.getKey(), date);
  }

  /**
   * Return operator name of the log of this JSON file.
   *
   * @return  Operator name of the log of this JSON file. Null if none provided.
   */
  public String getOperator()
  {
    return getPropertyAsString(JsonWellLogProperty.OPERATOR.getKey());
  }

  /**
   * Set operator name of the log of this JSON file.
   *
   * @param operator  Operator name of the log of this JSON file. Null to unset.
   */
  public void setOperator(String operator)
  {
    setProperty(JsonWellLogProperty.OPERATOR.getKey(), operator);
  }

  /**
   * Return service company name of the log of this JSON file.
   *
   * @return  Service company name of the log of this JSON file. Null if none provided.
   */
  public String getServiceCompany()
  {
    return getPropertyAsString(JsonWellLogProperty.SERVICE_COMPANY.getKey());
  }

  /**
   * Set service company name of the log of this JSON file.
   *
   * @param serviceCompany  Service company name of the log of this JSON file. Null to unset.
   */
  public void setServiceCompany(String serviceCompany)
  {
    setProperty(JsonWellLogProperty.SERVICE_COMPANY.getKey(), serviceCompany);
  }

  /**
   * Return run number of the log of this JSON file.
   *
   * @return  Run number of the log of this JSON file. Null if none provided.
   */
  public String getRunNumber()
  {
    return getPropertyAsString(JsonWellLogProperty.RUN_NUMBER.getKey());
  }

  /**
   * Set run number of the log of this JSON file.
   *
   * @param runNumber  Run number of the log of this JSON file. Null to unset.
   */
  public void setRunNumber(String runNumber)
  {
    setProperty(JsonWellLogProperty.RUN_NUMBER.getKey(), runNumber);
  }

  /**
   * Return value type of the index of the log of this JSON file, typically Double.class
   * or Date.class.
   *
   * @return Value type of the index of the log of this JSON file. Never null.
   *         If the log has no curves, Double.class is returned.
   */
  public Class<?> getIndexValueType()
  {
    return curves_.isEmpty() ? Double.class : curves_.get(0).getValueType();
  }

  /**
   * Return start index of the log of this JSON file.
   * <p>
   * <b>NOTE: </b> This property is taken from metadata, and may not necessarily
   * be in accordance with the <em>actual</em> data on the file.
   *
   * @return Start index of the log of this JSON file. The type will be according to
   *         the type of the index curve, @see #getIndexValueType.
   */
  public Object getStartIndex()
  {
    Class<?> indexValueType = getIndexValueType();
    if (indexValueType == Date.class)
      return getPropertyAsDate(JsonWellLogProperty.START_INDEX.getKey());

    return getPropertyAsDouble(JsonWellLogProperty.START_INDEX.getKey());
  }

  /**
   * Return the <em>actual</em> start index of the log of this JSON file.
   *
   * @return  The actual start index. Null if the log has no values.
   */
  public Object getActualStartIndex()
  {
    JsonCurve indexCurve = !curves_.isEmpty() ? curves_.get(0) : null;
    int nValues = indexCurve != null ? indexCurve.getNValues() : 0;
    return nValues > 0 ? indexCurve.getValue(0) : null;
  }

  /**
   * Set start index of the log of this JSON file.
   *
   * @param startIndex  Start index to set. Null to unset. The type should
   *                    be in accordance with the actual type of the index curve
   *                    of the file.
   */
  public void setStartIndex(Object startIndex)
  {
    if (startIndex instanceof Date)
      setProperty(JsonWellLogProperty.START_INDEX.getKey(), (Date) startIndex);
    else
      setProperty(JsonWellLogProperty.START_INDEX.getKey(), Util.getAsDouble(startIndex));
  }

  /**
   * Return end index of the log of this JSON file.
   * <p>
   * <b>NOTE: </b> This property is taken from metadata, and may not necessarily
   * be in accordance with the <em>actual</em> data on the file.
   *
   * @return End index of the log of this JSON file. The type will be according to
   *         the type of the index curve, @see #getIndexValueType.
   */
  public Object getEndIndex()
  {
    Class<?> indexValueType = getIndexValueType();
    if (indexValueType == Date.class)
      return getPropertyAsDate(JsonWellLogProperty.END_INDEX.getKey());

    return getPropertyAsDouble(JsonWellLogProperty.END_INDEX.getKey());
  }

  /**
   * Return the <em>actual</em> end index of the log of this JSON file.
   *
   * @return  The actual end index. Null if the log has no values.
   */
  public Object getActualEndIndex()
  {
    JsonCurve indexCurve = !curves_.isEmpty() ? curves_.get(0) : null;
    int nValues = indexCurve != null ? indexCurve.getNValues() : 0;
    return nValues > 0 ? indexCurve.getValue(nValues - 1) : null;
  }

  /**
   * Set end index of the log of this JSON file.
   *
   * @param endIndex  End index to set. Null to unset. The type should
   *                  be in accordance with the actual type of the index curve
   *                  of the file.
   */
  public void setEndIndex(Object endIndex)
  {
    if (endIndex instanceof Date)
      setProperty(JsonWellLogProperty.END_INDEX.getKey(), (Date) endIndex);
    else
      setProperty(JsonWellLogProperty.END_INDEX.getKey(), Util.getAsDouble(endIndex));
  }

  /**
   * Return the regular step of the log of this JSON file.
   * <p>
   * <b>NOTE: </b> This property is taken from metadata, and may not necessarily
   * be in accordance with the <em>actual</em> data on the file.
   *
   * @return The step of the index curve of the log of this JSON file.
   *         Null should indicate that the log in irregular or the step is unknown.
   */
  public Double getStep()
  {
    return getPropertyAsDouble(JsonWellLogProperty.STEP.getKey());
  }

  /**
   * Return the <em>actual</em> step of the index curve of this JSON file.
   *
   * @return  The actual step of the index curve.
   *          Null if the JSON file has no data or the log set is irregular.
   */
  public Double getActualStep()
  {
    return JsonUtil.computeStep(this);
  }

  /**
   * Set the regular step of the index curve of the log of this JSON file.
   *
   * @param step  Step to set. Null to indicate unknown or that the log set is irregular.
   *              If the log set is time based, the step should as a convention be the number
   *              of milliseconds between samples.
   */
  public void setStep(Double step)
  {
    setProperty(JsonWellLogProperty.STEP.getKey(), step);
  }

  /**
   * Add the specified curve to this JSON file.
   *
   * @param curve  Curve to add. Non-null.
   * @throws IllegalArgumentException  If curve is null.
   */
  public void addCurve(JsonCurve curve)
  {
    if (curve == null)
      throw new IllegalArgumentException("curve cannot be null");

    curves_.add(curve);
  }

  /**
   * Return the curves of this JSON file. The first curve
   * is by convention always the index curve.
   *
   * @return  The curves of this JSON file instance. Never null.
   */
  public List<JsonCurve> getCurves()
  {
    return Collections.unmodifiableList(curves_);
  }

  /**
   * Replace the present set of curves.
   * <p>
   * This method is called by the reader to populate a JsonFile instance
   * that initially was read without bulk data.
   *
   * @param curves  Curves to set. Non-null.
   */
  void setCurves(List<JsonCurve> curves)
  {
    assert curves != null : "curves cannot be null";

    // TODO: Not thread safe. Need an atomic replacement for these two
    curves_.clear();
    curves_.addAll(curves);

    hasCurveData_ = true;
  }

  /**
   * Return the number of curves in this JSON file.
   *
   * @return  Number of curves in this JSON file. [0,&gt;.
   */
  public int getNCurves()
  {
    return curves_.size();
  }

  /**
   * Return the number of values in this JSON file.
   *
   * @return  Number of curves in this JSON file. [0,&gt;.
   */
  public int getNValues()
  {
    return curves_.isEmpty() ? 0 : curves_.get(0).getNValues();
  }

  /**
   * Return the index curve of this JSON file.
   *
   * @return  The index curve of this JSON file, or null if the
   *          JSON file doesn't contain any curves.
   */
  public JsonCurve getIndexCurve()
  {
    return getNCurves() > 0 ? getCurves().get(0) : null;
  }

  /**
   * Clear curve data from all curves of this JSON file.
   */
  public void clearCurves()
  {
    for (JsonCurve curve : curves_)
      curve.clear();
  }

  /**
   * Set curve capacity to actual size to save memory.
   * The assumption is that the curves will not grow any further.
   */
  void trimCurves()
  {
    for (JsonCurve curve : curves_)
      curve.trim();
  }

  /**
   * Return number of significant digits to use to properly represent
   * the values of the specified curve.
   *
   * @param curve  JSON curve to consider. Non-null.
   * @return       The number of significant digits to use for the
   *               specified curve. [0,&gt;.
   */
  private int getNSignificantDigits(JsonCurve curve)
  {
    Class<?> valueType = curve.getValueType();

    if (valueType != Double.class && valueType != Float.class)
      return 0;

    if (curve.getNValues() == 0)
      return 0;

    double step = JsonUtil.computeStep(this);
    if (step == 0.0)
      return 6;

    Object[] range = curve.getRange();
    if (range[0] == null || range[1] == null)
      return 6;

    double minValue = Util.getAsDouble(range[0]);
    double maxValue = Util.getAsDouble(range[1]);

    double max = Math.max(Math.abs(minValue), Math.abs(maxValue));

    int nDigits = (int) Math.round(Math.abs(Math.log10(max)) + 0.5);
    int nDecimals = Util.countDecimals(step);

    int nSignificantDigits = nDigits + nDecimals;

    return Math.min(nSignificantDigits, 10);
  }

  /**
   * Create a formatter for the data of the specified curve.
   *
   * @param curve         Curve to create formatter for. Non-null.
   * @param isIndexCurve  True if curve is the index curve, false otherwise.
   * @return  A formatter that can be used to write the curve data.
   *                      Null if the log data is not of numeric type.
   */
  Formatter createFormatter(JsonCurve curve, boolean isIndexCurve)
  {
    assert curve != null : "curve cannot be null";

    Class<?> valueType = curve.getValueType();
    if (valueType != Double.class && valueType != Float.class)
      return null;

    Integer nDecimals = valueType == Double.class || valueType == Float.class ? null : 0;

    int nDimensions = curve.getNDimensions();
    int nValues = curve.getNValues();

    double[] values = new double[nValues * nDimensions];

    for (int index = 0; index < nValues; index++)
      for (int dimension = 0; dimension < nDimensions; dimension++)
        values[dimension * nValues + index] = Util.getAsDouble(curve.getValue(dimension, index));

    int nSignificantDigits = isIndexCurve ? getNSignificantDigits(curve) : 6;

    return new Formatter(values, nSignificantDigits, nDecimals, null);
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append("-- JSON file\n");

    s.append("Metadata:\n");
    for (String property : getProperties())
      System.out.println(property + ": " + getProperty(property));

    for (JsonCurve curve : curves_)
      s.append(curve + "\n");

    return s.toString();
  }
}
