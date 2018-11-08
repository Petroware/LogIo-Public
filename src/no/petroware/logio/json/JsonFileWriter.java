package no.petroware.logio.json;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonFileWriter
{
  /** The physical disk file to write. */
  private final File file_;

  /** True to write in human readable pretty format, false to write dense. */
  private final boolean isPretty_;

  /** The new line token according to pretty print mode. Cached for efficiency. */
  private final String newline_;

  /** Spacing between tokens according to pretty print mode. Cached for efficiency. */
  private final String spacing_;

  /** Current indentation according to pretty print mode. */
  private final Indentation indentation_;

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
   * Create a JSON file writer instance.
   *
   * @param file         Disk file to write to. Non-null.
   * @param isPretty     True to write in human readable pretty format, false
   *                     to write as dense as possible.
   * @param indentation  The indentation used in pretty print mode. [0,&gt;.
   *                     If isPretty is false, this setting has no effect.
   * @throws IllegalArgumentException  If file is null or indentation is out of bounds.
   */
  public JsonFileWriter(File file, boolean isPretty, int indentation)
  {
    if (file == null)
      throw new IllegalArgumentException("file cannot be null");

    if (isPretty && indentation < 0)
      throw new IllegalArgumentException("Invalid indentation: " + indentation);

    file_ = file;
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
   * @param formatter  Curve data formatter. Non-null.
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
   * @param formatter  Curve formatter. Specified for floating point values only.
   * @param width      Total with set aside for the values of this column.
   * @return           The JSON token to be written to file. Never null.
   */
  private static String getText(Object value, Class<?> valueType, Formatter formatter, int width)
  {
    String text;

    if (value == null)
      text = "null";
    else if (valueType == Date.class)
      text = ISO8601DateParser.toString((Date) value);
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
   * to the given writer.
   *
   * @param writer       Writer to write to. Non-null.
   * @param jsonParser   The JSON parser holding the object to write. Non-null.
   * @param indentation  Current file indentation. Non-null.
   * @throws IOException  If the write operation fails for some reason.
   */
  private void writeArray(Writer writer, JsonParser jsonParser, Indentation indentation)
    throws IOException
  {
    writer.write('[');

    boolean isFirst = true;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        writeObject(writer, jsonParser, indentation.push());
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.START_ARRAY) {
        writeArray(writer, jsonParser, indentation.push());
      }

      else if (parseEvent == JsonParser.Event.END_ARRAY) {
        writer.write("]");
        writer.write(newline_);
        return;
      }

      else if (parseEvent == JsonParser.Event.VALUE_FALSE) {
        if (!isFirst)
          writer.write(",\n");
        writer.write("false");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_TRUE) {
        if (!isFirst)
          writer.write(",\n");
        writer.write("true");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_NULL) {
        if (!isFirst)
          writer.write(",\n");
        writer.write("null");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_NUMBER) {
        if (!isFirst)
          writer.write(",\n");
        BigDecimal value = jsonParser.getBigDecimal();
        writer.write(value.toString());
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.VALUE_STRING) {
        if (!isFirst)
          writer.write(",\n");
        String value = jsonParser.getString();
        writer.write(value);
        isFirst = false;
      }
    }
  }

  /**
   * Write the object at the current position of the specified JSON parser
   * to the given writer.
   *
   * @param writer       Writer to write to. Non-null.
   * @param jsonParser   The JSON parser holding the object to write. Non-null.
   * @param indentation  Current file indentation. Non-null.
   * @throws IOException  If the write operation fails for some reason.
   */
  private void writeObject(Writer writer, JsonParser jsonParser, Indentation indentation)
    throws IOException
  {
    assert writer != null : "writer cannot be null";
    assert jsonParser != null : "jsonParser cannot be null";
    assert indentation != null : "indentation cannot be null";

    writer.write(spacing_);
    writer.write("{");
    writer.write(newline_);

    boolean isFirst = true;

    while (jsonParser.hasNext()) {
      JsonParser.Event parseEvent = jsonParser.next();

      if (parseEvent == JsonParser.Event.KEY_NAME) {
        String key = jsonParser.getString();

        if (!isFirst) {
          writer.write(",");
          writer.write(newline_);
        }

        writer.write(indentation.toString());
        writer.write('\"');
        writer.write(key);
        writer.write('\"');
        writer.write(":");
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.START_OBJECT) {
        writeObject(writer, jsonParser, indentation.push());
        isFirst = false;
      }

      else if (parseEvent == JsonParser.Event.END_OBJECT) {
        writer.write(newline_);
        writer.write(indentation.pop().toString());
        writer.write('}');
        return;
      }

      else if (parseEvent == JsonParser.Event.START_ARRAY) {
        writeArray(writer, jsonParser, indentation.push());
      }

      else if (parseEvent == JsonParser.Event.END_ARRAY) {
        assert false : "Invalid state";
      }

      else if (parseEvent == JsonParser.Event.VALUE_FALSE) {
        writer.write(spacing_);
        writer.write("false");
      }

      else if (parseEvent == JsonParser.Event.VALUE_TRUE) {
        writer.write(spacing_);
        writer.write("true");
      }

      else if (parseEvent == JsonParser.Event.VALUE_NULL) {
        writer.write(spacing_);
        writer.write("null");
      }

      else if (parseEvent == JsonParser.Event.VALUE_NUMBER) {
        BigDecimal value = jsonParser.getBigDecimal();
        writer.write(spacing_);
        writer.write(value.toString());
      }

      else if (parseEvent == JsonParser.Event.VALUE_STRING) {
        String value = jsonParser.getString();
        writer.write(spacing_);
        writer.write('\"');
        writer.write(value);
        writer.write('\"');
      }
    }
  }

  /**
   * Write the specified list of JSON file instances to the disk
   * file of this writer.
   *
   * @param jsonFiles  JSON files to write. Non-null.
   * @throws IllegalArgumentException  If jsonFiles is null.
   * @throws IOException  If the write operation fails for some reason.
   */
  public void write(List<JsonFile> jsonFiles)
    throws IOException
  {
    FileOutputStream outputStream = new FileOutputStream(file_);
    Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));

    writer.write("{");
    writer.write(newline_);

    Indentation indentation = indentation_.push();

    writer.write(indentation.toString());
    writer.write("\"log\":");
    writer.write(spacing_);
    writer.write("{");
    writer.write(newline_);

    indentation = indentation.push();

    for (JsonFile jsonFile : jsonFiles) {
      //
      // "metadata"
      //
      writer.write(indentation.toString());
      writer.write("\"metadata\":");

      JsonParser jsonParser = Json.createParserFactory(null).createParser(jsonFile.getMetadata());
      jsonParser.next();
      writeObject(writer, jsonParser, indentation.push());
      jsonParser.close();

      //
      // "curves"
      //
      writer.write(newline_);
      writer.write(indentation.toString());
      writer.write("\"curves\": [");

      boolean isFirst = true;

      List<JsonCurve> curves = jsonFile.getCurves();

      for (JsonCurve curve : curves) {

        if (!isFirst)
          writer.write(",");

        writer.write(newline_);
        indentation = indentation.push();
        writer.write(indentation.toString());
        writer.write("{");
        writer.write(newline_);
        indentation = indentation.push();

        // Name
        writer.write(indentation.toString());
        writer.write("\"name\":");
        writer.write(spacing_);
        writer.write(getText(curve.getName()));
        writer.write(",");
        writer.write(newline_);

        // Description
        writer.write(indentation.toString());
        writer.write("\"description\":");
        writer.write(spacing_);
        writer.write(getText(curve.getDescription()));
        writer.write(",");
        writer.write(newline_);

        // Quantity
        writer.write(indentation.toString());
        writer.write("\"quantity\":");
        writer.write(spacing_);
        writer.write(getText(curve.getQuantity()));
        writer.write(",");
        writer.write(newline_);

        // Unit
        writer.write(indentation.toString());
        writer.write("\"unit\":");
        writer.write(spacing_);
        writer.write(getText(curve.getUnit()));
        writer.write(",");
        writer.write(newline_);

        // Value type
        writer.write(indentation.toString());
        writer.write("\"valueType\":");
        writer.write(spacing_);
        writer.write(getText(JsonValueType.get(curve.getValueType()).toString()));
        writer.write(",");
        writer.write(newline_);

        // Dimension
        writer.write(indentation.toString());
        writer.write("\"dimensions\":");
        writer.write(spacing_);
        writer.write("" + curve.getNDimensions());
        writer.write(newline_);

        indentation = indentation.pop();
        writer.write(indentation.toString());
        writer.write("}");
        indentation = indentation.pop();

        isFirst = false;
      }

      writer.write(newline_);
      writer.write(indentation.toString());
      writer.write("]");

      //
      // "data"
      //
      writer.write(newline_);
      writer.write(indentation.toString());
      writer.write("\"data\": [");
      writer.write(newline_);

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
            writer.write(indentation.push().toString());
            writer.write("[");
          }

          if (nDimensions > 1) {
            if (curveNo > 0)
              writer.write(", ");

            writer.write("[");
            for (int dimension = 0; dimension < nDimensions; dimension ++) {
              Object value = curve.getValue(dimension, index);
              String text = getText(value, valueType, formatter, width);

              if (dimension > 0)
                writer.write(", ");
              writer.write(text);
            }
            writer.write("]");
          }
          else {
            Object value = curve.getValue(0, index);
            String text = getText(value, valueType, formatter, width);

            if (curveNo > 0)
              writer.write(", ");
            writer.write(text);
          }
        }

        writer.write(']');
        if (index < jsonFile.getNValues() - 1)
          writer.write(',');
        writer.write(newline_);
      }

      writer.write(indentation.toString());
      writer.write("]");
      writer.write(newline_);
    }

    indentation = indentation.pop();

    writer.write(indentation.toString());
    writer.write("}");
    writer.write(newline_);
    writer.write("}");
    writer.write(newline_);

    writer.close();
  }

  /**
   * Write the specified JSON file instance to the disk file of
   * this writer.
   *
   * @param jsonFile  JSON file instance to write. Non-null.
   * @throws IllegalArgumentException  If jsonFile is null.
   * @throws IOException  If the write operation fails for some reason.
   */
  public void write(JsonFile jsonFile)
    throws IOException
  {
    if (jsonFile == null)
      throw new IllegalArgumentException("jsonFile cannot be null");

    List<JsonFile> jsonFiles = new ArrayList<>();
    jsonFiles.add(jsonFile);

    write(jsonFiles);
  }
}
