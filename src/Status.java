import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

}
