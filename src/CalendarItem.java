import java.util.HashMap;
import java.util.Map;

import com.google.api.client.util.Lists;

/**
 * ics calendar / Google calendar along with all calendar events
 * used by UploadICS.
 * 
 * @author Maciej Szymczak
 */
public class CalendarItem {
	String calendarId;
	String TzId;
    Map classItems = new HashMap();
}
