package no.petroware.logio.json;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonParser;

import no.petroware.logio.util.Formatter;
import no.petroware.logio.util.ISO8601DateParser;
import no.petroware.logio.util.Util;

/**
 * Class for writing JSON files to disk.
 * <p>
 * Typical usage:
 *
 * <pre>
 *   JsonFile jsonFile = new JsonFile();
 *   :
 *
 *   // Writ as human readable with indentation = 2
 *   JsonWriter writer = new JsonWriter(new File("path/to/file"), true, 2);
 *   writer.write(jsonFile);
 *   writer.close();
 * </pre>
 *
 * If there is to much data to keep in memory, it is possible to write in a
 * <em>streaming</em> manner by adding curve data and appending the output
 * file alternately, see {@link #append}.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonWriter
  implements Closeable
{
  /** The physical disk file to write. */
  private final File file_;

  /** The output stream to write. */
  private final OutputStream outputStream_;

  /** True to write in human readable pretty format, false to write dense. */
  private final boolean isPretty_;

  /** The new line token according to pretty print mode. Cached for efficiency. */
  private final String newline_;

  /** Spacing between tokens according to pretty print mode. Cached for efficiency. */
  private final String spacing_;

  /** Current indentation according to pretty print mode. */
  private final Indentation indentation_;

  /** The writer instance. */
  private Writer writer_;

  /** Indicate if the last written JSON file contains data or not. */
  private boolean hasData_;

  /**
   * Class for holding a space indentation as used at the beginning
   * of the line when writing in pretty print mode to disk file.
   *
   * @author <a href="mailto:info@petroware.no">Petroware AS</a>
   */
  private final static class Indentation
  {
    /** Number of characters for the indentation. [0,&gt;. */
    private final int unit_;

    /** The actual indentation string. */
    private final String indent_;

    /**
     * Create an indentation instance of the specified unit,
     * and an initial indentation.
     *
     * @param unit         Number of characters per indentation. [0,&gt;.
     * @param indentation  Current indentation. Non-null.
     */
    private Indentation(int unit, String indentation)
    {
      assert unit >= 0 : "Invalid unit: " + unit;
      assert indentation != null : "indentation cannot be null";

      unit_ = unit;
      indent_ = indentation;
    }

    /**
     * Create a new indentation instance indented one level to the right.
     *
     * @return  The requested indentation instance. Never null.
     */
    private Indentation push()
    {
      int indentation = indent_.length() + unit_;
      return new Indentation(unit_, Util.getSpaces(indentation));
    }

    /**
     * Create a new indentation instance indented one level to the left.
     *
     * @return  The requested indentation instance. Never null.
     */
    private Indentation pop()
    {
      int indentation = Math.max(0, indent_.length() - unit_);
      return new Indentation(unit_, Util.getSpaces(indentation));
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
      return indent_;
    }
  }

  /**
   * Create a JSON writer instance.
   *
   * @param outputStream  Stream to write. Non-null.
   * @param isPretty      True to write in human readable pretty format, false
   *                      to write as dense as possible.
   * @param indentation   The white space indentation used in pretty print mode. [0,&gt;.
   *                      If isPretty is false, this setting has no effect.
   * @throws IllegalArgumentException  If outputStream is null or indentation is out of bounds.
   */
  public JsonWriter(OutputStream outputStream, boolean isPretty, int indentation)
  {
    if (outputStream == null)
      throw new IllegalArgumentException("outputStream cannot be null");

    if (isPretty && indentation < 0)
      throw new IllegalArgumentException("Invalid indentation: " + indentation);

    file_ = null;
    outputStream_ = outputStream;
    isPretty_ = isPretty;
    newline_ = isPretty_ ? "\n" : "";
    spacing_ = isPretty_ ? " " : "";
    indentation_ = new Indentation(isPretty ? indentation : 0, "");
  }

  /**
   * Create a JSON writer instance.
   *
   * @param file         Disk file to write to. Non-null.
   * @param isPretty     True to write in human readable pretty format, false
   *                     to write as dense as possible.
   * @param indentation  The white space indentation used in pretty print mode. [0,&gt;.
   *                     If isPretty is false, this setting has no effect.
   * @throws IllegalArgumentException  If file is null or indentation is out of bounds.
   */
  public JsonWriter(File file, boolean isPretty, int indentation)
  {
    if (file == null)
      throw new IllegalArgumentException("file cannot be null");

    if (isPretty && indentation < 0)
      throw new IllegalArgumentException("Invalid indentation: " + indentation);

    file_ = file;
    outputStream_ = null;
    isPretty_ = isPretty;
    newline_ = isPretty_ ? "\n" : "";
    spacing_ = isPretty_ ? " " : "";
    indentation_ = new Indentation(isPretty ? indentation : 0, "");
  }

  /**
   * Compute the width of the widest element of the column of the specified
   * curve data.
   *
   * @param curve      Curve to compute column width of. Non-null.
   * @param formatter  Curve data formatter. Null if N/A for the specified curve.
   * @return Width of widest element of the curve. [0,&gt;.
   */
  private static int computeColumnWidth(JsonCurve curve, Formatter formatter)
  {
    assert curve != null :  "curve cannot be null";

    int columnWidth = 0;
    Class<?> valueType = curve.getValueType();

    for (int index = 0; index < curve.getNValues(); index++) {
      for (int dimension = 0; dimension < curve.getNDimensions(); dimension++) {
        Object value = curve.getValue(dimension, index);

        String text = "";

        if (value == null)
          text = "null";

        else if (valueType == Date.class)
          text = "2018-10-10T12:20:00Z"; // Template

        else if (formatter != null)
          text = formatter.format(Util.getAsDouble(value));

        else if (valueType == String.class)
          text = '\"' + value.toString() + '\"';

        else // Boolean and Integers
          text = value.toString();

        if (text.length() > columnWidth)
          columnWidth = text.length();
      }
    }

    return columnWidth;
  }

  /**
   * Get the specified string token as a text suitable for writing to
   * a JSON disk file, i.e. "null" if null, or properly quoted if non-null.
   *
   * @param value  Value to get as text. May be null.
   * @return       The value as a JSON text. Never null.
   */
  private static String getText(String value)
  {
    return value != null ? '\"' + value + '\"' : "null";
  }

  /**
   * Get the specified data value as text, according to the specified value type,
   * the curve formmatter, the curve width and the general rules for the JSON
   * format.
   *
   * @param value      Curve value to get as text. May be null, in case "null" is returned.
   * @param valueType  Java value type of the curve of the value. Non-null.
   * @param formatter  Curve formatter. Specified for floating point values only, null otherwise,
   * @param width      Total with set aside for the values of this column. [0,&gt;.
   * @return           The JSON token to be written to file. Never null.
   */
  private static String getText(Object value, Class<?> valueType, Formatter formatter, int width)
  {
    assert valueType != null : "valueType cannot be null";
    assert width >= 0 : "Invalid width: " + width;

    String text;

    if (value == null)
      text = "null";
    else if (valueType == Date.class)
      text = '\"' + ISO8601DateParser.toString((Date) value) + '\"';
    else if (valueType == Boolean.class)
      text = value.toString();
    else if (formatter != null)
      text = formatter.format(Util.getAsDouble(value));
    else if (valueType == String.class)
      text = '\"' + value.toString() + '\"';
    else
      text = value.toString();

    String padding = Util.getSpaces(width - text.length());
    return padding + text;
  }

  /**
   * Write the array at the current position of the specified JSON parser
   * to the destination.
   *
   * @param jsonParser   The JSON parser holding the object to write. Non-null.
   * @param indentation  Current file indentation. Non-null.
   * @throws IOException  If the write operation fails for some reason.
   */
  private void writeArray(JsonParser jsonParser, Indentation indentation)
    throws IOException
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert indentation != null : "indentation cannot be null";

    writer_.write(" [\n");

    boolean isFirst = true;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        writeObject(jsonParser, indentation.push());
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.START_ARRAY) {
        writeArray(jsonParser, indentation.push());
      }

      else if (parseEvent == JsonParser.Event.END_ARRAY) {
        writer_.write("]");
        return;
      }

      else if (parseEvent == JsonParser.Event.VALUE_FALSE) {
        if (!isFirst)
          writer_.write(",\n");
        writer_.write(indentation.toString());
        writer_.write("false");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_TRUE) {
        if (!isFirst)
          writer_.write(",\n");
        writer_.write(indentation.toString());
        writer_.write("true");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_NULL) {
        if (!isFirst)
          writer_.write(",\n");
        writer_.write(indentation.toString());
        writer_.write("null");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_NUMBER) {
        if (!isFirst)
          writer_.write(",\n");
        writer_.write(indentation.toString());
        BigDecimal value = jsonParser.getBigDecimal();
        writer_.write(value.toString());
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_STRING) {
        if (!isFirst)
          writer_.write(",\n");
        writer_.write(indentation.toString());
        String value = jsonParser.getString();
        writer_.write(value);
        isFirst = false;
      }
    }
  }

  /**
   * Write the object at the current position of the specified JSON parser
   * to the destination.
   *
   * @param jsonParser   The JSON parser holding the object to write. Non-null.
   * @param indentation  Current file indentation. Non-null.
   * @throws IOException  If the write operation fails for some reason.
   */
  private void writeObject(JsonParser jsonParser, Indentation indentation)
    throws IOException
  {
    assert jsonParser != null : "jsonParser cannot be null";
    assert indentation != null : "indentation cannot be null";

    writer_.write(spacing_);
    writer_.write("{");
    writer_.write(newline_);

    boolean isFirst = true;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        String key = jsonParser.getString();

        if (!isFirst) {
          writer_.write(",");
          writer_.write(newline_);
        }

        writer_.write(indentation.toString());
        writer_.write('\"');
        writer_.write(key);
        writer_.write('\"');
        writer_.write(":");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        writeObject(jsonParser, indentation.push());
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        writer_.write(newline_);
        writer_.write(indentation.pop().toString());
        writer_.write('}');
        return;
      }

      else if (parseEvent == JsonParser.Event.START_ARRAY) {
        writeArray(jsonParser, indentation.push());
      }

      else if (parseEvent == JsonParser.Event.END_ARRAY) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.VALUE_FALSE) {
        writer_.write(spacing_);
        writer_.write("false");
      }

      else if (parseEvent == JsonParser.Event.VALUE_TRUE) {
        writer_.write(spacing_);
        writer_.write("true");
      }

      else if (parseEvent == JsonParser.Event.VALUE_NULL) {
        writer_.write(spacing_);
        writer_.write("null");
      }

      else if (parseEvent == JsonParser.Event.VALUE_NUMBER) {
        BigDecimal value = jsonParser.getBigDecimal();
        writer_.write(spacing_);
        writer_.write(value.toString());
      }

      else if (parseEvent == JsonParser.Event.VALUE_STRING) {
        String value = jsonParser.getString();
        writer_.write(spacing_);
        writer_.write('\"');
        writer_.write(value);
        writer_.write('\"');
      }
    }
  }

  /**
   * Write the curve data of the specified JSON file to the stream
   * of this writer.
   *
   * @param jsonFile  JSON file to write curves of. Non-null.
   * @throws IOException  If the write operation fails for some reason.
   */
  private void writeData(JsonFile jsonFile)
    throws IOException
  {
    assert jsonFile != null : "jsonFile cannot be null";

    Indentation indentation = indentation_.push().push().push();

    List<JsonCurve> curves = jsonFile.getCurves();

    // Create formatters for each curve
    Map<JsonCurve,Formatter> formatters = new HashMap<>();
    for (int curveNo = 0; curveNo < jsonFile.getNCurves(); curveNo++) {
      JsonCurve curve = curves.get(curveNo);
      formatters.put(curve, jsonFile.createFormatter(curve, curveNo == 0));
    }

    // Compute column width for each data column
    Map<JsonCurve,Integer> columnWidths = new HashMap<>();
    for (JsonCurve curve : curves)
      columnWidths.put(curve, computeColumnWidth(curve, formatters.get(curve)));

    for (int index = 0; index < jsonFile.getNValues(); index++) {
      for (int curveNo = 0; curveNo < jsonFile.getNCurves(); curveNo++) {
        JsonCurve curve = curves.get(curveNo);
        Class<?> valueType = curve.getValueType();
        int nDimensions = curve.getNDimensions();
        int width = columnWidths.get(curve);
        Formatter formatter = formatters.get(curve);

        if (curveNo == 0) {
          writer_.write(indentation.toString());
          writer_.write("[");
        }

        if (nDimensions > 1) {
          if (curveNo > 0)
            writer_.write(", ");

          writer_.write("[");
          for (int dimension = 0; dimension < nDimensions; dimension ++) {
            Object value = curve.getValue(dimension, index);
            String text = getText(value, valueType, formatter, width);

            if (dimension > 0)
              writer_.write(", ");
            writer_.write(text);
          }
          writer_.write("]");
        }
        else {
          Object value = curve.getValue(0, index);
          String text = getText(value, valueType, formatter, width);

          if (curveNo > 0)
            writer_.write(", ");
          writer_.write(text);
        }
      }

      writer_.write(']');
      if (index < jsonFile.getNValues() - 1) {
        writer_.write(',');
        writer_.write(newline_);
      }
    }
  }

  /**
   * Write the specified JSON file instances to this writer.
   * Multiple files can be written in sequence to the same stream.
   * Additional data can be appended to the last one by {@link #append}.
   * When writing is done, close the writer with {@link #close}.
   *
   * @param jsonFile  JSON file to write. Non-null.
   * @throws IllegalArgumentException  If jsonFile is null.
   * @throws IOException  If the write operation fails for some reason.
   */
  public void write(JsonFile jsonFile)
    throws IOException
  {
    if (jsonFile == null)
      throw new IllegalArgumentException("jsonFile cannot be null");

    boolean isFirstLog = writer_ == null;

    // Create the writer on first write operation
    if (isFirstLog) {
      OutputStream outputStream = file_ != null ? new FileOutputStream(file_) : outputStream_;
      writer_ = new BufferedWriter(new OutputStreamWriter(outputStream));
      writer_.write("{");
      writer_.write(newline_);
    }

    // If this is an additional log, close the previous and make ready for a new
    else {
      writer_.write(newline_);
      writer_.write(indentation_.push().push().toString());
      writer_.write("]");
      writer_.write(newline_);

      writer_.write(indentation_.push().toString());
      writer_.write("},");
      writer_.write(newline_);
    }

    Indentation indentation = indentation_.push();

    writer_.write(indentation.toString());
    writer_.write("\"log\":");
    writer_.write(spacing_);
    writer_.write("{");
    writer_.write(newline_);

    indentation = indentation.push();

    //
    // "metadata"
    //
    writer_.write(indentation.toString());
    writer_.write("\"metadata\":");

    JsonParser jsonParser = Json.createParserFactory(null).createParser(jsonFile.getMetadata());
    jsonParser.next();
    writeObject(jsonParser, indentation.push());
    jsonParser.close();

    writer_.write(',');

    //
    // "curves"
    //
    writer_.write(newline_);
    writer_.write(indentation.toString());
    writer_.write("\"curves\": [");

    boolean isFirstCurve = true;

    List<JsonCurve> curves = jsonFile.getCurves();

    for (JsonCurve curve : curves) {

      if (!isFirstCurve)
        writer_.write(",");

      writer_.write(newline_);
      indentation = indentation.push();
      writer_.write(indentation.toString());
      writer_.write("{");
      writer_.write(newline_);
      indentation = indentation.push();

      // Name
      writer_.write(indentation.toString());
      writer_.write("\"name\":");
      writer_.write(spacing_);
      writer_.write(getText(curve.getName()));
      writer_.write(",");
      writer_.write(newline_);

      // Description
      writer_.write(indentation.toString());
      writer_.write("\"description\":");
      writer_.write(spacing_);
      writer_.write(getText(curve.getDescription()));
      writer_.write(",");
      writer_.write(newline_);

      // Quantity
      writer_.write(indentation.toString());
      writer_.write("\"quantity\":");
      writer_.write(spacing_);
      writer_.write(getText(curve.getQuantity()));
      writer_.write(",");
      writer_.write(newline_);

      // Unit
      writer_.write(indentation.toString());
      writer_.write("\"unit\":");
      writer_.write(spacing_);
      writer_.write(getText(curve.getUnit()));
      writer_.write(",");
      writer_.write(newline_);

      // Value type
      writer_.write(indentation.toString());
      writer_.write("\"valueType\":");
      writer_.write(spacing_);
      writer_.write(getText(JsonValueType.get(curve.getValueType()).toString()));
      writer_.write(",");
      writer_.write(newline_);

      // Dimension
      writer_.write(indentation.toString());
      writer_.write("\"dimensions\":");
      writer_.write(spacing_);
      writer_.write("" + curve.getNDimensions());
      writer_.write(newline_);

      indentation = indentation.pop();
      writer_.write(indentation.toString());
      writer_.write("}");
      indentation = indentation.pop();

      isFirstCurve = false;
    }

    writer_.write(newline_);
    writer_.write(indentation.toString());
    writer_.write("]");

    writer_.write(',');

    //
    // "data"
    //
    writer_.write(newline_);
    writer_.write(indentation.toString());
    writer_.write("\"data\": [");
    writer_.write(newline_);

    writeData(jsonFile);

    hasData_ = jsonFile.getNValues() > 0;
  }

  /**
   * Append the curve data of the specified JSON file to this
   * writer.
   * <p>
   * This feature can be used to <em>stream</em> data to a JSON
   * destination. By repeatedly clearing and populating the JSON
   * file curves with new data there is no need for the client to
   * keep the full volume in memory at any point in time.
   * <p>
   * <b>NOTE:</b> This method should be called after the JSON meta
   * data has been written (see {@link #write}), and the JSON file must be
   * compatible with this.
   * <p>
   * When writing is done, close the stream with {@link #close}.
   *
   * @param jsonFile  JSON file of data append to stream. Non-null.
   * @throws IllegalArgumentException  If jsonFile is null.
   * @throws IllegalStateException     If the writer is not open for writing.
   * @throws IOException  If the write operation fails for some reason.
   */
  public void append(JsonFile jsonFile)
    throws IOException
  {
    if (jsonFile == null)
      throw new IllegalArgumentException("jsonFile cannot be null");

    if (writer_ == null)
      throw new IllegalStateException("Writer is not open");

    if (hasData_) {
      writer_.write(",");
      writer_.write(newline_);
    }

    writer_.write(indentation_.toString());
    writeData(jsonFile);

    if (!hasData_ && jsonFile.getNValues() > 0)
      hasData_ = true;
  }

  /**
   * Append the final brackets to the JSON stream and
   * close the writer.
   */
  @Override
  public void close()
    throws IOException
  {
    // Nothing to do if the writer was never opened
    if (writer_ == null)
      return;

    // Complete the data array
    writer_.write(newline_);
    writer_.write(indentation_.push().push().toString());
    writer_.write("]");
    writer_.write(newline_);

    // Complete the log object
    writer_.write(indentation_.push().toString());
    writer_.write("}");
    writer_.write(newline_);

    // Complete the JSON object
    writer_.write("}");
    writer_.write(newline_);

    writer_.close();
    writer_ = null;
  }

  /**
   * Convenience method for writing the content of the specified JSON
   * files to a string.
   *
   * @param jsonFiles    JSON files to write. Non-null.
   * @param isPretty     True to write in human readable pretty format, false
   *                     to write as dense as possible.
   * @param indentation  The white space indentation used in pretty print mode. [0,&gt;.
   *                     If isPretty is false, this setting has no effect.
   * @return             The requested string. Never null.
   * @throws IllegalArgumentException  If jsonFiles is null or indentation is out of bounds.
   */
  public static String toString(List<JsonFile> jsonFiles, boolean isPretty, int indentation)
  {
    if (jsonFiles == null)
      throw new IllegalArgumentException("jsonFiles cannot be null");

    if (indentation < 0)
      throw new IllegalArgumentException("invalid indentation: " + indentation);

    ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
    JsonWriter writer = new JsonWriter(stringStream, isPretty, indentation);

    String string = "";

    try {
      for (JsonFile jsonFile : jsonFiles)
        writer.write(jsonFile);
    }
    catch (IOException exception) {
      // Since we are writing to memory (ByteArrayOutputStream) we don't really
      // expect an IOException so if we get one anyway, we are in serious trouble
      throw new RuntimeException("Unable to write", exception);
    }
    finally {
      try {
        writer.close();
        string = new String(stringStream.toByteArray(), "UTF-8");
      }
      catch (IOException exception) {
        // Again: This will never happen.
        throw new RuntimeException("Unable to write", exception);
      }
    }

    return string;
  }

  /**
   * Convenience method for writing the content of the specified JSON
   * file to a string.
   *
   * @param jsonFile     JSON files to write. Non-null.
   * @param isPretty     True to write in human readable pretty format, false
   *                     to write as dense as possible.
   * @param indentation  The white space indentation used in pretty print mode. [0,&gt;.
   *                     If isPretty is false, this setting has no effect.
   * @return             The requested string. Never null.
   * @throws IllegalArgumentException  If jsonFile is null or indentation is out of bounds.
   */
  public static String toString(JsonFile jsonFile, boolean isPretty, int indentation)
  {
    if (jsonFile == null)
      throw new IllegalArgumentException("jsonFile cannot be null");

    if (indentation < 0)
      throw new IllegalArgumentException("invalid indentation: " + indentation);

    List<JsonFile> jsonFiles = new ArrayList<>();
    jsonFiles.add(jsonFile);
    return toString(jsonFiles, isPretty, indentation);
  }

  /**
   * Convenience method for writing the content of the specified JSON
   * file to a pretty printed string.
   *
   * @param jsonFile  JSON files to write. Non-null.
   * @return          The requested string. Never null.
   * @throws IllegalArgumentException  If jsonFile is null.
   */
  public static String toString(JsonFile jsonFile)
  {
    if (jsonFile == null)
      throw new IllegalArgumentException("jsonFile cannot be null");

    return toString(jsonFile, true, 2);
  }
}
