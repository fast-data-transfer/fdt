/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa;

import lia.util.net.copy.monitoring.lisa.cmdExec.CommandResult;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gregory Denis
 * @author Ciprian Dobre
 */
public class MacHostPropertiesMonitor {

    protected static final Object lock = new Object();
    protected static int ptnr = 0;
    protected final Logger logger;
    protected String[] networkInterfaces;
    protected String activeInterface;
    protected String cpuUsage = "0";
    protected String cpuUSR = "0";
    protected String cpuSYS = "0";
    protected String cpuIDLE = "0";
    protected String nbProcesses = "0";
    protected String load1 = "0";
    protected String load5 = "0";
    protected String load15 = "0";
    protected String memUsed = "0";
    protected String memFree = "0";
    protected String memUsage = "0";
    protected String netIn = "0";
    protected String netOut = "0";
    protected String pagesIn = "0";
    protected String pagesOut = "0";
    protected String macAddress = "unknown";
    protected String diskIO = "0";
    protected String diskIn = "0";
    protected String diskOut = "0";
    protected String diskFree = "0";
    protected String diskUsed = "0";
    protected String diskTotal = "0";
    protected String command = "";
    protected cmdExec execute = null;
    protected String sep = null;

    public MacHostPropertiesMonitor(final Logger logger) {

        this.logger = logger;

        execute = cmdExec.getInstance();
        sep = System.getProperty("file.separator");
        // get the network interfaces up
        command = sep + "sbin" + sep + "ifconfig -l -u";
        CommandResult cmdRes = execute.executeCommand(command, "lo0", 3 * 1000L);
        String result = cmdRes.getOutput();
        // System.out.println(command + " = "+ result);
        if (result == null || result.equals("")) {
            logger.warning(command + ": No result???");
        } else {
            int where = result.indexOf("lo0");
            networkInterfaces = result.substring(where + 3, result.length()).replaceAll("  ", " ").trim().split(" ");

            // get the currently used Mac Address
            for (int i = 0; i < networkInterfaces.length; i++) {
                String current = networkInterfaces[i];
                command = sep + "sbin" + sep + "ifconfig " + current;
                cmdRes = execute.executeCommand(command, current, 3 * 1000L);
                result = cmdRes.getOutput();
                // System.out.println(command + " = " + result);
                if (result == null || result.equals("")) {
                    logger.warning(command + ": No result???");
                } else {
                    if (result.indexOf("inet ") != -1) {
                        int pointI = result.indexOf("ether");
                        int pointJ = result.indexOf("media", pointI);
                        macAddress = result.substring(pointI + 5, pointJ).trim();
                        // System.out.println("Mac Address:" + macAddress);
                        activeInterface = current;
                    }
                }
            }
        }

        // get the disk information
        command = sep + "bin" + sep + "df -k -h " + sep;
        cmdRes = execute.executeCommand(command, "/dev", 3 * 1000L);
        result = cmdRes.getOutput();
        // System.out.println(command + " = "+ result);
        if (result == null || result.equals("")) {
            logger.warning(command + ": No result???");
        } else {
            parseDf(result);
        }

        update();
    }

    public String getMacAddresses() {
        return macAddress;
    }

    public void update() {

        if (execute == null) {
            execute = cmdExec.getInstance();
        }

        // get CPU, load, Mem, Pages, Processes from '/usr/bin/top'
        command = sep + "usr" + sep + "bin" + sep + "top -d -l2 -n1 -F -R -X";
        CommandResult cmdRes = execute.executeCommand(command, "PID", 2, 100 * 1000L);
        String result = cmdRes.getOutput();
        // System.out.println(command + " = "+ result);
        if (result == null || result.equals("")) {
            logger.warning("No result???");
        } else {
            parseTop(result);
        }
    }

    // private void parseIfConfig(String toParse) {
    //
    // //System.out.println("result of ifconfig:" + toParse);
    // if (toParse.indexOf("inet ") != -1) {
    // int pointI = toParse.indexOf("ether");
    // int pointJ = toParse.indexOf("media", pointI);
    // macAddress = toParse.substring(pointJ + 5, pointI).trim();
    // System.out.println("Mac Address:" + macAddress);
    // }
    // }

