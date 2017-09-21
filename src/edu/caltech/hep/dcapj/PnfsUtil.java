/**
 * A utility class for extracting useful information
 * from the pnfs filesystem by reading pnfs files.
 *
 * @author Faisal Khan
 * @version 0.1
 */

package edu.caltech.hep.dcapj;

import java.io.*;
import java.util.Vector;

public class PnfsUtil {

    /** Enables/disables debug mode. */
    public static boolean DEBUG = false;
    private static String PATH_SEPARATOR = "/";

    /**
     * PNFS ID is extracted by reading .(id)(filename) under the parent directory of
     * the given path.
     */
    public static String getPnfsID(String path) throws FileNotFoundException,
            IOException {

        File pnfsPath = new File(path);
        File dir = pnfsPath.getParentFile();

        if (!dir.isDirectory())
            throw new FileNotFoundException(
                    "Invalid pnfs path: Unable to extract parent directory");

        //fomat = /path-to-pnfs file-parents directory/.(id)(pnfs-file-name)
        String pnfslayer = dir.getPath() + "" + PATH_SEPARATOR + ".(id)("
                + pnfsPath.getName() + ")";

        if (DEBUG)
            System.err.println("Reading pnfslayer's file: " + pnfslayer);

        FileInputStream fis = new FileInputStream(pnfslayer);
        byte idbytes[] = new byte[1024];
        int rcount = fis.read(idbytes);

        if (DEBUG)
            System.out.println("Number of bytes read = " + rcount);

        if (rcount < 0)
            throw new IOException("Couldn't read pnfslayer file " + pnfslayer);

        return new String(idbytes).trim();
    }

    /**
     * Check whether a given path is a valid PNFS path.
     * @param path The path to check.
     * @return <code>true</code> if the path is a valid PNFS path, <code>false</code> otherwise.
     */
    public static boolean isPnfs(String path) {
        File pnfsPath = new File(path);
        File dir = pnfsPath.getParentFile();

        //file/path should exist as the remaining test are based on directory.
        if (!pnfsPath.exists()) {
            if (DEBUG)
                System.out.println("Requested path or file doesn't exist. " + pnfsPath);

            return false;
        }

        if (!dir.isDirectory()) {
            if (DEBUG)
                System.out
                        .println("Invalid pnfs path: Unable to extract parent directory");
            return false;
        }

        //format = /path-to-pnfs-file's parent direcoty/.(get)(cursor)
        String pnfslayer = dir.getPath() + "" + PATH_SEPARATOR
                + ".(get)(cursor)";

        if (DEBUG)
            System.err.println("Checking  pnfslayer's file: " + pnfslayer);

        File fpnfslayer = new File(pnfslayer);
        //if the pnfs layer file exists and we can read it
        //it means it is pnfs file system.
        if (fpnfslayer.exists() && fpnfslayer.canRead())
            return true;
        else
            return false;
    }

    /**
     * Get all dCap doors from the PNFS server.
     * @param path The PNFS path.
     * @return The addresses of the dCap doors in a <code>String</code> array.
     * @throws FileNotFoundException If the path is invalid.
     * @throws IOException If there was an error getting the dCap doors.
     */
    public static String[] getdCapDoors(String path)
            throws FileNotFoundException, IOException {

        File pnfsfile = new File(path);
        File dir = pnfsfile.getParentFile();

        if (DEBUG) {
            System.out.println("Querying dCache's door for pnfs path " + path);
            System.out.println("Checking if it is pnfs path");

        }

        //path should exist and it should belong to pnfs layer.
        if (!isPnfs(path)) {

            if (DEBUG)
                System.out.println("Not a valid pnfs path : " + path);

            return null;
        }

        //It's a pnfs path, going to read the conf file for getting list of
        //dcache's file.
        String pnfslayer = dir.getPath() + PATH_SEPARATOR
                + ".(config)(dCache)/dcache.conf";

        if (DEBUG)
            System.out.println("Reading pnfs layer file " + pnfslayer);

        File fpnfslayer = new File(pnfslayer);
        if (!fpnfslayer.exists() || !fpnfslayer.canRead()) {
            throw new IOException("Unable to read pnfs layer file " + pnfslayer);
        }

        BufferedReader buffreader = new BufferedReader(
                new FileReader(pnfslayer));

        String line = null;
        Vector<String> hosts = new Vector<String>();
        while ((line = buffreader.readLine()) != null) {
            hosts.add(line);

            if (DEBUG)
                System.out.println("Host = " + line);
        }

        if (DEBUG)
            System.out
                    .println("Number of doors available are  " + hosts.size());

        return hosts.toArray(new String[0]);
    }

    /**
     * Get random PNFS door from the PNFS server.
     * @param pnfspath The PNFS path.
     * @return Address of random PNFS door.
     * @throws FileNotFoundException If the path is invalid.
     * @throws IOException If there was an error getting the dCap doors.
     */
    public static String getdCapDoor(String pnfspath)
            throws FileNotFoundException, IOException {

        String selectedHost = null;

        String[] hosts = getdCapDoors(pnfspath);
        //First get the list of all available doors and then
        //pick one at random. Almost the same mechanism as adopted
        //by dcap.
        if (hosts.length == 1)
            selectedHost = hosts[0];
        else if (hosts.length > 1) {
            //more than 1 hosts. Let's pick one at random
            int random = (int) Math.random() * hosts.length;
            if (random > 0 && random < hosts.length) {
                if (DEBUG)
                    System.out.println("Picking " + hosts[random]
                            + " at random ");
                selectedHost = hosts[random];

            } else {
                if (DEBUG) //this shouldn't happen
                    System.out.println("dCap host selection failed; unable to precicesly calculate" +
                            " random number.");
            }
        }
        return selectedHost;
    }
}
