/*
 * $Id: FDTSessionConfigMsg.java 582 2010-03-01 07:23:28Z ramiro $
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
    public String[] remappedFileLists;
    public long[]   fileSizes;
    public long[]   lastModifTimes;
    
    public FDTSessionConfigMsg() {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n destinationDir: ").append(destinationDir);
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
