/*
 * $Id$
 */
package lia.util.net.copy.transport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The list files msg between FDT peers.
 * @author Raimondas Sirvinskas
 */
public class FDTListFilesMsg implements Serializable {

    public String   listFilesFrom;
    public List<String> filesInDir;

    public FDTListFilesMsg(String listFilesFrom) {
        this.listFilesFrom = listFilesFrom;
        this.filesInDir = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n listFilesFrom: ").append(listFilesFrom);
        sb.append("\n filesInDir: ").append(filesInDir.toString());
        return sb.toString();
    }
}
