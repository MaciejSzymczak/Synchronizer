import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

		FileInputStream fstream = new FileInputStream("C:/Users/Maciek/Planowanie/documents/Semestry/GRUPA_CW_2.ics");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		while ((strLine = br.readLine()) != null)   {
			  System.out.print(strLine.replace("\\r\\n", "\r\n")+"\r\n"+"a kuku");
		}
		br.close();
		
		
	}

}
