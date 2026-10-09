import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

public class Status {
	
	static Map folders = new TreeMap();
	
	class Cnt {
		public int processedCnt;
		public int notProcessedCnt;
		Cnt (int pprocessedCnt, int pnotProcessedCnt) {
			processedCnt = pprocessedCnt;
			notProcessedCnt = pnotProcessedCnt;
		}
		public int getProcessedCnt() {
			return processedCnt;
		}
		public int getNotProcessedCnt() {
			return notProcessedCnt;
		}
	};
	
	public int ReadFolderTree(final File folder) throws IOException {
		//actual processing
		Boolean hasProcessed = false;
		int processedCnt = 0;
		int filesCnt = 0;
	    for (File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	if (fileEntry.getName().equals("processed")) {
	        		hasProcessed = true;
	        	    processedCnt = ReadFolderTree(fileEntry); 
	        	} else ReadFolderTree(fileEntry);
	        } else {	        	
	            //System.out.println(folder.getPath()+fileEntry.getName());
	            if (fileEntry.getName().endsWith(".ics")) {
	            	filesCnt = filesCnt + 1;
	            }   
	        }
	    }
	    if (hasProcessed)
	        folders.put( folder.getName(), new Cnt(processedCnt, filesCnt) );
		return filesCnt;
	}
	
	public void Display(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);			
		fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		fw.write("<?xml-stylesheet type=\"text/xsl\" href=\"layout.xslt\"?>");
		fw.write("<xml>");
		fw.write("<title name=\"Status publikacji rozkładów zajęć\"></title>");
		fw.write("<data>");
		for ( Object k : folders.keySet()) { 
			int p = ((Cnt) folders.get(k)).getProcessedCnt();
			int np = ((Cnt) folders.get(k)).getNotProcessedCnt();			
			fw.write("  <folder name=\""+k+"\" ProcessedCnt=\""+p+"\" NotProcessedCnt=\""+np+"\" icon=\""+(np==0?"check.png":"gear_refresh.png")+"\"/>");
		}
		fw.write("</data>");
		fw.write("<who lastupdatetext=\"Aktualizacja: "+DateFormat.getDateInstance().format(new Date())+"\"></who>");
		fw.write("</xml>");
		fw.close();


	}

	// status.xml is rendered by layout.xslt, but browsers are phasing XSLT out.
	// status.html contains the same layout as layout.xslt produces, rendered directly (no transformation needed).
	public void DisplayHtml(String fileName) throws IOException {
		String title = "Status publikacji rozkładów zajęć";
		Writer fw = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
		fw.write("<!DOCTYPE html>\r\n");
		fw.write("<html>\r\n<head>\r\n<meta charset=\"utf-8\">\r\n<title>"+title+"</title>\r\n</head>\r\n");
		fw.write("<body style=\"font-variant: small-caps; text-align: center; background-color: #eeeeee;\">\r\n");
		fw.write("<h1 style=\"font-variant: small-caps; text-align: center; color: white; background-color: black;\">"+title+"</h1>\r\n");
		fw.write("<center>\r\n");
		fw.write("<table border=\"1\" width=\"30%\" style=\"font-variant: small-caps; border: 0px dashed black\">\r\n");
		fw.write("<tr style=\"background-color: silver\"><td><center>Folder</center></td><td><center>Processed</center></td><td><center>Not Processed</center></td><td><center>Status</center></td></tr>\r\n");
		for ( Object k : folders.keySet()) {
			int p = ((Cnt) folders.get(k)).getProcessedCnt();
			int np = ((Cnt) folders.get(k)).getNotProcessedCnt();
			fw.write("<tr valign=\"top\" style=\"border: 1px dashed silver\">"
					+"<td><center>"+escapeHtml(k.toString())+"</center></td>"
					+"<td><center>"+p+"</center></td>"
					+"<td><center>"+np+"</center></td>"
					+"<td><center><img src=\""+(np==0?"check.png":"gear_refresh.png")+"\"/></center></td>"
					+"</tr>\r\n");
		}
		fw.write("</table>\r\n");
		fw.write("</center>\r\n");
		fw.write("<br/><small>Aktualizacja: "+DateFormat.getDateInstance().format(new Date())+"</small>\r\n");
		fw.write("</body>\r\n</html>\r\n");
		fw.close();
	}

	private static String escapeHtml(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}
