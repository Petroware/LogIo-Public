package no.petroware.logio.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

import no.petroware.logio.util.Util;

/**
 * Class for validating JSON Well Log Format files.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonValidator
{
  /**
   * Represents the different message types created during validation.
   *
   * @author <a href="mailto:info@petroware.no">Petroware AS</a>
   */
  public final static class Message
  {
    /**
     * Severity level associated with message.
     */
    public enum Level
    {
      /** An irrecoverable severe error. The JSON file cannot be properly read. */
      SEVERE,

      /** A recoverable format error. */
      WARNING,

      /** General information about questionable issues. */
      INFO;
    }

    /** Severity level. Non-null. */
    private final Level level_;

    /** Location in stream. null if N/A. */
    private final JsonLocation location_;

    /** The associated textual message. */
    private final String message_;

    /**
     * Create a validation message.
     *
     * @param level     Severity level.
     * @param location  Stream location. Null if N/A.
     * @param message    Textual messga.e Non-null.
     */
    Message(Level level, JsonLocation location, String message)
    {
      assert level != null : "level cannot be null";
      assert message != null : "message cannot be null";

      level_ = level;
      location_ = location;
      message_ = message;
    }

    /**
     * Return the severity level of this message.
     *
     * @return Severity level of this message. Never null.
     */
    public Level getLevel()
    {
      return level_;
    }

    /**
     * Return the text of this message.
     *
     * @return Text of this message. Never null.
     */
    public String getMessage()
    {
      return message_;
    }

    /**
     * Return stream line number of this message.
     *
     * @return  Stream line number of this message.
     *          -1 if not associated with a particular line number.
     */
    public long getLineNumber()
    {
      return location_ != null ? location_.getLineNumber() : -1L;
    }

    /**
     * Return stream column number of this message.
     *
     * @return  Stream column number of this message.
     *          -1 if not associated with a particular line number.
     */
    public long getColumnNumber()
    {
      return location_ != null ? location_.getColumnNumber() : -1L;
    }

    /**
     * Return stream offset of this message.
     *
     * @return  Stream offset of this message.
     *          -1 if not associated with a particular position.
     */
    public long getStreamOffset()
    {
      return location_ != null ? location_.getStreamOffset() : -1L;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
      return level_ + ": " + message_ + " " + location_;
    }
  }

  /** File listing the valid Energistics units. */
  private final static String UNITS_FILE = "units.txt";

  /** The sole instance of this class. */
  private final static JsonValidator instance_ = new JsonValidator();

  /** List of valid Energistics units, mapped on quantity. */
  private final Map<String,List<String>> validUnits_ = new HashMap<>();

  /**
   * Return the sole instance of this class.
   *
   * @return  The sole instance of this class. Never null.
   */
  public static JsonValidator getInstance()
  {
    return instance_;
  }

  /**
   * Create a JSON Well Log Format validator.
   */
  private JsonValidator()
  {
    loadValidUnits();
  }

  /**
   * Load valid quantity/unit combinations from file.
   * Populate the validUnits_ member.
   */
  private void loadValidUnits()
  {
    InputStream stream = null;

    try {
      stream = JsonValidator.class.getResourceAsStream(UNITS_FILE);
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

      String line;

      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#"))
          continue;

        int commaPos = line.indexOf(',');
        assert commaPos != -1 : "Invalid unit entry: " + line;

        String quantity = line.substring(0, commaPos);
        String unit = line.substring(commaPos + 1);

        List<String> units = validUnits_.get(quantity);
        if (units == null) {
          units = new ArrayList<String>();
          validUnits_.put(quantity, units);
        }

        units.add(unit);
      }
    }
    catch (IOException exception) {
      assert false : "Programming error. Quantities file missing.";
    }
    finally {
      if (stream != null) {
        try {
          stream.close();
        }
        catch (IOException exception) {
          // Ignore.
        }
      }
    }
  }

  /**
   * Check if the curves in the specified JSON file is aligned,
   * i.e. that all curves and dimensions have the same number
   * of values.
   *
   * @param jsonFile  JSON file to check. Non-null.
   * @return          True if the JSON file is aligned, false otherwise.
   */
  private static boolean isAligned(JsonFile jsonFile)
  {
    assert jsonFile != null : "jsonFile cannot be null";

    int nValues = jsonFile.getNValues();
    for (JsonCurve curve : jsonFile.getCurves()) {
      for (int dimension = 0; dimension < curve.getNDimensions(); dimension++)
        if (curve.getNValues(dimension) != nValues)
          return false;
    }

    return true;
  }

  /**
   * Validate the index range given in the metadata of the specified JSON file
   * against the actual ones captured from the data section.
   *
   * @param jsonFile          JSON file holding metadata. Non-null.
   * @param actualStartIndex  Actual start index from file. Null if no data.
   * @param actualEndIndex    Actual end index from file. Null if no data.
   * @param minStep           Minimum step encountered in the actual data. Null if no data.
   * @param maxStep           Maximum step encountered in the actual data. Null if no data.
   * @param messages          Validation messages to append to. Non-null.
   */
  private static void validateIndex(JsonFile jsonFile,
                                    Double actualStartIndex, Double actualEndIndex, Double minStep, Double maxStep,
                                    List<Message> messages)
  {
    assert jsonFile != null : "jsonFile cannot be null";
    assert messages != null : "messages cannot be null";

    Class<?> valueType = jsonFile.getIndexValueType();

    Double actualStep = minStep != null ? (minStep + maxStep) / 2.0 : null;
    if (actualStep != null && minStep != null && Math.abs(actualStep - minStep) > 0.001)
      actualStep = null;

    // We will also validate the start/end/step metadata compared
    // to the actual such in the data section
    double startIndex = Util.getAsDouble(jsonFile.getStartIndex());
    double endIndex = Util.getAsDouble(jsonFile.getEndIndex());
    double step = Util.getAsDouble(jsonFile.getStep());

    //
    // Start index
    //
    if (actualStartIndex == null && !Double.isNaN(startIndex))
      messages.add(new Message(Message.Level.WARNING, null,
                               "startIndex " + Util.getAsType(startIndex, valueType) +
                               " doesn't match actual: null."));

    if (actualStartIndex != null && Double.isNaN(startIndex))
      messages.add(new Message(Message.Level.WARNING, null,
                               "startIndex is missing: " + Util.getAsType(actualStartIndex, valueType) + "."));

    if (actualStartIndex != null && actualStartIndex != startIndex)
      messages.add(new Message(Message.Level.WARNING, null,
                               "startIndex " + Util.getAsType(startIndex, valueType) +
                               " doesn't match actual: " + Util.getAsType(actualStartIndex, valueType) + "."));

    //
    // End index
    //
    if (actualEndIndex == null && !Double.isNaN(startIndex))
      messages.add(new Message(Message.Level.WARNING, null,
                               "endIndex " + Util.getAsType(startIndex, valueType) +
                               " doesn't match actual: null."));

    if (actualEndIndex != null && Double.isNaN(endIndex))
      messages.add(new Message(Message.Level.WARNING, null,
                               "endIndex is missing: " + Util.getAsType(actualEndIndex, valueType) + "."));

    if (actualEndIndex != null && actualEndIndex != endIndex)
      messages.add(new Message(Message.Level.WARNING, null,
                               "endIndex " + Util.getAsType(endIndex, valueType) +
                               " doesn't match actual: " + Util.getAsType(actualEndIndex, valueType) + "."));

    //
    // Step
    //
    if (minStep != null && maxStep != null && minStep * maxStep <= 0.0)
      messages.add(new Message(Message.Level.WARNING, null,
                               "Index is not continous increasing or decreasing. " +
                               "min/max step = " + minStep + "/" + maxStep + "."));

    if (actualStep != null && Math.abs(actualStep - step) > 0.001)
      messages.add(new Message(Message.Level.WARNING, null,
                               "step " + step +
                               " doesn't match actual: " + actualStep + "."));

    if (actualStep != null && Double.isNaN(step))
      messages.add(new Message(Message.Level.WARNING, null,
                               "step is missing: " + actualStep + "."));
  }

  /**
   * Validate the data object at the current parser position against
   * the specified JSON file instance.
   *
   * @param jsonParser  JSON parser. Non-null.
   * @param jsonFile    JSON file with curve definitions.
   * @param messages    Validation messages to append to. Non-null.
   * @throws JsonParsingException  If the data object is invalid for some reason.
   */
  private static void validateData(JsonParser jsonParser, JsonFile jsonFile, List<Message> messages)
    throws JsonParsingException
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert jsonFile != null : "jasonFile cannot be null";

    int curveNo = 0;
    int dimension = 0;
    int nDimensions = 1;

    int nCurves = jsonFile.getNCurves();
    int level = 0;

    Double startIndex = null;
    Double endIndex = null;
    Double minStep = null;
    Double maxStep = null;
    Double previousIndex = null;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.START_ARRAY) {
        dimension = 0;
        level++;
      }

      else if (parseEvent == JsonParser.Event.END_ARRAY) {

        level--;

        //
        // If we get to level 0 we are all done. Validate the metadata index
        //
        if (level == 0) {
          validateIndex(jsonFile, startIndex, endIndex, minStep, maxStep, messages);
          return;
        }

        //
        // If at level 1 we have reached end of one row
        //
        if (level == 1) {
          boolean isAligned = isAligned(jsonFile);
          if (!isAligned)
            throw new JsonParsingException("Invalid number of data", jsonParser.getLocation());
          curveNo = 0;
          dimension = 0;

          jsonFile.clearCurves(); // We don't need the data
        }

        //
        // Otherwise we have reached the end of a n-dim curve
        //
        else {
          curveNo++;
          dimension = 0;
        }
      }

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        throw new JsonParsingException("Unrecognized event in curve data", jsonParser.getLocation());
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        throw new JsonParsingException("Unrecognized event in curve data", jsonParser.getLocation());
      }

      else if (parseEvent == JsonParser.Event.KEY_NAME) {
        throw new JsonParsingException("Unrecognized event in curve data", jsonParser.getLocation());
      }

      else {
        Object value = null;
        if (parseEvent == JsonParser.Event.VALUE_NUMBER)
          value = jsonParser.getBigDecimal().doubleValue();
        else if (parseEvent == JsonParser.Event.VALUE_STRING)
          value = jsonParser.getString();
        else if (parseEvent == JsonParser.Event.VALUE_TRUE)
          value = Boolean.TRUE;
        else if (parseEvent == JsonParser.Event.VALUE_FALSE)
          value = Boolean.FALSE;

        //
        // At this point we are about to set the value into the JSON file,
        // so here we check the validity of curveNo and dimension
        //

        boolean isIndexCurve = curveNo == 0;

        if (curveNo >= nCurves) {
          throw new JsonParsingException("Invalid number of data.",
                                         jsonParser.getLocation());
        }

        JsonCurve curve = jsonFile.getCurves().get(curveNo);
        Class<?> valueType = curve.getValueType();
        nDimensions = curve.getNDimensions();

        if (dimension >= nDimensions) {
          throw new JsonParsingException("Invalid number of dimensions.",
                                         jsonParser.getLocation());
        }

        if (isIndexCurve && value == null)
          throw new JsonParsingException("Index values cannot be null",
                                         jsonParser.getLocation());

        if (isIndexCurve) {
          previousIndex = startIndex;

          if (startIndex == null)
            startIndex = Util.getAsDouble(value);

          Double step = previousIndex != null ? startIndex - previousIndex : null;
          if (step != null && (minStep == null || step < minStep))
            minStep = step;
          if (step != null && (maxStep == null || step > maxStep))
            maxStep = step;

          endIndex = startIndex;
        }

        curve.addValue(dimension, Util.getAsType(value, valueType));

        // Move on to the next curve or dimension
        if (nDimensions == 1) {
          curveNo++;
          dimension = 0;
        }
        else {
          dimension++;
        }
      }
    }

    assert false : "Invalid state";
  }

  /**
   * Validate the data object at the current parser position against
   * the specified JSON file instance.
   *
   * @param jsonParser  JSON parser. Non-null.
   * @param jsonFile    JSON file to append curve definitions to. Non-null.
   * @param messages    Validation messages to append to. Non-null.
   * @throws JsonParsingException  If the data object is invalid for some reason.
   */
  private void validateCurveDefinition(JsonParser jsonParser, JsonFile jsonFile, List<Message> messages)
    throws JsonParsingException
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert jsonFile != null : "jsonFile cannot be null";
    assert messages != null : "messages cannot be null";

    String curveName = null;
    String description = null;
    String quantity = null;
    String unit = null;
    Class<?> valueType = null;
    int nDimensions = 1;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      //
      // We are done.
      // Wrap up the curve definition and add to the file instance
      //
      if (parseEvent == JsonParser.Event.END_OBJECT) {
        boolean isIndexCurve = jsonFile.getNCurves() == 0;

        // Check that curve name is present
        if (curveName == null) {
          messages.add(new Message(Message.Level.SEVERE, jsonParser.getLocation(),
                                   "Curve name is missing for curve " + jsonFile.getNCurves() + "."));
          curveName = "curve"; // Use this to we can get on with the validation
        }

        // Check that value type is present
        if (valueType == null) {
          messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                   "Curve valueType is missing. Float assumed."));
          valueType = Double.class;
        }

        // Check that index curve has dimension 1
        if (isIndexCurve && nDimensions > 1) {
          throw new JsonParsingException("Invalid dimensions for index curve: " + nDimensions + ".",
                                         jsonParser.getLocation());
        }

        // Check that index curve is numeric
        if (isIndexCurve && valueType == Boolean.class || valueType == String.class) {
          throw new JsonParsingException("Invalid valueType for index curve: " + valueType + ".",
                                         jsonParser.getLocation());
        }

        // Check that quantity is absent or legal
        if (quantity != null && !quantity.isEmpty() && !validUnits_.containsKey(quantity)) {
          messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                   "Unrecognized quantity: " + quantity + "."));
        }

        // Check that unit is absent or legal according to quantity
        if (unit != null && !unit.isEmpty()) {
          boolean isLegal = false;
          for (Map.Entry<String,List<String>> entry : validUnits_.entrySet()) {
            String knownQuantity = entry.getKey();
            List<String> validUnits = entry.getValue();

            if (quantity != null && quantity.equals(knownQuantity) && validUnits.contains(unit) ||
                quantity == null && validUnits.contains(unit))
              isLegal = true;
          }

          if (!isLegal) {
            if (quantity != null)
              messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                       "Unrecognized unit for: \"" + quantity + "\": " + unit + "."));
            else
              messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                       "Unrecognized unit: \"" + unit + "."));
          }
        }

        JsonCurve curve = new JsonCurve(curveName, description,
                                        quantity, unit, valueType,
                                        nDimensions);
        jsonFile.addCurve(curve);
        return;
      }

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        String key = jsonParser.getString();

        //
        // "name"
        //
        if (key.equals("name")) {
          parseEvent = jsonParser.next();
          curveName = jsonParser.getString();
        }

        //
        // "description"
        //
        else if (key.equals("description")) {
          parseEvent = jsonParser.next();
          description = parseEvent == JsonParser.Event.VALUE_STRING ? jsonParser.getString() : null;
        }

        //
        // "quantity"
        //
        else if (key.equals("quantity")) {
          parseEvent = jsonParser.next();
          quantity = parseEvent == JsonParser.Event.VALUE_STRING ? jsonParser.getString() : null;
        }

        //
        // "unit"
        //
        else if (key.equals("unit")) {
          parseEvent = jsonParser.next();
          unit = parseEvent == JsonParser.Event.VALUE_STRING ? jsonParser.getString() : null;
        }

        //
        // "valueType"
        //
        else if (key.equals("valueType")) {
          parseEvent = jsonParser.next();
          String valueTypeString = jsonParser.getString();
          JsonValueType jsonValueType = JsonValueType.get(valueTypeString);
          if (jsonValueType == null)
            messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                     "Unrecognized value type:  + \"" + valueTypeString + "\". Assume float."));
          valueType = jsonValueType != null ? jsonValueType.getValueType() : Double.class;
        }

        //
        // "dimensions"
        //
        else if (key.equals("dimensions")) {
          parseEvent = jsonParser.next();
          nDimensions = jsonParser.getInt();
          if (nDimensions < 1) {
            throw new JsonParsingException("Invalid number of dimensions: " + nDimensions + ".",
                                           jsonParser.getLocation());
          }
        }

        //
        // Others
        //
        else {
          messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                   "Unrecognized curves element: \"" + key + "\". Ignored."));
        }
      }
    }
  }

  /**
   * Validate the curve definitions at the current location
   * in the parsing process.
   *
   * @param jsonParser  JSON parser. Non-null.
   * @param jsonFile    JSON file we are populating. Non-null.
   * @param messages      Validation messages to append to. Non-null.
   * @throws JsonParsingException  If the parsing fails for some reason.
   */
  private void validateCurveDefinitions(JsonParser jsonParser, JsonFile jsonFile, List<Message> messages)
    throws JsonParsingException
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert jsonFile != null : "jsonFile cannot be null";
    assert messages != null : "message cannot be null";

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.END_ARRAY)
        return;

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        validateCurveDefinition(jsonParser, jsonFile, messages);
      }

      else if (parseEvent != JsonParser.Event.START_ARRAY) {
        messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                 "Unrecognized event in curve definitions: " + parseEvent + "."));
      }
    }
  }

  /**
   * Validate the metadata of the specified JSON file.
   *
   * @param jsonFile  JSON file to validate. Non-null.
   * @param messages  Validation messages to append to. Non-null.
   */
  private static void validateMetadata(JsonFile jsonFile, List<Message> messages)
  {
    assert jsonFile != null : "jsonFile cannot be null";
    assert messages != null : "messages cannot be null";

    // Get the properties that are there
    List<String> actualProperties = jsonFile.getProperties();

    List<String> wellKnownProperties = new ArrayList<>();
    for (JsonWellLogProperty wellKnownProperty : JsonWellLogProperty.values())
      wellKnownProperties.add(wellKnownProperty.getKey());

    // Check what well know property we don't define
    for (String wellKnownProperty : wellKnownProperties) {
      if (!actualProperties.contains(wellKnownProperty))
        messages.add(new Message(Message.Level.WARNING, null, "Property \"" + wellKnownProperty + "\" is undefined."));
    }

    // Check which property we include that is not well known
    for (String actualProperty : actualProperties) {
      if (!wellKnownProperties.contains(actualProperty))
        messages.add(new Message(Message.Level.WARNING, null, "Unrewcognized property \"" + actualProperty + "\"."));
    }
  }

  /**
   * Read "log" object from the current position in the file
   * and return as a JsonFile instance.
   *
   * @param jsonParser    The parser. Non-null.
   * @param messages      Validation messages to append to. Non-null.
   * @throws IOException  If the read operation fails for some reason.
   * @throws JsonParsingException  If the content is not valid JSON.
   */
  private void validateLog(JsonParser jsonParser, List<Message> messages)
    throws IOException, JsonParsingException
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert messages != null : "messages cannot be null";

    JsonFile jsonFile = new JsonFile();

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.END_OBJECT) {
        return;
      }

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        String key = jsonParser.getString();

        //
        // "metadata"
        //
        if (key.equals("metadata")) {
          JsonObjectBuilder objectBuilder = JsonUtil.readJsonObject(jsonParser);
          JsonObject metadata = objectBuilder.build();
          jsonFile.setMetadata(metadata);

          validateMetadata(jsonFile, messages);
        }

        //
        // "curves"
        //
        else if (key.equals("curves")) {
          validateCurveDefinitions(jsonParser, jsonFile, messages);
        }

        //
        // "data"
        //
        else if (key.equals("data")) {
          validateData(jsonParser, jsonFile, messages);
        }

        //
        // Others
        //
        else {
          messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                   "Unrecognized log element: \"" + key + "\". Ignored."));
        }
      }
    }
  }

  /**
   * Validate the specified stream according to the JSON Well Log Format
   * specification.
   * <p>
   * <b>NOTE:</b> The stream is owned by the client and should be closed
   * by the client.
   *
   * @param stream  Stream to validate. Non-null.
   * @return        List of validation messages. Never null.
   * @throws IOException  If the stream access fails for some reason.
   * @throws IllegalArgumentException  If stream is null.
   */
  public List<Message> validate(InputStream stream)
    throws IOException
  {
    if (stream == null)
      throw new IllegalArgumentException("stream cannot be null");

    List<Message> messages = new ArrayList<>();

    JsonParser jsonParser = null;

    try {
      jsonParser = Json.createParser(stream);
    }
    catch (JsonException exception) {
      if (jsonParser != null)
        jsonParser.close();

      if (exception.getCause() instanceof IOException)
        throw (IOException) exception.getCause();

      messages.add(new Message(Message.Level.SEVERE, null, "Unable to parse JSON."));
      return messages;
    }

    assert jsonParser != null : "jsonParser cannot be null here";

    boolean hasTopLevelTag = false;

    try {
      while (jsonParser.hasNext()) {
        JsonParser.Event parseEvent = jsonParser.next();

        if (parseEvent == JsonParser.Event.KEY_NAME) {
          String key = jsonParser.getString();

          //
          // Log
          //
          if (key.equals("log")) {
            hasTopLevelTag = true;
            validateLog(jsonParser, messages);
          }

          //
          // Others
          //
          else {
            messages.add(new Message(Message.Level.WARNING, jsonParser.getLocation(),
                                     "Unrecognized top level element \"" + key + "\". Ignored."));
          }
        }
      }
    }
    catch (JsonParsingException exception) {
      messages.add(new Message(Message.Level.SEVERE, jsonParser.getLocation(), exception.getMessage()));
      return messages;
    }
    finally {
      jsonParser.close();
    }

    if (!hasTopLevelTag)
      messages.add(new Message(Message.Level.WARNING, null,
                               "Top level log tag is missing."));

    return messages;
  }

  /**
   * Validate the content of the specified disk file against the
   * JSON Well Log Format specification.
   *
   * @param file  File to validate. Non-null.
   * @return      List of validation messages. Never null.
   * @throws IllegalArgumentException  If file is null.
   * @throws IOException  If the file access fails for some reason.
   *
   */
  public List<Message> validate(File file)
    throws IOException
  {
    if (file == null)
      throw new IllegalArgumentException("file cannot be null");

    FileInputStream stream = new FileInputStream(file);
    List<Message> messages = validate(stream);
    stream.close();

    return messages;
  }
}
