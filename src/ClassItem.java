/**
 * Event in ics calendar / google calendar. Used by class CalendarItem.
 * 
 * @author Maciej Szymczak
 */
public class ClassItem {
	String dtStart;
	String dtEnd;
	String description; 
	String location;
	String summary;
	String key;
	//Google event id used when event needs to be deleted
    String eventId; 
	
	ClassItem(
			  String pdtStart
			, String pdtEnd
			, String pDescription 
			, String pLocation
			, String pSummary
			, String pEventId
	) {
		dtStart     = pdtStart.substring(0, 13);
		dtEnd       = pdtEnd.substring(0, 13);
		description = pDescription+""; 
		location    = (pLocation+"").replace("null", "");
		summary     = pSummary+"";
		eventId     = pEventId;
		key         = dtStart.replace("00Z", "")+":"+
					  dtEnd.replace("00Z", "")+":"+
					  description +":"+
					  location+":"+
					  summary;
	}
}
