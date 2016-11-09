	import com.google.api.client.auth.oauth2.Credential;
	import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
	import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
	import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
	import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
	import com.google.api.client.googleapis.batch.BatchRequest;
	import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
	import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
	import com.google.api.client.googleapis.json.GoogleJsonError;
	import com.google.api.client.http.HttpHeaders;
	import com.google.api.client.http.HttpTransport;
	import com.google.api.client.json.JsonFactory;
	import com.google.api.client.json.jackson2.JacksonFactory;
	import com.google.api.client.util.DateTime;
	import com.google.api.client.util.Lists;
	import com.google.api.client.util.store.DataStoreFactory;
	import com.google.api.client.util.store.FileDataStoreFactory;
	import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.calendar.model.AclRule.Scope;
import com.google.api.services.calendar.model.Calendar;
	import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
	import com.google.api.services.calendar.model.EventDateTime;
	import com.google.api.services.calendar.model.Events;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
	import java.io.InputStreamReader;
import java.security.acl.AclEntry;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
	import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Main class of the project. This class uploads ISC files from given folder into Google Calendar
 * 
 * There are two instances of class CalendarItem:
 * - googleCalendars
 * - ics.calendars
 * Class UploadICS compares the content of googleCalendars and ics.calendars and does the synchronization
 * - for each icsClass: no googleEvent found ==> insert	
 * - for each googleEvent: no icsItem found => delete
 * 
 * @author Maciej Szymczak
 */

	public class Synchronizer {

	  private static final String APPLICATION_NAME = "";
	  private static final java.io.File DATA_STORE_DIR =
	      new java.io.File(System.getProperty("user.home"), ".store/calendar_sample");
	  private static FileDataStoreFactory dataStoreFactory;
	  private static HttpTransport httpTransport;
	  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	  private static com.google.api.services.calendar.Calendar client;
	  static final java.util.List<Calendar> addedCalendarsUsingBatch = Lists.newArrayList();

	  static Map googleCalendars = new HashMap();

	private static Credential authorize(String client_secrets) throws Exception {
	    // load client secrets
		System.out.println("client_secrets:" + client_secrets);
		FileInputStream fis = new FileInputStream(client_secrets);
	    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
	        new InputStreamReader(fis)); //CalendarExample.class.getResourceAsStream("/client_secrets.json") 
	    if (clientSecrets.getDetails().getClientId().startsWith("Enter")
	        || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
	      System.out.println(
	          "Enter Client ID and Secret from https://code.google.com/apis/console/?api=calendar "
	          + "into calendar-cmdline-sample/src/main/resources/client_secrets.json");
	      System.exit(1);
	    }
	    // set up authorization code flow
	    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	        httpTransport, JSON_FACTORY, clientSecrets,
	        Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
	        .build();
	    // authorize
	    LocalServerReceiver lsr  = new LocalServerReceiver();
	    return new AuthorizationCodeInstalledApp(flow, lsr).authorize("user"); 
	  }

	  public static void main(String[] args) {
	    try {
	    	
	    	System.out.println("Ics synchronizer, ver 2016.11.05");
	    	System.out.println("Software Factory Maciej Szymczak, All Rights reserved");
	    	
    		System.out.println("Parameter count "+args.length );	    			    	
	    	for (int i = 0; i < args.length; i++) { 
	    		System.out.println("Parameter "+i+" is "+ args[i] );	    		
	    	}
	    	
            String client_secrets = "";
	    	String actionName = "";
	    	String folderName = "";
	    	Boolean mockMode = false;
	    	
	    	if (args.length==0 && getComputerName().equals("LAPTOP-S6GP7AMU")) {
	    		mockMode = true;
	    		client_secrets = "C:\\Users\\Maciek\\Desktop\\tests\\client_secrets.json";
		    	actionName = "uploadIcs"; //uploadIcs or deleteCalendars
		    	folderName = "C:\\Users\\Maciek\\planowanie\\documents\\Semestry";
	    	}	    
	    	
	    	if (!mockMode) {
		    	if (args.length==0) {
		    		System.out.println("Usage: java uploadIcs synchronize <folder name>");
		    		System.out.println("   or  java deleteCalendars");
		    		System.exit(1);
		    	}
		    	client_secrets = args[0];
		    	actionName = args[1];
		    	if (actionName.equals("uploadIcs"))
		        	folderName = args[2];
	    	}	
	    	
	    	//Login to Google cloud
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			Credential credential = authorize(client_secrets);
			client = new com.google.api.services.calendar.Calendar.Builder(
			    httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
			System.out.println("Login succeed");
			
			if (actionName.equals("deleteCalendars")) {
				System.out.println("Delete Google Calendars");
				deleteGoogleCalendars();
				System.out.println("Done");
				System.exit(0);
			}

			System.out.println("Ics synchronizer");
			
			readGoogleCalendars();
			
		    //Read Ics calendars
			System.out.println("Loading ics calendars...");
			ReadDirectory ics = new ReadDirectory(folderName);				
			ics.readIcsFilesFromFolder(ics.folder);
			
			//this procedure also gets GoogleEvents
			AddGoogleCalendars(ics);
			
			//for each icsClass: no googleEvent found ==> insert	
			System.out.println("Inserting events...");
			for(Object entry: ics.calendars.keySet()) {
				String calName = (String)entry;
				CalendarItem icsCalendar = (CalendarItem)ics.calendars.get(calName);
				for(Object o: icsCalendar.classItems.values()) {
					ClassItem icsClass = (ClassItem)o;					
					//no Event found ==> insert	
					CalendarItem googleCalendar = (CalendarItem)googleCalendars.get(calName);
					if (!(googleCalendar).classItems.containsKey(icsClass.key)) {
						System.out.println("    Inserting Event ["+calName+"] "+icsClass.key);
						createGoogleEvent(googleCalendar.calendarId, icsClass);
					}
				}
			}	
						
			//	for each googleEvent: no icsItem found => delete
			System.out.println("Deleting events...");
			for(Object entry: googleCalendars.keySet()) {
				String calName = (String)entry;
				CalendarItem googleCalendar = (CalendarItem)googleCalendars.get(calName);
				for(Object o: googleCalendar.classItems.values()) {
					ClassItem googleClass = (ClassItem)o;
					//no Event found ==> delete	
					CalendarItem icsCalendar = (CalendarItem)ics.calendars.get(calName);
					if (!(icsCalendar).classItems.containsKey(googleClass.key)) {
						System.out.println("    Deleting Event ["+calName+"] "+googleClass.key);
						client.events().delete(googleCalendar.calendarId, googleClass.eventId).execute();
					}
				}
			}
			
			tableofContents(ics, folderName+"\\ListOfCalendars.html");
			
	    } catch (IOException e) {
	      System.err.println(e.getMessage());
	    } catch (Throwable t) {
	      t.printStackTrace();
	    }
	    System.out.println("Done");	
	    System.exit(1);
	  }
	  
	  private static EventDateTime ISOStringToEventDateTime (String ISOString) throws ParseException {
			DateFormat df = new SimpleDateFormat("yyyyMMdd HHmm");
			DateTime dt = new DateTime(df.parse(ISOString.replaceAll("T", " ").substring(0, 13)) /*, TimeZone.getTimeZone("UTC")*/ );
			//System.out.println(dt + " " + ISOString);	
			return new EventDateTime().setDateTime(dt);
	  }
	  
	  
	  private static void createGoogleEvent(String calendarId, ClassItem icsClass) throws ParseException, IOException {
		    Event event = new Event();	
		    event.setStart( ISOStringToEventDateTime( icsClass.dtStart ) );
		    event.setEnd( ISOStringToEventDateTime( icsClass.dtEnd ) );
		    event.setDescription( icsClass.description ); //.replace("\\n", "\r\n") 
		    event.setLocation( icsClass.location );
		    event.setSummary( icsClass.summary );
		    
		    client.events().insert(calendarId, event).execute();
	  }
	  
	  private static void deleteGoogleCalendars() throws IOException {
		    CalendarList feed = client.calendarList().list().execute();
		    if (feed.getItems() != null) {
		        for (CalendarListEntry entry : feed.getItems()) {
		          if ((entry.getDescription()+"").toLowerCase().contains("plansoft.org".toLowerCase())) {
			         System.out.println("Delete calendar " + entry.getSummary() );
		        	 client.calendars().delete(entry.getId()).execute();
		          }
		        }
		      }		    		  
	  }

	  private static void readGoogleCalendars() throws IOException {
		    CalendarList feed = client.calendarList().list().execute();
		    if (feed.getItems() != null) {
		        for (CalendarListEntry entry : feed.getItems()) {
		          //System.out.println( entry.getSummary());
				  CalendarItem ci = new CalendarItem();
				  ci.calendarId = entry.getId();
		          googleCalendars.put(entry.getSummary(), ci);
		        }
		      }		    		  
	  }
	  
	  private static void AddGoogleCalendars(ReadDirectory ics) throws IOException {
		  System.out.println("Addding calendars...");	
		  for(Object entry: ics.calendars.keySet()) {
				String calName = (String)entry;
				if (googleCalendars.containsKey(calName)) {
					String calendarId = ((CalendarItem)googleCalendars.get(calName)).calendarId;
					System.out.println("Calendar \""+calName+"\" exists, loading Google events");
				    Events feed = client.events().list(calendarId).setMaxResults(2500).execute();
				    for (Event googleEvent : feed.getItems()) {
				    	ClassItem googleClass = new ClassItem(
				    		  	 googleEvent.getStart().getDateTime().toStringRfc3339().replace("-","").replace(":", "").substring(0, 13)
				    			,googleEvent.getEnd().getDateTime().toStringRfc3339().replace("-","").replace(":", "").substring(0, 13)
				    			,googleEvent.getDescription()
				    			,googleEvent.getLocation()
				    			,googleEvent.getSummary()
				    			,googleEvent.getId()
				    			);
				    	System.out.println("    Loading Event ["+calName+"] "+googleClass.key);
						((CalendarItem)googleCalendars.get(calName)).classItems.put(googleClass.key, googleClass);
				    }					    
				} else {
					System.out.println("Addding calendar "+calName);	
				    Calendar newEntry = new Calendar();
				    newEntry.setSummary(calName);
				    newEntry.setDescription("Kalendarz został utworzony za pomocą www.Plansoft.org\nCalendar has been created by www.Plansoft.org");
				    newEntry.setTimeZone(  ((CalendarItem)ics.calendars.get(calName)).TzId  ); 
				   //client.acl().insert(calendarId, content)
				    Calendar result = client.calendars().insert(newEntry).execute();
				    //make the calendar public
				    AclRule rule = new AclRule();
				    Scope scope = new Scope();
				    scope.setType("default");
				    rule.setScope(scope).setRole("reader");
				    client.acl().insert(result.getId(), rule).execute();
				    
					CalendarItem ci = new CalendarItem();
					ci.calendarId = result.getId();
					googleCalendars.put(calName, ci);
				    
				    //View.display(result);
				}				
			}
	  }

	  private static void tableofContents(ReadDirectory ics, String fileName) throws IOException {
		  FileWriter fw = new FileWriter(fileName);
		  
		  System.out.println( String.format("Creating file %s...",fileName));	
		  fw.write("<!DOCTYPE html>\n<html lang=\"pl\">\n<body>\n");
		  fw.write("<h1 style=\"font-variant: small-caps; text-align: center; color: white; background-color: black; \">Lista kalendarzy do zaimportowania do Twojego kalendarza Google</h1>"); 
		  fw.write(String.format("<a href=\"%s\">%s</a><br/><br/>\n", "https://support.google.com/a/users/answer/178357?hl=pl", "Dodawanie kalendarza udostępnionego przez inną osobę"));
		  
		  fw.write("<table  border=\"1\" width=\"80%\" style=\"font-variant: small-caps; border: 1px dashed silver\">\n");
		  for(Object entry: ics.calendars.keySet()) {
				String calName = (String)entry;
				String calendarId = ((CalendarItem)googleCalendars.get(calName)).calendarId;
				fw.write("<tr><td>");
				fw.write(String.format("<a href=\"https://calendar.google.com/calendar/ical/%s/public/basic.ics\">%s</a>", calendarId, calName));
				fw.write("</td><td>");
				fw.write(String.format("https://calendar.google.com/calendar/ical/%s/public/basic.ics", calendarId));
				fw.write("</td></tr>");
				}		  
		  fw.write("</table>\n");
		  fw.write("</body>\n</html>");
		  fw.close();		  
	  }
	  
	  
		  /*
		   Batch processing hits the API limits https://support.google.com/a/answer/2905486?hl=en
		   As no viable workaround was found the batch processing although very fast, has been abandoned.
		   
		  private static void AddMissingGoogleCalendarsBatch(ReadDirectory rd, Map googleCalendars) throws IOException {
		    BatchRequest batch = client.batch();
		    JsonBatchCallback<Calendar> callback = new JsonBatchCallback<Calendar>() {

		      @Override
		      public void onSuccess(Calendar calendar, HttpHeaders responseHeaders) {
		        //View.display(calendar);
		        //addedCalendarsUsingBatch.add(calendar);
		      }

		      @Override
		      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
		        System.out.println("Error Message: " + e.toPrettyString() +" Response: "+responseHeaders.toString()  );
		      }
		    };

		    Boolean executeAllowed = false;
			for(Object entry: rd.calendars.keySet()) {
				String calName = (String)entry;
				if (googleCalendars.containsKey(entry)) {
					System.out.println("Calendar found:"+calName);					
				} else {
					System.out.println("Addding "+calName);	
					executeAllowed = true;
				    Calendar newEntry = new Calendar().setSummary(calName);
				    client.calendars().insert(newEntry).queue(batch, callback);
				}				
			}	
	        
			if (executeAllowed)
				batch.execute();
		  }
		  */		  
		  	  
		private static String getComputerName()
		{
		    Map<String, String> env = System.getenv();
		    if (env.containsKey("COMPUTERNAME"))
		        return env.get("COMPUTERNAME");
		    else if (env.containsKey("HOSTNAME"))
		        return env.get("HOSTNAME");
		    else
		        return "Unknown Computer";
		}	  
	  
	  
	}