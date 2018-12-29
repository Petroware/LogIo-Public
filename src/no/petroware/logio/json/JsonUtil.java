package no.petroware.logio.json;

import java.math.BigDecimal;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;

import no.petroware.logio.util.Util;

/**
 * A collection of utilities for the Log I/O JSON module.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
final class JsonUtil
{
  /**
   * Read a JSON array from the current location of the JSON parser.
   *
   * @param jsonParser  The JSON parser. Non-null.
   * @return  The JSON array builder. Never null.
   */
  static JsonArrayBuilder readJsonArray(JsonParser jsonParser)
  {
    assert jsonParser != null : "jsonParser cannot be null";

    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.START_OBJECT) {
        JsonObjectBuilder objectBuilder = readJsonObject(jsonParser);
        arrayBuilder.add(objectBuilder);
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.START_ARRAY) {
        JsonArrayBuilder subArrayBuilder = readJsonArray(jsonParser);
        arrayBuilder.add(subArrayBuilder);
      }

      else if (parseEvent == JsonParser.Event.END_ARRAY) {
        return arrayBuilder;
      }

      else if (parseEvent == JsonParser.Event.VALUE_FALSE) {
        arrayBuilder.add(false);
      }

      else if (parseEvent == JsonParser.Event.VALUE_TRUE) {
        arrayBuilder.add(true);
      }

      else if (parseEvent == JsonParser.Event.VALUE_NULL) {
        arrayBuilder.addNull();
      }

      else if (parseEvent == JsonParser.Event.VALUE_NUMBER) {
        BigDecimal value = jsonParser.getBigDecimal();
        arrayBuilder.add(value);
      }

      else if (parseEvent == JsonParser.Event.VALUE_STRING) {
        String value = jsonParser.getString();
        arrayBuilder.add(value);
      }
    }

    assert false : "Invalid state";
    return null;
  }

  /**
   * Read a JSON array from the current location of the JSON parser.
   *
   * @param jsonParser  The JSON parser. Non-null.
   * @return  The JSON object builder. Never null.
   */
  static JsonObjectBuilder readJsonObject(JsonParser jsonParser)
  {
    assert jsonParser != null : "jsonParser cannot be null";

    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

    String key = null;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        key = jsonParser.getString();
      }

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        if (key != null) {
          JsonObjectBuilder subObjectBuilder = readJsonObject(jsonParser);
          objectBuilder.add(key, subObjectBuilder);
        }
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        return objectBuilder;
      }

      else if (parseEvent == JsonParser.Event.START_ARRAY) {
        JsonArrayBuilder arrayBuilder = readJsonArray(jsonParser);
        objectBuilder.add(key, arrayBuilder);
      }

      else if (parseEvent == JsonParser.Event.END_ARRAY) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.VALUE_FALSE) {
        objectBuilder.add(key, false);
      }

      else if (parseEvent == JsonParser.Event.VALUE_TRUE) {
        objectBuilder.add(key, true);
      }

      else if (parseEvent == JsonParser.Event.VALUE_NULL) {
        objectBuilder.addNull(key);
      }

      else if (parseEvent == JsonParser.Event.VALUE_NUMBER) {
        BigDecimal value = jsonParser.getBigDecimal();
        objectBuilder.add(key, value);
      }

      else if (parseEvent == JsonParser.Event.VALUE_STRING) {
        String value = jsonParser.getString();
        objectBuilder.add(key, value);
      }
    }

    assert false : "Invalid state";
    return null;
  }

  /**
   * Find object with the specified key from the current position in
   * the JSON object of the given parser.
   *
   * @param jsonParser  JSON parser to consider. Non-null.
   * @param key         Key to search. Non-null.
   * @return            The requested object, or null if not found.
   */
  static Object findObject(JsonParser jsonParser, String key)
  {
    assert jsonParser != null : "jsonPArser cannot be null";
    assert key != null : "key cannot be null";

    boolean foundKey = false;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        String tag = jsonParser.getString();

        if (tag.equals(key))
          foundKey = true;
      }

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        JsonObjectBuilder objectBuilder = readJsonObject(jsonParser);
        if (foundKey)
          return objectBuilder.build();
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        return null;
      }

      else if (parseEvent == JsonParser.Event.START_ARRAY) {
        JsonArrayBuilder arrayBuilder = readJsonArray(jsonParser);
        if (foundKey)
          return arrayBuilder.build();
      }

      else if (parseEvent == JsonParser.Event.VALUE_FALSE) {
        if (foundKey)
          return Boolean.FALSE;
      }

      else if (parseEvent == JsonParser.Event.VALUE_TRUE) {
        if (foundKey)
          return Boolean.TRUE;
      }

      else if (parseEvent == JsonParser.Event.VALUE_NULL) {
        if (foundKey)
          return null;
      }

      else if (parseEvent == JsonParser.Event.VALUE_NUMBER) {
        if (foundKey)
          return jsonParser.getBigDecimal();
      }

      else if (parseEvent == JsonParser.Event.VALUE_STRING) {
        if (foundKey)
          return jsonParser.getString();
      }
    }

    // Key not found
    return null;
  }

  /**
   * Find actual step value of the specified JSON log, being the distance between
   * values in the index curve. Three values are returned: the <em>minimum step</em>,
   * the <em>maximum step</em> and the <em>average step</em>. It is left to the client
   * to decide if these numbers represents a <em>regular</em> or an <em>irregular</em>
   * log set.
   *
   * @param log  Log to get step from. Non-null.
   * @return     The (minimum, maximum and average) step value of the log.
   * @throws IllegalArgumentException  If log is null.
   */
  static double[] findStep(JsonLog log)
  {
    if (log == null)
      throw new IllegalArgumentException("log");

    List<JsonCurve> curves = log.getCurves();

    JsonCurve indexCurve = !curves.isEmpty() ? curves.get(0) : null;
    int nValues = indexCurve != null ? indexCurve.getNValues() : 0;

    if (nValues < 2)
      return new double[] {0.0, 0.0, 0.0};

    double minStep = +Double.MAX_VALUE;
    double maxStep = -Double.MAX_VALUE;
    double averageStep = 0.0;

    int nSteps = 0;
    double indexValue0 = Util.getAsDouble(indexCurve.getValue(0));
    for (int index = 1; index < nValues; index++) {
      double indexValue1 = Util.getAsDouble(indexCurve.getValue(index));
      double step = indexValue1 - indexValue0;

      nSteps++;

      if (step < minStep)
        minStep = step;

      if (step > maxStep)
        maxStep = step;

      averageStep += (step - averageStep) / nSteps;

      indexValue0 = indexValue1;
    }

    return new double[] {minStep, maxStep, averageStep};
  }

  /**
   * Based on the index curve, compute the step value of the specified log
   * as it will be reported in the <em>step</em> metadata.
   * <p>
   * The method uses the {@link JsonUtil#findStep} method to compute min, max and
   * average step, and then compare the largest deviation from the average
   * (min or max) to the average itself.
   * If this is within some limit (0.5% currently) the step is considered
   * regular.
   *
   * @param jsonLog  Log to compute step of. Non-null.
   * @return         The log step value. null if irregular.
   * @throws IllegalArgumentException  If log is null.
   */
  public static Double computeStep(JsonLog log)
  {
    if (log == null)
      throw new IllegalArgumentException("log cannot be null");

    double[] step = findStep(log);

    double minStep = step[0];
    double maxStep = step[1];
    double averageStep = step[2];

    // Find largest deviation from average of the two
    double d = Math.max(Math.abs(minStep - averageStep), Math.abs(maxStep - averageStep));

    // Figure out if this is close enough to regard as equal
    // NOTE: If this number causes apparently regular log sets to appear irregular
    // we might consider adjusting it further, probably as high as 0.01 would be OK.
    boolean isEqual = d <= Math.abs(averageStep) * 0.005;

    return isEqual ? averageStep : null;
  }
}
