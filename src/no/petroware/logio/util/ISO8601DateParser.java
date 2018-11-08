package no.petroware.logio.util;

import java.text.ParseException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date parser for the ISO 8601 format.
 * See <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">
 * http://www.w3.org/TR/xmlschema-2/#dateTime</a>
 *
 * @author <a href="mailto:info@petroware.no">Petroware AS</a>
 */
public final class ISO8601DateParser
{
  /**
   * Private constructor to prevent client instantiation.
   */
  private ISO8601DateParser()
  {
    assert false : "This constructor should never be called";
  }

	private static int getIndexOfSign(String str)
  {
		int index = str.indexOf('+');
		return index != -1 ? index : str.indexOf('-');
	}

	private static Calendar parseHour(Calendar calendar, String hourString)
  {
		String basicFormatHour = hourString.replace(":", "");

		int indexOfZ = basicFormatHour.indexOf('Z');
		if (indexOfZ != -1) {
			parseHourWithoutHandlingTimeZone(calendar, basicFormatHour.substring(0, indexOfZ));
		}
    else {
			int indexOfSign = hourString.indexOf('+');
      if (indexOfSign == -1)
        indexOfSign = hourString.indexOf('-');

      if (indexOfSign == -1) {
				parseHourWithoutHandlingTimeZone(calendar, basicFormatHour);
				calendar.setTimeZone(TimeZone.getDefault());
			}
      else {
				parseHourWithoutHandlingTimeZone(calendar, basicFormatHour.substring(0, indexOfSign));
				calendar.setTimeZone(TimeZone.getTimeZone("GMT" + basicFormatHour.substring(indexOfSign)));
			}
		}

		return calendar;
	}

	private static void parseHourWithoutHandlingTimeZone(Calendar calendar, String basicFormatHour)
  {
		basicFormatHour = basicFormatHour.replace(',', '.');
		int indexOfDot = basicFormatHour.indexOf('.');
		double fractionalPart = 0;
		if ( indexOfDot != -1 ){
			fractionalPart = Double.parseDouble("0" + basicFormatHour.substring(indexOfDot));
			basicFormatHour = basicFormatHour.substring(0, indexOfDot);
		}

		if ( basicFormatHour.length() >= 2 ){
			calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(basicFormatHour.substring(0, 2)));
		}

		if ( basicFormatHour.length() > 2 ){
			calendar.set(Calendar.MINUTE, Integer.parseInt(basicFormatHour.substring(2, 4)));
		} else {
			fractionalPart *= 60;
		}

		if ( basicFormatHour.length() > 4 ){
			calendar.set(Calendar.SECOND, Integer.parseInt(basicFormatHour.substring(4, 6)));
		} else {
			fractionalPart *= 60;
		}

