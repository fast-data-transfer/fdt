package apmon.host;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

public class Parser {

	private StringTokenizer st = null;
	
	private StringTokenizer auxSt = null;
	
	public Parser() {
	}
	
	public void parse(String text) {
	
		st = new StringTokenizer(text);
	}
	
	public void parseAux(String text) {
		
		auxSt = new StringTokenizer(text);
	}
	
	public void parseFromFile(String fileName) {
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String str = "";
			String line = "";
			while ((line = reader.readLine()) != null)
				str += line+"\n";
			st = new StringTokenizer(str);
		} catch (Exception e) {
			st = null;
		}
	}
	
	public String nextLine() {
		
		if (st == null) return null;
		try {
			return st.nextToken("\n");
		} catch (Exception e) {
			return null;
		}
	}
	
	public String nextAuxLine() {
		
		if (auxSt == null) return null;
		try {
			return auxSt.nextToken("\n");
		} catch (Exception e) {
			return null;
		}
	}
	
	public String nextToken() {
		
		if (st == null) return "";
		try {
			return st.nextToken();
		} catch (Exception e) {
			return null;
		}
	}
	
	public String nextToken(String token) {
		
		if (st == null) return "";
		try {
			return st.nextToken(token);
		} catch (Exception e) {
			return null;
		}
	}
	
	public String nextAuxToken() {
		
		if (auxSt == null) return "";
		try {
			return auxSt.nextToken();
		} catch (Exception e) {
			return null;
		}
	}
	
	public String nextAuxToken(String token) {
		
		if (auxSt == null) return "";
		try {
			return auxSt.nextToken(token);
		} catch (Exception e) {
			return null;
		}
	}
	
	public String getTextAfterToken(String text, String start) {
		if (text.indexOf(start) == -1) return null;
		return text.substring(text.indexOf(start)+start.length());
	}
	
	public String getTextBeforeToken(String text, String end) {
		if (text.indexOf(end) == -1) return text;
		return text.substring(0, text.indexOf(end));
	}
	
	public String getTextBetween(String text, String start, String end) {
		if (text.indexOf(start) == -1) return null;
		text = text.substring(text.indexOf(start)+start.length());
		if (text.lastIndexOf(end) == -1) return text;
		return text.substring(0, text.lastIndexOf(end));
	}
	
	public String[] listFiles(String directory) {
		
		String[] fileList = null;
		try {
			File dir = new File(directory);
			if (!dir.isDirectory()) return null;
			File[] list = dir.listFiles();
			if (list == null) return null;
			fileList = new String[list.length];
			for (int i=0; i<list.length; i++)
				fileList[i] = list[i].getName();
		} catch (Exception e) {
			return null;
		}
		return fileList;
	}
	
} // end of class Parser

