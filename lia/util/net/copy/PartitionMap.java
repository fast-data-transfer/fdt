
package lia.util.net.copy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PartitionMap {

    
    private static final Logger logger = Logger.getLogger(PartitionMap.class.getName());

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
		    final String fLine = runICommand(command);
		    if ( fLine!=null ) {
		        try {
		            return Integer.parseInt(fLine);
		        } catch( Throwable t) {
		            if(logger.isLoggable(Level.FINE)) {
		                logger.log(Level.FINE, " [ PartitionMap ] exception parsing line: " + fLine + " for cmd: " + command);
		            }
		        }
		    }
		}
        return 0;
    }


	
	private static String runICommand(final String cmd) {
	    BufferedReader br = null;
	    BufferedReader err = null;
	    
	    String retLine = null;
	    
		try {
			Process pro = null;

			if (osname.startsWith("Linux") || osname.startsWith("Mac")) {
				pro = Runtime.getRuntime().exec(cmd);
			} else if (osname.startsWith("Windows")) {
				String exehome = System.getProperty("user.home");
				pro = Runtime.getRuntime().exec(exehome + cmd);
			}

			if(pro == null) {
			    return null;
			}
			
			br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
			err = new BufferedReader(new InputStreamReader(pro.getErrorStream()));

			String line = null;
			StringBuilder ret = new StringBuilder();
			while((line = err.readLine())!= null) {
			    ret.append(line).append("\n");
			}

			if (ret.length() == 0){
			    retLine = br.readLine();
			}

		} catch (Throwable t) {
		    if(logger.isLoggable(Level.FINE)) {
		        logger.log(Level.FINE, " [ PartiotionMap ] [ runICommand ] got exception for cmd: " + cmd, t);
		    }
		    retLine = null;
		} finally {
            if(err != null) {
                try {
                    err.close();
                }catch(Throwable _) {}
            }
            
            if(br != null) {
                try {
                    br.close();
                }catch(Throwable _) {}
            }
		}
		
		return retLine;
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
