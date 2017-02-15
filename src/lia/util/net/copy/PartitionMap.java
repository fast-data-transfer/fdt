/*
 * $Id: PartitionMap.java 535 2009-11-01 22:52:49Z ramiro $
 */
package lia.util.net.copy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * Helper class which determines the real partition ID for a file
 *  
 * @author Lucian Musat
 * @author ramiro
 */
public class PartitionMap {

    /** Logger used by this class */
    private static final Logger logger = Logger.getLogger(PartitionMap.class.getName());

    public static final String osname = System.getProperty("os.name");
    
    public static final int getPartition(String fileName) {

        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " OSName in getPartition ( " + fileName + " ) is " + osname);
        }
        
        String[] command= null;
		if (osname.indexOf("Linux") != -1 || osname.indexOf("SunOS") != -1) {
			//identifies the partition the real file denoted by this path resides on
//          command="/usr/bin/stat -L -c \"%d\" \""+fileName+"\"";
            command = new String[] {"stat", "-L", "-c", "%d", fileName };
		} else if (osname.indexOf("Win") != -1) {
			//not yet implemented!
		} else if (osname.indexOf("Mac") != -1) {
			//identifies the major number for the drive that has the file
			//that means, it identifies the disk, not only the partition
//			command="/usr/bin/stat -L -f \"%Hd\" \""+fileName+"\"";
            command = new String[] {"stat", "-L", "-f", "%Hd", fileName };
		}
		if ( command!=null ) {
		    final String fLine = runICommand(command);
		    if ( fLine!=null ) {
		        try {
		            return Integer.parseInt(fLine);
		        } catch( Throwable t) {
		            if(logger.isLoggable(Level.FINE)) {
		                logger.log(Level.FINE, " [ PartitionMap ] exception parsing line: " + fLine + " for cmd: " + Arrays.toString(command));
		            }
		        }
		    }
		}
        return 0;
    }


	/**
	 * runs a shell command and returns first line if nothing on stderr<br>
	 * TODO: replace with cmdExec.java
	 * @author mluc
	 * @since Aug 31, 2006
	 * @param cmd
	 * @return
	 */
	private static String runICommand(final String[] cmd) {
	    BufferedReader br = null;
	    BufferedReader err = null;
	    
	    String retLine = null;
	    
		try {
			Process pro = null;

			if (osname.startsWith("Linux") || osname.startsWith("Mac") || osname.startsWith("SunOS")) {
				pro = Runtime.getRuntime().exec(cmd, new String[] {"PATH=/bin:/usr/bin:/sbin:/usr/sbin:/usr/local/bin:/usr/local/sbin"});
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
                if(logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " [ PartitionMap ] [ runICommand ] for cmd: " + Arrays.toString(cmd) + " got smth: " + line + " on stderr ... will ignore stdin");
                }
			    ret.append(line).append("\n");
			}

			if (ret.length() == 0){
			    retLine = br.readLine();
                if(logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " [ PartitionMap ] [ runICommand ] for cmd: " + Arrays.toString(cmd) + " read from stdout: " + retLine);
                }
			}

		} catch (Throwable t) {
		    if(logger.isLoggable(Level.FINE)) {
		        logger.log(Level.FINE, " [ PartitionMap ] [ runICommand ] got exception for cmd: " + Arrays.toString(cmd), t);
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
		
        if(logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " [ PartitionMap ] [ runICommand ] for: " + Arrays.toString(cmd) + " returning: {" + retLine + "} ");
        }
        
		return retLine;
	}

	private static Hashtable<String,Integer> hLocations = new Hashtable<String,Integer>();

	/**
	 * stores the directory path to a file and for a second file with the same
	 * directory path will return the last value and will not search for a new one<br>
	 * ATTENTION: if one of two files is a link, then the result returned will probably
	 * not be valid!
	 * @author mluc
	 * @since Sep 18, 2006
	 * @param fileName
	 * @return tag of partition taken from cache if available
	 */
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
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " [ PartitionMap ] [ getPartitionFromCache ] fileName: " + fileName + " partitionID: " + val);
        }
        
		return val;
	}

}
