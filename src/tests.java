import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;

/**
 * Place your unit tests here
 * 
 * @author Maciej Szymczak
 */
public class tests {	
	
	  private static EventDateTime ISOStringToEventDateTime (String ISOString) throws ParseException {
			DateFormat df = new SimpleDateFormat("yyyyMMdd hhmm");
			Date x = df.parse("20161019 1530");
			DateTime dt = new DateTime(x /*, TimeZone.getTimeZone("UTC")*/ );
			return new EventDateTime().setDateTime(dt);
	  }	
	
	public static void main(String[] args) throws IOException, ParseException {

		ISOStringToEventDateTime("20161019T153000Z");
		
		
		
		//Test: IcsTokenizer
		//IcsTokenizer isct = new IcsTokenizer("X-WR-CALNAME:xx");
		//System.out.println ("key="+isct.key);
		//System.out.println ("value="+isct.value);
	
		
		//DateFormat df = new SimpleDateFormat("yyyyMMdd hhmm");
		//Date x = df.parse("20161124T1340".replaceAll("T", " "));
		////DateTime dt = new DateTime(x, TimeZone.getTimeZone("UTC"));
		//DateTime dt = new DateTime(x);
		//System.out.println(dt.toStringRfc3339().replace("-","").replace(":", "").substring(0, 13));		
		
		//Test: ReadDirectory
		//ReadDirectory rd = new ReadDirectory("C:\\Users\\Maciek\\planowanie\\documents\\Semestry");				
		//rd.readIcsFilesFromFolder(rd.folder);
		//list calendars
		//for(Object x: rd.calendars.keySet()) {
		//	System.out.println((String)x);
		//}	
		
		
	}

}
