/*
 * $Id: FDTSessionConfigMsg.java 360 2007-08-16 14:51:52Z ramiro $
 */
package lia.util.net.copy.transport;

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
    public boolean  recursive;
    
    //future? use
    public String dirOffset;
    
    public UUID[]   fileIDs;
    public String[] fileLists;
    public long[]   fileSizes;
    public long[]   lastModifTimes;
    
    public FDTSessionConfigMsg() {

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n destinationDir: ").append(destinationDir);
        sb.append(" recursive: ").append(recursive);
        sb.append(" dirOffset: ").append(dirOffset);
        sb.append("\n UUID[]: ").append(Arrays.toString(fileIDs));
        sb.append("\n fileList[]: ").append(Arrays.toString(fileLists));
        sb.append("\n fileSizes[]: ").append(Arrays.toString(fileSizes));
        sb.append("\n lastModifTimes[]: ").append(Arrays.toString(lastModifTimes));
        return sb.toString();
    }
}
