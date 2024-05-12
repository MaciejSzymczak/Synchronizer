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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.CRC32;

/**
 * Main class of the project. This class uploads ICS files from given folder into Google Calendar
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

	  private static final String APPLICATION_NAME = "Calendar";
	  private static java.io.File DATA_STORE_DIR;
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
	    System.out.println(clientSecrets.getDetails());
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
	    //System.out.println(new AuthorizationCodeInstalledApp(flow, lsr).getReceiver().toString());
	    
	    return new AuthorizationCodeInstalledApp(flow, lsr).authorize("user"); 
	  }

	  public static void main(String[] args) {
	    try {
	    	
	    	System.out.println( System.getProperty("user.home") );

	    	
	    	System.out.println("Cello, ver 2024.05.12");
	    	System.out.println("Software Factory Maciej Szymczak, All Rights reserved");
	    	
    		System.out.println("Parameter count "+args.length );	    			    	
	    	for (int i = 0; i < args.length; i++) { 
	    		System.out.println("Parameter "+i+" is "+ args[i] );	    		
	    	}
	    	
            String client_secrets = "";
	    	String actionName = "";
	    	String folderName = "";
	    		    	

	    	if (args.length==0) {
	    		System.out.println("Usage: java cello.jar uploadIcs json folderName");
	    		System.out.println("   or  java cello.jar deleteCalendars json ");
	    		System.out.println("   or  java cello.jar status folderName");
	    		System.exit(1);
	    	}
	    	actionName = args[0];

	    	if (actionName.equals("uploadIcs") || actionName.equals("deleteCalendars") || actionName.equals("readGoogleCalendars") ) 
		    	client_secrets = args[1];

	    	if (actionName.equals("uploadIcs"))
	        	folderName = args[2];

	    	if (actionName.equals("status"))
	        	folderName = args[1];

	    	
			if (actionName.equals("status")) {
				System.out.println("Preparing Status");
				Status s = new Status();
				s.ReadFolderTree( new File(folderName) );
				s.Display(folderName+"\\status.xml");
				System.out.println("Done");
				System.exit(0);
			} 	
	    	
			//create subfolder processed
			File folder = new File(folderName+"\\processed");
			if(!folder.exists()) if(folder.mkdir());

			//Login to Google cloud
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			
			//set DATA_STORE_DIR = name of the json file
			int index = client_secrets.lastIndexOf("\\");
			String storageFolderName = client_secrets.substring(index+1, client_secrets.lastIndexOf("."));			
			System.out.println("DATA_STORE_DIR=" + System.getProperty("user.home")+ ".store/" + storageFolderName);			
			DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store/" + storageFolderName );

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
			
			//tests
			if (actionName.equals("readGoogleCalendars")) {
				readGoogleCalendars();
				System.out.println("Done");
				System.exit(0);
			}

			readGoogleCalendars();
			
		    //Read Ics calendars
			System.out.println("Loading ics calendars...");
			ReadDirectory ics = new ReadDirectory(folderName);				
			ics.readOneIcsFilesFromFolder( new File(folderName) );
			
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
						
			//move processed file to folder processed
			if (ics.currentFileName != null) {
				File afile =new File(folderName +  "\\" + ics.currentFileName);
				File processedFile = new File(folderName +  "\\processed\\" + ics.currentFileName);
				if (processedFile.exists()) { processedFile.delete(); } 
		    	afile.renameTo(new File(folderName +  "\\processed\\" + ics.currentFileName));			
			    System.out.println("File processed:" + ics.currentFileName);
			    
			} else {
			    System.out.println("No files to process");					
			}
			
			
			ReadDirectory icsProcessed = new ReadDirectory(folderName+  "\\processed");				
			icsProcessed.readIcsFilesFromFolder( new File(folderName+  "\\processed") );			
			tableofContents(icsProcessed, folderName+"\\ListOfCalendars.js");
			
	    //} catch (IOException e) {
	    //  System.err.println(e.getMessage());
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

	  private static void readGoogleCalendars() throws IOException  {
		    
           		  
		    CalendarList feed = client.calendarList().list().setMaxResults(250).execute();
		    Boolean inLoop = true;
		    
		    //System.out.println("*** before loop");
		    while (inLoop) {
			    if (feed.getItems() != null) {
			        for (CalendarListEntry entry : feed.getItems()) {
			          //System.out.println( "Calendar: " + entry.getSummary());
					  CalendarItem ci = new CalendarItem();
					  ci.calendarId = entry.getId();
			          googleCalendars.put(entry.getSummary(), ci);
			        }
			      }	
			    
			    System.out.println("*** feed.getNextPageToken:" + feed.getNextPageToken() );			    
			    if (feed.getNextPageToken() !=null) {
			    	feed = client.calendarList().list().setPageToken(feed.getNextPageToken()).setMaxResults(250).execute();
				    inLoop = true;
			    } else {
			    	inLoop = false;
			    }
			//try {Thread.sleep(4000);} catch (InterruptedException e) {}    
		    }
		    //System.out.println("*** after loop");
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
				    	//System.out.println("    Loading Event ["+calName+"] "+googleClass.key);
				    	System.out.print("@");
						((CalendarItem)googleCalendars.get(calName)).classItems.put(googleClass.key, googleClass);
				    }					    
				} else {
					System.out.println("Addding calendar "+calName);	
				    Calendar newEntry = new Calendar();
				    newEntry.setSummary(calName);
				    newEntry.setDescription("Kalendarz został utworzony za pomocą Plansoft.org\nCalendar has been created by Plansoft.org");
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
		  FileWriter fwErrors = new FileWriter(fileName+".err");
		  
		  System.out.println( String.format("Creating file %s...",fileName));		  

		  String separator = "";
		  String separatore = "";
		  fw.write("var data = [\n");
		  fwErrors.write("var data = [\n");
		  for(Object entry: ics.calendars.keySet()) {
				String calName = (String)entry;
				//Do not publish calendars of lecturers
				if ( calName.contains("Wykładowca") ) continue;
				String calendarId= "*** Calendar not found ***";
				try {
					calendarId = ((CalendarItem)googleCalendars.get(calName)).calendarId;
				} catch (Exception e) {}
				if ((calendarId+"").length()==0) 
					calendarId= "*** Calendar not found ***";
				
				if (calendarId.equals("*** Calendar not found ***")) {
				  System.out.println( String.format("Calendar has been deleted in Google: %s...",calName));		  
				  fwErrors.write(String.format("%s{ calendarName: '%s', link: 'https://calendar.google.com/calendar/ical/%s/public/basic.ics', calendarId: '%s' }", separatore, calName, calendarId, calendarId ));
				  separatore=",";
				} else {
				  fw.write(String.format("%s{ calendarName: '%s', link: 'https://calendar.google.com/calendar/ical/%s/public/basic.ics', calendarId: '%s' }", separator, calName, calendarId, calendarId ));
				  separator=",";
				}
				}		  
		  fw.write("];");
		  fw.close();		  
		  fwErrors.write("];");
		  fwErrors.close();		  
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