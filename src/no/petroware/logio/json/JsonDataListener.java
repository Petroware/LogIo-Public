package no.petroware.logio.json;

/**
 * Provides a mechanism for the client to monitor and process data
 * <em>during</em> the JSON read operation.
 * <p>
 * Convenient for handling JSON files that are larger than physical
 * memory. In this case the client should <em>clear</em> the JSON
 * file instance at each invocation:
 *
 * <pre>
 *   class DataListener implements JsonDataListener
 *   {
 *      &#64;Override
 *      public void dataRead(JsonFile jsonFile)
 *      {
 *         // Process log data
 *         :
 *
 *         // Clear curve data to save memory
 *         jsonFile.clearCurves();
 *      }
 *    }
 * </pre>
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public interface JsonDataListener
{
  /**
   * A notification from {@link JsonReader} indicating that a new
   * portion of data has been read into the specified JSON file.
   * <p>
   * After the client has processed the data, it may clean the curve data
   * in order to save memory storage. See {@link JsonFile#clearCurves}.
   * <p>
   * @see JsonReader#read(boolean,boolean,JsonDataListener)
   *
   * @param jsonFile  JSON file that has been populated with new data.
   *                  Never null.
   */
  public void dataRead(JsonFile jsonFile);
}
