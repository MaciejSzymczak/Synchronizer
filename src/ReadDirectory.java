import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import com.google.api.client.util.Lists;
import com.google.api.services.calendar.model.Calendar;

/**
 * Reads given folder on the disk and loads into memory content of ics files
 * 
 * @author Maciej Szymczak
 * @param  Full file directory
 * @return calendars
 */

public class ReadDirectory {

	static Map calendars = new HashMap();
	final String pathName;
	final String processedPathName;
	String currentFileName;
	
	public static void readFile (String FileName) throws IOException {
		FileInputStream fstream = new FileInputStream(FileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		
		String calName= "";
		String TzId= "";

		String dtStart= "";
		String dtEnd= "";
		String description= ""; 
		String location= "";
		String summary= "";
		
		String strLine;
		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
		  
		  IcsTokenizer isct = new IcsTokenizer(strLine);

		  if (isct.key.equals("END") && isct.value.equals("VTIMEZONE")) {
			  CalendarItem ci = new CalendarItem();
			  ci.TzId = TzId;
			  calendars.put(calName, ci);
		  }
		  
		  if (isct.key.equals("BEGIN") && isct.value.equals("VEVENT"))  {
			  dtStart = "";
			  dtEnd = "";
			  description = "";
			  location = "";
			  summary = "";
		  } 
		  
		  if (isct.key.equals("X-WR-CALNAME"))  calName = isct.value;
		  if (isct.key.equals("TZID"))  TzId = isct.value;
		  if (isct.key.equals("DTSTART"))  dtStart = isct.value;
		  if (isct.key.equals("DTEND"))  dtEnd = isct.value;
		  if (isct.key.equals("DESCRIPTION"))  description = isct.value;
		  if (isct.key.equals("LOCATION"))  location = isct.value;
		  if (isct.key.equals("SUMMARY"))  summary = isct.value;

		  if (isct.key.equals("END") && isct.value.equals("VEVENT"))  {
			  ClassItem icsClass = new ClassItem(dtStart,dtEnd,description,location,summary,"n/a");
			  ((CalendarItem)calendars.get(calName)).classItems.put(icsClass.key, icsClass);
			  System.out.println("    Loading Event ["+calName+"] "+icsClass.key);		  
		  }		  
		}
		br.close();
	}
	
	public static long checksumInputStream(String filepath) throws IOException {
		InputStream inputStreamn = new FileInputStream(filepath);
		CRC32 crc = new CRC32();
		int cnt;
		while ((cnt = inputStreamn.read()) != -1) {
			crc.update(cnt);
		}
		inputStreamn.close();
		return crc.getValue();
	}	
	
	public void readOneIcsFilesFromFolder(final File folder) throws IOException {
	    //delete files already processed
		for (File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	//do not process subfolders
	        	//readIcsFilesFromFolder(fileEntry);
	        } else {	        	
	            System.out.println(folder.getPath()+fileEntry.getName());
	            if (fileEntry.getName().endsWith(".ics")) {
	            	//Ignore identical file
	                currentFileName = fileEntry.getName();
	            	File processedFile = new File(processedPathName+currentFileName);
	            	if (processedFile.exists() && checksumInputStream(processedPathName+fileEntry.getName()) == checksumInputStream(pathName+fileEntry.getName())) {
		            //if (processedFile.exists() ) {
	            		System.out.println("Ignore file because is identical:" + currentFileName);
	            		fileEntry.delete();
	            	}
	            }   
	        }
	    }
    	currentFileName = null;

		//actual processing
	    for (File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	//do not process subfolders
	        	//readIcsFilesFromFolder(fileEntry);
	        } else {	        	
	            System.out.println(folder.getPath()+fileEntry.getName());
	            if (fileEntry.getName().endsWith(".ics")) {
	            	//Ignore identical file
	                currentFileName = fileEntry.getName();
	                readFile(folder.getPath()+"\\"+currentFileName);
	                //return causes only one file from the folder is taken
	                return;
	            }   
	        }
	    }
	}

	
	public void readIcsFilesFromFolder(final File folder) throws IOException {
		//actual processing
	    for (File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	//do not process subfolders
	        	//readIcsFilesFromFolder(fileEntry);
	        } else {	        	
	            System.out.println(folder.getPath()+fileEntry.getName());
	            if (fileEntry.getName().endsWith(".ics")) {
	            	//Ignore identical file
	                currentFileName = fileEntry.getName();
	                readFile(folder.getPath()+"\\"+currentFileName);
	            }   
	        }
	    }
	}	
	
	
    ReadDirectory(String folderName) {
    	pathName = folderName + "\\\\";
    	processedPathName = folderName + "\\\\processed\\\\";
		//folder = new File(folderName);
    }
	
}
