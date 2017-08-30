/*
 * $Id$
 */
package lia.util.net.copy.transport;

import lia.util.net.common.Config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

/**
 * The config msg between FDT peers. 
 * @author ramiro
 */
public class FDTSessionConfigMsg implements Serializable {

    private static final long serialVersionUID = 691756564099111644L;
    
    
    public String   destinationDir;
    public String   destinationIP;
    public int   destinationPort;
    public boolean  recursive;
    
    //future? use
    public String dirOffset;

    public String sourceIP;
    
    public UUID[]   fileIDs;
    public String[] fileLists;
    public String[] remappedFileLists;
    public long[]   fileSizes;
    public long[]   lastModifTimes;
    
    public FDTSessionConfigMsg() {

    }

    public FDTSessionConfigMsg(Config config) {
        this.destinationIP = config.getDestinationIP();
        this.destinationDir = config.getDestinationDir();
        this.sourceIP = config.getSourceIP();
        this.fileLists = config.getFileList();
        this.destinationPort = config.getDestinationPort();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n destinationDir: ").append(destinationDir);
        sb.append("\n destinationIP: ").append(destinationIP != null ? destinationIP : "");
        sb.append("\n destinationPort: ").append(String.valueOf(destinationPort));
        sb.append("\n sourceIP: ").append(sourceIP != null ? sourceIP : "");
        sb.append(" recursive: ").append(recursive);
        sb.append(" dirOffset: ").append(dirOffset);
        sb.append("\n UUID[]: ").append(Arrays.toString(fileIDs));
        sb.append("\n fileList[]: ").append(Arrays.toString(fileLists));
        sb.append("\n remappedFileLists[]: ").append(Arrays.toString(remappedFileLists));
        sb.append("\n fileSizes[]: ").append(Arrays.toString(fileSizes));
        sb.append("\n lastModifTimes[]: ").append(Arrays.toString(lastModifTimes));
        return sb.toString();
    }
}
