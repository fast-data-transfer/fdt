/*
 * $Id: ProcessorInfo.java 656 2012-02-24 13:51:44Z ramiro $
 */
package lia.util.net.copy.filters;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * 
 * This class encapsulates the file list which is to be, or has been transfered
 * and the destination directory.
 *  
 * @author ramiro
 */
public class ProcessorInfo {
    
    public String[] fileList;
    public String destinationDir;
    public InetAddress remoteAddress;
    public int remotePort;
    public boolean recursive;

    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProcessorInfo [fileList=")
               .append(Arrays.toString(fileList))
               .append(", destinationDir=")
               .append(destinationDir)
               .append(", remoteAddress=")
               .append(remoteAddress)
               .append(", remotePort=")
               .append(remotePort)
               .append(", recursive=")
               .append(recursive)
               .append("]");
        return builder.toString();
    }
    
}
