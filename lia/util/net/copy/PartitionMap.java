package lia.util.net.copy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class PartitionMap {
	
    public static final String osname = System.getProperty("os.name");
    
    public static final int getPartition(String fileName) {

        String command=null;
		if (osname.indexOf("Linux") != -1) {
			
			command="stat -L -c \"%d\" \""+fileName+"\"";
		} else if (osname.indexOf("Win") != -1) {
			
		} else if (osname.indexOf("Mac") != -1) {
			
			
			command="stat -L -f \"%Hd\" \""+fileName+"\"";
		}
		if ( command!=null ) {
			
			BufferedReader br = null;
			br = runCommand(command);
			if ( br!=null ) {
				try {
					String sId = br.readLine();
					return Integer.parseInt(sId);
				} catch( Exception ex) {
					
				}
			}
		}
        return 0;
    }


	
	private static BufferedReader runCommand(String cmd) {
		try {
			Process pro = null;

			if (osname.startsWith("Linux") || osname.startsWith("Mac")) {
				pro =
					Runtime.getRuntime().exec(
							new String[] { "/bin/sh", "-c", cmd });
			} else if (osname.startsWith("Windows")) {
				String exehome = System.getProperty("user.home");
				pro = Runtime.getRuntime().exec(exehome + cmd);
			}

			InputStream out = pro.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(out));
			BufferedReader err = new BufferedReader(new InputStreamReader(pro.getErrorStream()));

			String buffer = "";
			String ret = "";
			while((buffer = err.readLine())!= null) {
				ret += buffer+"\n'";
			}

			if (ret.length() != 0){
				return null;
			}

			return br;

		} catch (Exception e) {
			Thread.currentThread().interrupt();
		}

		return null;
	}

	private static Hashtable<String,Integer> hLocations = new Hashtable<String,Integer>();

	
	public static int getPartitionFromCache(String fileName) {
		String dirPath;
		int lastIndex = fileName.lastIndexOf(File.separatorChar);
		if ( lastIndex!=-1 )
			dirPath=fileName.substring(0, lastIndex);
		else
			dirPath="";
		Object value=hLocations.get(dirPath);
		if ( value!=null )
			return ((Integer)value).intValue();
		int val = getPartition(fileName);
		hLocations.put(dirPath, val);
		return val;
	}

}