    private void parseDf(String toParse) {

        // System.out.println("result of df -k -h /:" + toParse);

        int pointI = toParse.indexOf("/dev/");
        int pointJ = 0;
        int pointK = 0;

        // Get the size of the root disk
        try {
            pointJ = toParse.indexOf(" ", pointI);
            pointK = indexOfUnitLetter(toParse, pointJ);
            diskTotal = toParse.substring(pointJ, pointK).trim();
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;

        // Get the capacity used
        try {
            pointI = toParse.indexOf(" ", pointK);
            pointJ = indexOfUnitLetter(toParse, pointI);
            diskUsed = toParse.substring(pointI, pointJ).trim();
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;

        // Get the free space
        try {
            pointK = toParse.indexOf(" ", pointJ);
            pointI = indexOfUnitLetter(toParse, pointK);
            diskFree = toParse.substring(pointK, pointI).trim();
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;

		/*
         * System.out.println( "Disk: Total:" + diskTotal + " Used:" + diskUsed + " Free:" + diskFree);
		 */
    }

    private int indexOfUnitLetter(String inside, int from) {

        int temp = inside.indexOf('K', from);
        if (temp == -1 || (temp - from > 10)) {
            temp = inside.indexOf('M', from);
            if (temp == -1 || (temp - from > 10)) {
                temp = inside.indexOf('G', from);
                if (temp == -1 || (temp - from > 10)) {
                    temp = inside.indexOf('B', from);
                    if (temp == -1 || (temp - from > 10)) {
                        temp = inside.indexOf('T', from);
                        if (temp == -1 || (temp - from > 10)) {
                            temp = inside.indexOf('b', from);
                            if (temp - from > 10)
                                temp = -1;
                        }
                    }
                }
            }
        }
        return temp;
    }

    private int lastIndexOfUnitLetter(String inside, int from) {

        int temp = inside.lastIndexOf('K', from);
        if (temp == -1 || (from - temp > 10)) {
            temp = inside.lastIndexOf('M', from);
            if (temp == -1 || (from - temp > 10)) {
                temp = inside.lastIndexOf('G', from);
                if (temp == -1 || (from - temp > 10)) {
                    temp = inside.lastIndexOf('B', from);
                    if (temp == -1 || (from - temp > 10)) {
                        temp = inside.lastIndexOf('T', from);
                        if (temp == -1 || (from - temp > 10)) {
                            temp = inside.lastIndexOf('b', from);
                            if (from - temp > 10)
                                temp = -1;
                        }
                    }
                }
            }
        }
        return temp;
    }

    // private double howMuchKiloBytes(char a) {
    //
    // switch (a) {
    // case 'T' :
    // return 1073741824.0;
    // case 'G' :
    // return 1048576.0;
    // case 'M' :
    // return 1024.0;
    // case 'K' :
    // return 1.0;
    // case 'B' :
    // return 0.0009765625;
    // default :
    // return 1.0;
    // }
    // }

    private double howMuchMegaBytes(char a) {

        switch (a) {
            case 'T':
                return 1048576.0;
            case 'G':
                return 1024.0;
            case 'M':
                return 1.0;
            case 'K':
                return 0.0009765625;
            case 'B':
                return 0.0000009537;
            default:
                return 1.0;
        }
    }

    private void parseTop(String toParse) {

        // System.out.println("\n******\n"+toParse+"\n********\n");

        int pointA = 0;
        int pointB = 0;
        int unitPos = 0;
        double sum = 0.0;

        // Get number of total Processes
        try {
            pointA = toParse.indexOf("Procs:");
            // System.out.println("First Procs at " + pointA);
            pointA = toParse.indexOf("Procs:", pointA + 6) + 6;
            // System.out.println("Second Procs at " + pointA);
            pointB = toParse.indexOf(",", pointA + 1);
            nbProcesses = toParse.substring(pointA, pointB).trim();
            // System.out.println(nbProcesses + " processes");
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;

        // Get the loads...
        try {
            pointA = toParse.indexOf("LoadAvg:", pointA);
            pointA += 9;
            pointB = toParse.indexOf(",", pointA);
            load1 = toParse.substring(pointA, pointB).trim();
            pointA = toParse.indexOf(",", pointB + 1);
            load5 = toParse.substring(pointB + 1, pointA).trim();
            pointB = toParse.indexOf("CPU:", pointA + 1);
            pointB = toParse.lastIndexOf(".", pointB);
            load15 = toParse.substring(pointA + 1, pointB).trim();
            // System.out.println("load: [" + load1 + "][" + load5 + "][" + load15 + "]");
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;

        // Get CPUs...
        try {
            pointB = toParse.indexOf("CPU:", pointB + 1) + 4;
            pointA = toParse.indexOf("% user", pointB);
            cpuUSR = toParse.substring(pointB, pointA).trim();
            pointA = toParse.indexOf(",", pointA);
            pointB = toParse.indexOf("% sys", pointA + 1);
            cpuSYS = toParse.substring(pointA + 1, pointB).trim();
            pointA = toParse.indexOf(",", pointB);
            pointB = toParse.indexOf("% idle", pointA + 1);
            cpuIDLE = toParse.substring(pointA + 1, pointB).trim();
            sum = 100.0 - Double.parseDouble(cpuIDLE);
            cpuUsage = String.valueOf(sum);
			/*
			 * System.out.println("Cpu Usage:" + cpuUsage + " user:" + cpuUSR + " sys:" + cpuSYS + " idle:" + cpuIDLE);
			 */
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;

        // Get Mem...
        try {
            pointA = toParse.indexOf("PhysMem", pointB);
            pointA += 8;
            pointB = toParse.indexOf("M used", pointA);
            pointA = toParse.lastIndexOf(",", pointB);
            memUsed = toParse.substring(pointA + 1, pointB).trim();
            pointB = toParse.indexOf("M free", pointB);
            pointA = toParse.lastIndexOf(",", pointB);
            memFree = toParse.substring(pointA + 1, pointB).trim();
            // System.out.println("Mem Used:"+memUsed+"M Free:"+memFree+"M");
            sum = Double.parseDouble(memUsed) + Double.parseDouble(memFree);
            double percentage = Integer.parseInt(memUsed) / sum * 100;
            memUsage = String.valueOf(percentage);
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;

        // Pages In/Out...
        try {
            pointA = toParse.indexOf("VirtMem:", pointB + 6);
            pointB = toParse.indexOf("pagein", pointA);
            pointA = toParse.lastIndexOf(",", pointB);
            pagesIn = toParse.substring(pointA + 1, pointB).trim();
            pointA = toParse.indexOf("pageout", pointB);
            pointB = toParse.lastIndexOf(",", pointA);
            pagesOut = toParse.substring(pointB + 1, pointA).trim();
            // System.out.println("Pages In:" + pagesIn + " Out" + pagesOut);
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            logger.warning("Can't find pages in :" + toParse);
        }
        ;

        // Get Network IO...
        try {
            pointA = toParse.indexOf("Networks:", pointB) + 9;
            pointB = toParse.indexOf("data =", pointA) + 6;
            pointA = toParse.indexOf("in", pointB);
            unitPos = lastIndexOfUnitLetter(toParse, pointA);
            netIn = toParse.substring(pointB, unitPos).trim();
            // System.out.print("Net In:" + netIn);
            double factor = howMuchMegaBytes((toParse.substring(unitPos, unitPos + 1).toCharArray())[0]);
            netIn = String.valueOf(Double.parseDouble(netIn) * factor * 4);
            pointB = toParse.indexOf("out", pointA);
            unitPos = lastIndexOfUnitLetter(toParse, pointB);
            factor = howMuchMegaBytes((toParse.substring(unitPos, unitPos + 1).toCharArray())[0]);
            netOut = toParse.substring(pointA + 3, unitPos).trim();
            // System.out.println("Net Out:" + netOut);
            netOut = String.valueOf(Double.parseDouble(netOut) * factor);
            // System.out.println("Network In:" + netIn + " OUT:" + netOut);
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            logger.log(Level.INFO, "Got exception", e);
        }
        ;

        // Get Disks IO...
        try {
            pointB = toParse.indexOf("Disks:", pointA) + 6;
            pointA = toParse.indexOf("data =", pointB) + 6;
            pointB = toParse.indexOf("in,", pointA);
            unitPos = lastIndexOfUnitLetter(toParse, pointB);
            diskIn = toParse.substring(pointA, unitPos).trim();
            pointA = toParse.indexOf("out", pointB);
            unitPos = lastIndexOfUnitLetter(toParse, pointA);
            diskOut = toParse.substring(pointB + 3, unitPos).trim();

            // System.out.println("diskIO In:" + diskIn + " Out:" + diskOut);
            diskIO = diskOut;
        } catch (java.lang.StringIndexOutOfBoundsException e) {
        }
        ;
    }

    public String getCpuUsage() {
        return cpuUsage;
    }

    public String getCpuUSR() {
        return cpuUSR;
    }

    public String getCpuSYS() {
        return cpuSYS;
    }

    public String getCpuNICE() {
        return "0";
    }

    public String getCpuIDLE() {
        return cpuIDLE;
    }

    public String getPagesIn() {
        return pagesIn;
    }

    public String getPagesOut() {
        return pagesOut;
    }

    public String getMemUsage() {
        return memUsage;
    }

    public String getMemUsed() {
        return memUsed;
    }

    public String getMemFree() {
        return memFree;
    }

    public String getDiskIO() {
        return diskIO;
    }

    public String getDiskTotal() {
        return diskTotal;
    }

    public String getDiskUsed() {
        return diskUsed;
    }

    public String getDiskFree() {
        return diskFree;
    }

    public String getNoProcesses() {
        return nbProcesses;
    }

    public String getLoad1() {
        return load1;
    }

    public String getLoad5() {
        return load5;
    }

    public String getLoad15() {
        return load15;
    }

    public String[] getNetInterfaces() {
        return networkInterfaces;
    }

    public String getNetIn(String ifName) {
        if (ifName.equalsIgnoreCase(activeInterface))
            return netIn;
        else
            return "0";
    }

    public String getNetOut(String ifName) {
        if (ifName.equalsIgnoreCase(activeInterface))
            return netOut;
        return "0";
    }
}
