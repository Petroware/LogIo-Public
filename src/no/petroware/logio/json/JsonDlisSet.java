package no.petroware.logio.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A convenience class to hold DLIS <em>sets</em> when DLIS files
 * are converted to JSON to avoid that information gets lost.
 * <p>
 * A DLIS set has a name, a set of attributes and a number of
 * objects with one or more values for each of the attributes.
 * It can be viewed as a matrix as follows:
 * <pre>
 *
 *             attr1  attr2  attr3  ... attrn
 *   ----------------------------------------
 *   object1    v11    v12    v13        v1n
 *   object2    v21    v22    v23        v2n
 *   object3    v31    v32    v33        v3n
 *      :
 *   objectm    vm1    vm2    vm3        vmn
 *   ----------------------------------------
 * </pre>
 * <p>
 * Using the {@link JsonDlisSet} class for transition, the information
 * can be represented in JSON as:
 * <pre>
 *   "&lt;name&gt;": {
 *     "attributes": ["attr1", "attr2", "attr3", ..., "attrn"],
 *     "objects" [
 *       "object1": [v11, v12, v13, ..., v1n],
 *       "object2": [v21, v22, v23, ..., v1n],
 *       "object3": [v21, v22, v23, ..., v1n],
 *       :
 *       "objectm": [vm1, vm2, vm3, ..., vmn],
 *     }
 *   }
 * </pre>
 * According to the DLIS specification each value (v11, v12 etc.) are themselves a
 * <em>list</em> of values. In DLIS the attributes also contains an optional
 * <em>unit of measurement</em> entry and a <em>value type</em> entry that
 * applies to all the object values for that attribute. To simplify, both have
 * been omitted in the present class; The latter is unnecessary, and the former
 * is hardly used, and can be added as a separate attribute column if needed.
 * <p>
 * Note that the <em>informational value</em> of such sets are low.
 * Some clients may understand the meaning of the attributes and objects
 * involved, but in general this information is not fit for further processing.
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class JsonDlisSet
{
  /** Set name. Non-null. */
  private final String name_;

  /** Attributes for the objects of this set. */
  private final List<String> attributes_ = new ArrayList<>();

  /** The objects of this set. With values for each attribute. */
  private final Map<String, List<List<Object>>> objects_ = new LinkedHashMap<>();

  /**
   * Create a JSON DLIS set with the given name and set of attributes.
   *
   * @param name        Set name. Non-null.
   * @param attributes  List of attributes. Non-null.
   * @throws IllegalArgumentException  If name or attributes is null.
   */
  public JsonDlisSet(String name, List<String> attributes)
  {
    if (name == null)
      throw new IllegalArgumentException("name cannot be null");

    if (attributes == null)
      throw new IllegalArgumentException("attributes cannot be null");

    name_ = name;
    attributes_.addAll(attributes);
  }

  /**
   * Return name of this set.
   *
   * @return  Name of this set. Never null.
   */
  public String getName()
  {
    return name_;
  }

  /**
   * Return the attributes that exists for the object of this set.
   *
   * @return  The attributes of this set. Never null.
   */
  public List<String> getAttributes()
  {
    return Collections.unmodifiableList(attributes_);
  }

  /**
   * Return the  attribute values for the specified object.
   * Create if they doesn't yet exists.
   *
   * @param objectName  Object name to get or create attribute values for.
   * @return            The requested list of object values. Never null.
   */
  private List<List<Object>> getOrCreateValues(String objectName)
  {
    assert objectName != null : "objectName cannot be null";

    if (!objects_.containsKey(objectName)) {
      List<List<Object>> objectValues = new ArrayList<>();
      for (int i = 0; i < attributes_.size(); i++) {
        List<Object> baseValues = new ArrayList<>();
        objectValues.add(baseValues);
      }
      objects_.put(objectName, objectValues);
    }

    return objects_.get(objectName);
  }

  /**
   * Add a single value for the given attribute of the specified object.
   *
   * @param objectName     Name of object to set values for. Non-null.
   * @param attributeName  Attribute to specify values for. Non-null.
   * @param value          Value to set. May be null to indicate absent.
   * @throws IllegalArgumentException  If objectName or attributeName is null,
   *                       or if the attribute doesn't exists in this set.
   */
  public void addValue(String objectName, String attributeName, Object value)
  {
    if (objectName == null)
      throw new IllegalArgumentException("objectName cannot be null");

    if (attributeName == null)
      throw new IllegalArgumentException("attributeName cannot be null");

    int attributeNo = attributes_.indexOf(attributeName);
    if (attributeNo == -1)
      throw new IllegalArgumentException("Unknown attribute: " + attributeName);

    // Get all values for this object
    List<List<Object>> objectValues = getOrCreateValues(objectName);

    // Get the values for the specific attribute
    List<Object> baseValues = objectValues.get(attributeNo);

    // Add the new ones
    baseValues.add(value);
  }

  /**
   * Return the objects of this set.
   *
   * @return  The objects of this set. Never null.
   */
  public List<String> getObjects()
  {
    return new ArrayList<>(objects_.keySet());
  }

  /**
   * Return the attribute values for the specified object.
   *
   * @param objectName     Object name to get attribute values for. Non-null.
   * @param attributeName  Attribute name to find values for. Non-null
   * @return               The requested values, or null if the object or attribute
   *                       is not found in this set.
   * @throws IllegalArgumentException  If objectName or attribute is null or
   *                       if the attribute doesn't exists in this set.
   */
  public List<Object> getValues(String objectName, String attributeName)
  {
    if (objectName == null)
      throw new IllegalArgumentException("objectName cannot be null");

    if (attributeName == null)
      throw new IllegalArgumentException("attributeName cannot be null");

    int attributeNo = attributes_.indexOf(attributeName);

    // The attribute doesn't exist
    if (attributeNo == -1)
      return null;

    List<List<Object>> values = objects_.get(objectName);

    // The object doesn't exist
    if (values == null)
      return null;

    return values.get(attributeNo);
  }

  /**
   * Return the (first) attribute value for the specified object.
   * <p>
   * This is a convenience method if the client knows that there is
   * exactly one value for the given attribute/object.
   *
   * @param objectName     Object name to get attribute values for. Non-null.
   * @param attributeName  Attribute name to find values for. Non-null
   * @return               The requested value, or null if the object or attribute
   *                       is not found in this set.
   */
  public Object getValue(String objectName, String attributeName)
  {
    if (objectName == null)
      throw new IllegalArgumentException("objectName cannot be null");

    if (attributeName == null)
      throw new IllegalArgumentException("attributeName cannot be null");

    int attributeNo = attributes_.indexOf(attributeName);

    // The attribute doesn't exist
    if (attributeNo == -1)
      return null;

    List<Object> values = getValues(objectName, attributeName);
    return !values.isEmpty() ? values.get(0) : null;
  }

  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    int[] columnWidths = new int[attributes_.size() + 1];
    for (int columnNo : columnWidths)
      columnWidths[columnNo] = 1;

    // Initialize columns 1,n based on attribute name
    for (int i = 0; i < attributes_.size(); i++) {
      String attribute = attributes_.get(i);
      if (attribute.length() > columnWidths[i + 1])
        columnWidths[i + 1] = attribute.length();
    }

    // Initialize columns 0 based on object name
    for (String objectName : objects_.keySet()) {
      if (objectName.length() > columnWidths[0])
        columnWidths[0] = objectName.length();
    }

    // Initialize columns 1,n based on value size
    for (Map.Entry<String,List<List<Object>>> entry : objects_.entrySet()) {
      List<List<Object>> values = entry.getValue();
      for (int i = 0; i < values.size(); i++) {
        for (List<Object> atomicValue : values) {
          String text = atomicValue.toString();
          if (text.length() > columnWidths[i + 1])
            columnWidths[i + 1] = text.length();
        }
      }
    }

    StringBuilder s = new StringBuilder();

    // Set name
    s.append(name_);

    // Attributes
    s.append('\n');
    for (int i = 0; i < columnWidths.length; i++) {
      String formatString = "%" + columnWidths[i] + "s";
      String text = String.format(formatString, i == 0 ? "" : attributes_.get(i - 1));
      s.append(text);
      s.append(' ');
    }
    s.append('\n');

    // Horizontal line
    for (int i = 0; i < columnWidths.length; i++) {
      for (int j = 0; j < columnWidths[i]; j++)
        s.append('-');
      s.append('-');
    }
    s.append('\n');

    //
    // Object rows
    //
    for (String objectName : objects_.keySet()) {

      // Number of rows for this object
      int nRows = 1;
      for (List<Object> baseValues : objects_.get(objectName)) {
        if (baseValues.size() > nRows)
          nRows = baseValues.size();
      }

      for (int rowNo = 0; rowNo < nRows; rowNo++) {

        // Object name on first row
        String formatString = "%" + columnWidths[0] + "s";
        String text = String.format(formatString, rowNo == 0 ? objectName : "");
        s.append(text);
        s.append(' ');

        // Attribute values
        List<List<Object>> values = objects_.get(objectName);
        for (int i = 0; i < attributes_.size(); i++) {
          List<Object> baseValues = values.get(i);
          Object value = baseValues.size() > rowNo ? baseValues.get(rowNo) : null;
          formatString = "%" + columnWidths[i + 1] + "s";
          text = String.format(formatString, value != null ? value.toString() : "-");
          s.append(text);
          s.append(' ');
        }

        s.append('\n');
      }
    }

    return s.toString();
  }
}