		calendar.set(Calendar.MILLISECOND, (int) (fractionalPart * 1000));
	}

	private static Calendar buildCalendarWithDateOnly(String dateString, String originalDate)
    throws ParseException
  {
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.setMinimalDaysInFirstWeek(4);
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		String basicFormatDate = dateString.replaceAll("-", "");

		if (basicFormatDate.indexOf('W') != -1)
			return parseWeekDate(calendar, basicFormatDate);

    else if (basicFormatDate.length() == 7)
			return parseOrdinalDate(calendar, basicFormatDate);

    else
			return parseCalendarDate(calendar, basicFormatDate, originalDate);
  }

	private static Calendar parseCalendarDate(Calendar calendar, String basicFormatDate, String originalDate)
    throws ParseException
  {
		if (basicFormatDate.length() == 2)
			return parseCalendarDateWithCenturyOnly(calendar, basicFormatDate);

    else if (basicFormatDate.length() == 4)
			return parseCalendarDateWithYearOnly(calendar, basicFormatDate);

    else
			return parseCalendarDateWithPrecisionGreaterThanYear(calendar, basicFormatDate, originalDate);
	}

	private static Calendar parseCalendarDateWithCenturyOnly(Calendar calendar, String basicFormatDate)
  {
		calendar.set(Integer.parseInt(basicFormatDate) * 100, 0, 1);
		return calendar;
	}

	private static Calendar parseCalendarDateWithYearOnly(Calendar calendar, String basicFormatDate)
  {
		calendar.set(Integer.parseInt(basicFormatDate), 0, 1);
		return calendar;
	}

	private static Calendar parseCalendarDateWithPrecisionGreaterThanYear(Calendar calendar, String basicFormatDate, String originalDate)
    throws ParseException
  {
		int year = Integer.parseInt(basicFormatDate.substring(0, 4));
		int month = Integer.parseInt(basicFormatDate.substring(4, 6)) - 1;

		if (basicFormatDate.length() == 6) {
			calendar.set(year, month, 1);
			return calendar;
		}

		if (basicFormatDate.length() == 8) {
			calendar.set(year, month, Integer.parseInt(basicFormatDate.substring(6)));
			return calendar;
		}

		throw new ParseException("Can't parse " + originalDate, 0);
	}

	private static Calendar parseWeekDate(Calendar calendar, String basicFormatDate)
  {
		calendar.set(Calendar.YEAR, Integer.parseInt(basicFormatDate.substring(0, 4)));
		calendar.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(basicFormatDate.substring(5, 7)));
		calendar.set(Calendar.DAY_OF_WEEK, basicFormatDate.length() == 7 ? Calendar.MONDAY : Calendar.SUNDAY + Integer.parseInt(basicFormatDate.substring(7)));
		return calendar;
	}

	private static Calendar parseOrdinalDate(Calendar calendar, String basicFormatOrdinalDate)
  {
		calendar.set(Calendar.YEAR, Integer.parseInt(basicFormatOrdinalDate.substring(0, 4)));
		calendar.set(Calendar.DAY_OF_YEAR, Integer.parseInt(basicFormatOrdinalDate.substring(4)));
		return calendar;
	}

  /**
   * Parse the given string in ISO 8601 format and build a Date object.
   *
   * @param  dateString  ISO8601 date string to convert. Non-null.
   * @return A date instance. Never null.
   * @throws IllegalArgumentException  If dateString is null.
   * @throws ParseException If the supplied string is not valid according to ISO8601
   */
  public static Date parse(String dateString)
    throws ParseException
  {
		int indexOfT = dateString.indexOf('T');
    if (indexOfT == -1)
      indexOfT = dateString.indexOf(' ');

    Calendar calendar;

		if (indexOfT == -1) {
			calendar = buildCalendarWithDateOnly(dateString, dateString);
		}
    else {
      calendar = buildCalendarWithDateOnly(dateString.substring(0, indexOfT), dateString);
      calendar = parseHour(calendar, dateString.substring(indexOfT + 1));
    }

    return calendar.getTime();
  }

  /**
   * Generate a ISO 8601 string representation of the specified date.
   *
   * @param date The date to create string representation of. Non-null.
   * @return     String representing the date in the ISO 8601 format. Never null.
   * @throws IllegalArgumentException  If date is null.
   */
  public static String toString(Date date)
  {
    if (date == null)
      throw new IllegalArgumentException("date cannot be null");

    return date.toInstant().toString();
  }

  public static void main(String[] arguments)
  {
    // See https://www.myintervals.com/blog/2009/05/20/iso-8601-date-validation-that-doesnt-suck/
    String[] validDates = new String[] {
      "2009-12T12:34",
      "2009",
      "2009-05-19",
      "2009-05-19",
      "20090519",
      "2009123",
      "2009-05",
      "2009-123",
      "2009-222",
      "2009-001",
      "2009-W01-1",
      "2009-W51-1",
      "2009-W511",
      "2009-W33",
      "2009W511",
      "2009-05-19",
      "2009-05-19 00:00",
      "2009-05-19 14",
      "2009-05-19 14:31",
      "2009-05-19 14:39:22",
      "2009-05-19T14:39Z",
      "2009-W21-2",
      "2009-W21-2T01:22",
      "2009-139",
      "2009-05-19 14:39:22-06:00",
      "2009-05-19 14:39:22+0600",
      "2009-05-19 14:39:22-01",
      "20090621T0545Z",
      "2007-04-06T00:00",
      "2007-04-05T24:00",
      "2010-02-18T16:23:48.5",
      "2010-02-18T16:23:48,444",
      "2010-02-18T16:23:48,3-06:00",
      "2010-02-18T16:23.4",
      "2010-02-18T16:23,25",
      "2010-02-18T16:23.33+0600",
      "2010-02-18T16.23334444",
      "2010-02-18T16,2283",
      "2009-05-19 143922.500",
      "2009-05-19 1439,55"
    };

    String[] invalidDates = new String[] {
      "200905",
      "2009367",
      "2009-",
      "2007-04-05T24:50",
      "2009-000",
      "2009-M511",
      "2009M511",
      "2009-05-19T14a39r",
      "2009-05-19T14:3924",
      "2009-0519",
      "2009-05-1914:39",
      "2009-05-19 14:",
      "2009-05-19r14:39",
      "2009-05-19 14a39a22",
      "200912-01",
      "2009-05-19 14:39:22+06a00",
      "2009-05-19 146922.500",
      "2010-02-18T16.5:23.35:48",
      "2010-02-18T16:23.35:48",
      "2010-02-18T16:23.35:48.45",
      "2009-05-19 14.5.44",
      "2010-02-18T16:23.33.600",
      "2010-02-18T16,25:23:48,444"
    };

    for (String dateString : validDates) {
      try {
        Date date = ISO8601DateParser.parse(dateString);
        System.out.println(date + "      " + ISO8601DateParser.toString(date));
      }
      catch (Exception exception) {
        System.out.println("-- EXCEPTION -- " + exception.getMessage());
      }
    }
  }
}
